package pe.test.exchange.infrastructure.external;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.ExchangeRateNotFoundException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExchangeRateClientAdapter implements ExchangeRateProvider {

    private static final String UNKNOWN_ERROR = "unknown";
    private static final String INVALID_RESPONSE = "invalid-response";
    private static final String CONNECTION_ERROR = "connection-error";

    private final ExchangeRateApiClient apiClient;

    @Value("${exchange-rate.api.key}")
    private String apiKey;

    @Override
    public BigDecimal getExchangeRate(CurrencyCode sourceCurrency, CurrencyCode targetCurrency) {
        ExchangeRateApiResponse response;
        try {
            response = apiClient.getLatestRates(apiKey, sourceCurrency.value());
        } catch (RuntimeException exception) {
            throw new ExchangeRateProviderException(
                    "No se pudo consultar el proveedor de tipo de cambio",
                    CONNECTION_ERROR,
                    exception);
        }

        if (response == null) {
            throw new ExchangeRateProviderException(
                    "El proveedor devolvió una respuesta vacía", INVALID_RESPONSE);
        }

        if (!"success".equals(response.result())) {
            String errorType = response.errorType() == null
                    ? UNKNOWN_ERROR
                    : response.errorType();
            throw new ExchangeRateProviderException(
                    "El proveedor rechazó la consulta", errorType);
        }

        if (response.conversionRates() == null) {
            throw new ExchangeRateProviderException(
                    "El proveedor devolvió una respuesta sin tasas", INVALID_RESPONSE);
        }

        BigDecimal exchangeRate = response.conversionRates().get(targetCurrency.value());
        if (exchangeRate == null) {
            throw new ExchangeRateNotFoundException(
                    "No se encontró tipo de cambio para la moneda destino: " + targetCurrency.value());
        }
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeRateProviderException(
                    "El proveedor devolvió un tipo de cambio inválido", INVALID_RESPONSE);
        }

        return exchangeRate;
    }
}
