package pe.test.exchange.application;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.test.exchange.infrastructure.persistence.ConversionEntity;
import pe.test.exchange.infrastructure.external.ExchangeRateApiClient;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;
import pe.test.exchange.infrastructure.persistence.JpaConversionRepository;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "exchange-rate.api.key=test-api-key")
@AutoConfigureMockMvc
class ConversionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaConversionRepository conversionRepository;

    @MockitoBean
    private ExchangeRateApiClient apiClient;

    @BeforeEach
    void cleanDatabase() {
        conversionRepository.deleteAll();
    }

    @Test
    @DisplayName("Convierte el monto y guarda la operación en la base de datos")
    void shouldConvertAndPersistRequest() throws Exception {
        // Arrange
        when(apiClient.getLatestRates("test-api-key", "USD"))
                .thenReturn(new ExchangeRateApiResponse(
                        "success",
                        "USD",
                        Map.of("USD", BigDecimal.ONE, "PEN", new BigDecimal("3.70")),
                        null
                ));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 100,
                                  "monedaOrigen": "USD",
                                  "monedaDestino": "PEN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100.0))
                .andExpect(jsonPath("$.montoConvertido").value(370.0))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.7));

        // Verify the operation was stored
        var savedConversions = conversionRepository.findAll();
        assertEquals(1, savedConversions.size());

        ConversionEntity savedConversion = savedConversions.get(0);
        assertNotNull(savedConversion.getId());
        assertEquals(new BigDecimal("100.0000"), savedConversion.getOriginalAmount());
        assertEquals("USD", savedConversion.getSourceCurrency());
        assertEquals("PEN", savedConversion.getTargetCurrency());
        assertEquals(new BigDecimal("3.700000"), savedConversion.getExchangeRate());
        assertEquals(new BigDecimal("370.00"), savedConversion.getConvertedAmount());
        assertNotNull(savedConversion.getOperationDate());
    }

    @Test
    @DisplayName("Guarda la conversión entre la misma moneda sin consultar al proveedor")
    void shouldPersistSameCurrencyWithoutCallingFeign() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100000000000000, "monedaOrigen": "PEN", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100000000000000L))
                .andExpect(jsonPath("$.montoConvertido").value(100000000000000L))
                .andExpect(jsonPath("$.monedaOrigen").value("PEN"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(1));

        verifyNoInteractions(apiClient);
        var savedConversions = conversionRepository.findAll();
        assertEquals(1, savedConversions.size());
        ConversionEntity saved = savedConversions.get(0);
        assertEquals(new BigDecimal("100000000000000.0000"), saved.getOriginalAmount());
        assertEquals(new BigDecimal("100000000000000.00"), saved.getConvertedAmount());
        assertEquals(new BigDecimal("1.000000"), saved.getExchangeRate());
        assertEquals("PEN", saved.getSourceCurrency());
        assertEquals("PEN", saved.getTargetCurrency());
        assertNotNull(saved.getOperationDate());
    }

    @Test
    @DisplayName("Devuelve 400 cuando la moneda origen no existe en el proveedor")
    void shouldRejectUnsupportedSourceCurrency() throws Exception {
        // Arrange
        when(apiClient.getLatestRates("test-api-key", "ZZZ"))
                .thenReturn(new ExchangeRateApiResponse("error", null, null, "unsupported-code"));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "ZZZ", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La moneda origen no está soportada: ZZZ"));

        assertEquals(0, conversionRepository.count());
    }

    @Test
    @DisplayName("Devuelve 400 cuando Feign recibe un 404 por moneda origen no soportada")
    void shouldRejectUnsupportedSourceCurrencyFromFeign() throws Exception {
        // Arrange
        Request request = Request.create(Request.HttpMethod.GET, "https://example.com/latest/ZZZ",
                Map.of(), null, StandardCharsets.UTF_8);

        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request)
                .body("{\"result\":\"error\",\"error-type\":\"unsupported-code\"}", StandardCharsets.UTF_8)
                .build();

        when(apiClient.getLatestRates("test-api-key", "ZZZ"))
                .thenThrow(FeignException.errorStatus("getLatestRates", response));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 120, "monedaOrigen": "ZZZ", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La moneda origen no está soportada: ZZZ"));

        assertEquals(0, conversionRepository.count());
    }

    @Test
    @DisplayName("No expone al cliente que la clave del proveedor es inválida")
    void shouldHideInvalidApiKeyFromClient() throws Exception {
        // Arrange
        Request request = Request.create(Request.HttpMethod.GET, "https://example.com/latest/PEN",
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(403)
                .reason("Forbidden")
                .request(request)
                .body("{\"result\":\"error\",\"error-type\":\"invalid-key\"}", StandardCharsets.UTF_8)
                .build();
        when(apiClient.getLatestRates("test-api-key", "PEN"))
                .thenThrow(FeignException.errorStatus("getLatestRates", response));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 120, "monedaOrigen": "PEN", "monedaDestino": "USD"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "No fue posible obtener el tipo de cambio en este momento. Inténtalo más tarde"))
                .andExpect(jsonPath("$.errorType").doesNotExist());

        assertEquals(0, conversionRepository.count());
    }

    @ParameterizedTest(name = "monto={0}, {1} a {2}")
    @CsvSource(delimiter = '|', textBlock = """
            100              | ZZZ | ZZZ
            1000000000000000 | USD | PEN
            0.123456         | USD | PEN
            """)
    @DisplayName("Rechaza solicitudes inválidas sin consultar al proveedor")
    void shouldRejectInvalidRequestBeforeCallingProvider(String amount, String source, String target) throws Exception {
        String request = """
                {"monto": %s, "monedaOrigen": "%s", "monedaDestino": "%s"}
                """.formatted(amount, source, target);

        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(apiClient);
        assertEquals(0, conversionRepository.count());
    }

    @Test
    @DisplayName("No guarda la conversión si el proveedor agota el tiempo de espera")
    void shouldReturnBadGatewayWithoutSavingOnTimeout() throws Exception {
        Request request = Request.create(Request.HttpMethod.GET,
                "https://example.com/latest/USD", Map.of(), null, StandardCharsets.UTF_8);
        when(apiClient.getLatestRates("test-api-key", "USD"))
                .thenThrow(new RetryableException(-1, "Read timed out", Request.HttpMethod.GET,
                        new SocketTimeoutException("Read timed out"), (Long) null, request));

        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "No fue posible obtener el tipo de cambio en este momento. Inténtalo más tarde"));

        verify(apiClient).getLatestRates("test-api-key", "USD");
        assertEquals(0, conversionRepository.count());
    }

    @Test
    @DisplayName("Rechaza tasas que no pueden guardarse sin perder precisión")
    void shouldRejectRateWithExcessivePrecision() throws Exception {
        // Arrange
        when(apiClient.getLatestRates("test-api-key", "USD"))
                .thenReturn(new ExchangeRateApiResponse(
                        "success", "USD", Map.of("PEN", new BigDecimal("3.1234567")), null));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadGateway());

        assertEquals(0, conversionRepository.count());
    }

    @Test
    @DisplayName("Rechaza conversiones cuyo resultado supera la capacidad de la base de datos")
    void shouldRejectConvertedAmountBeyondStorageLimit() throws Exception {
        // Arrange
        when(apiClient.getLatestRates("test-api-key", "USD"))
                .thenReturn(new ExchangeRateApiResponse(
                        "success", "USD", Map.of("PEN", new BigDecimal("9999999999999")), null));

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100000, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, conversionRepository.count());
    }
}
