package pe.test.exchange.application.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionApplicationService;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversionControllerTest {

    @Mock
    private ConversionApplicationService applicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConversionController(applicationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Convierte el monto y devuelve la respuesta definida por la API")
    void shouldReturnConversionResponse() throws Exception {
        // Arrange
        ConversionResponse response = new ConversionResponse(
                new BigDecimal("100.00"),
                new BigDecimal("370.00"),
                "USD",
                "PEN",
                new BigDecimal("3.70")
        );
        when(applicationService.convertCurrency(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
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

        verify(applicationService).convertCurrency(any());
    }

    @Test
    @DisplayName("Devuelve 400 cuando los datos de conversión no son válidos")
    void shouldReturnBadRequestForDomainErrors() throws Exception {
        when(applicationService.convertCurrency(any()))
                .thenThrow(new DomainException("El monto debe ser mayor a cero"));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 0,
                                  "monedaOrigen": "USD",
                                  "monedaDestino": "PEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El monto debe ser mayor a cero"));
    }

    @Test
    @DisplayName("Devuelve 502 cuando el proveedor de tipo de cambio falla")
    void shouldReturnBadGatewayForProviderErrors() throws Exception {
        when(applicationService.convertCurrency(any()))
                .thenThrow(new ExchangeRateProviderException(
                        "No se pudo consultar el proveedor", "connection-error"));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 100,
                                  "monedaOrigen": "USD",
                                  "monedaDestino": "PEN"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "No fue posible obtener el tipo de cambio en este momento. Inténtalo más tarde"));
    }

    @Test
    @DisplayName("Devuelve el mismo formato de error cuando el JSON está mal formado")
    void shouldReturnProblemDetailForInvalidJson() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no es válido"));
    }

    @Test
    @DisplayName("Rechaza un monto enviado como texto")
    void shouldRejectAmountAsString() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":"100","monedaOrigen":"USD","monedaDestino":"PEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El monto debe ser un número"));
    }

    @Test
    @DisplayName("Indica que monedaOrigen debe ser texto")
    void shouldRejectSourceCurrencyAsNumber() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":123,"monedaOrigen":3122,"monedaDestino":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("monedaOrigen debe ser texto"));
    }

    @Test
    @DisplayName("Indica que monedaDestino debe ser texto")
    void shouldRejectTargetCurrencyAsBoolean() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":123,"monedaOrigen":"PEN","monedaDestino":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("monedaDestino debe ser texto"));
    }

    @Test
    @DisplayName("Mantiene el error de formato para un código de moneda escrito como texto")
    void shouldRejectInvalidCurrencyCode() throws Exception {
        when(applicationService.convertCurrency(any()))
                .thenThrow(new DomainException("El código de moneda debe tener exactamente 3 letras ISO (ej. USD, PEN)"));

        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":123,"monedaOrigen":"US","monedaDestino":"PEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "El código de moneda debe tener exactamente 3 letras ISO (ej. USD, PEN)"));
    }

    @Test
    @DisplayName("Identifica el monto si se envía como arreglo")
    void shouldRejectAmountAsArray() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":[],"monedaOrigen":"USD","monedaDestino":"PEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El monto debe ser un número"));
    }
}
