package pe.test.exchange.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
