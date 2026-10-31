package pe.test.exchange.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.test.exchange.domain.exception.ExchangeRateNotFoundException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateClientAdapterTest {

    private static final String TEST_API_KEY = "test-api-key";

    @Mock
    private ExchangeRateApiClient apiClient;

    private ExchangeRateClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExchangeRateClientAdapter(apiClient);
        ReflectionTestUtils.setField(adapter, "apiKey", TEST_API_KEY);
    }

    @Test
    @DisplayName("Obtiene el tipo de cambio de la respuesta del proveedor")
    void shouldReturnExchangeRateFromExternalApi() {
        // Arrange
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse(
                "success",
                "USD",
                Map.of("USD", new BigDecimal("1"), "PEN", new BigDecimal("3.70")),
                null
        );
        when(apiClient.getLatestRates(TEST_API_KEY, "USD")).thenReturn(apiResponse);

        // Act
        BigDecimal exchangeRate = adapter.getExchangeRate(sourceCurrency, targetCurrency);

        // Assert
        assertEquals(new BigDecimal("3.70"), exchangeRate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unsupported-code", "malformed-request", "invalid-key", "inactive-account", "quota-reached"})
    @DisplayName("Conserva el tipo de error devuelto por el proveedor")
    void shouldPreserveProviderErrorType(String errorType) {
        // Arrange
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse(
                "error",
                "USD",
                null,
                errorType
        );
        when(apiClient.getLatestRates(TEST_API_KEY, "USD")).thenReturn(apiResponse);

        // Act & Assert
        ExchangeRateProviderException exception = assertThrows(
                ExchangeRateProviderException.class,
                () -> adapter.getExchangeRate(sourceCurrency, targetCurrency));
        assertEquals(errorType, exception.getErrorType());
    }

    @Test
    @DisplayName("Lanza una excepción cuando falla la consulta al proveedor")
    void shouldWrapClientFailure() {
        // Arrange
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        RuntimeException clientFailure = new RuntimeException("Connection failed");
        when(apiClient.getLatestRates(TEST_API_KEY, "USD")).thenThrow(clientFailure);

        // Act & Assert
        ExchangeRateProviderException exception = assertThrows(
                ExchangeRateProviderException.class,
                () -> adapter.getExchangeRate(sourceCurrency, targetCurrency));
        assertEquals(clientFailure, exception.getCause());
        assertEquals("connection-error", exception.getErrorType());
    }

    @Test
    @DisplayName("Lanza una excepción cuando la respuesta no incluye la moneda destino")
    void shouldThrowExceptionWhenTargetCurrencyNotFound() {
        // Arrange
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("XYZ");
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse(
                "success",
                "USD",
                Map.of("USD", new BigDecimal("1"), "PEN", new BigDecimal("3.70")),
                null
        );
        when(apiClient.getLatestRates(TEST_API_KEY, "USD")).thenReturn(apiResponse);

        // Act & Assert
        assertThrows(ExchangeRateNotFoundException.class,
                () -> adapter.getExchangeRate(sourceCurrency, targetCurrency));
    }
}
