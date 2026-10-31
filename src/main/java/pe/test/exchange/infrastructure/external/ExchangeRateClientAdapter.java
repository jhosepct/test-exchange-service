package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.ExchangeRateNotFoundException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateClientAdapter implements ExchangeRateProvider {

    private static final String UNKNOWN_ERROR = "unknown";
    private static final String INVALID_RESPONSE = "invalid-response";
    private static final String CONNECTION_ERROR = "connection-error";
    private static final JsonMapper ERROR_MAPPER = JsonMapper.builder().build();

    private final ExchangeRateApiClient apiClient;

    @Value("${exchange-rate.api.key}")
    private String apiKey;

    @Override
    public BigDecimal getExchangeRate(CurrencyCode sourceCurrency, CurrencyCode targetCurrency) {
        ExchangeRateApiResponse response;
        try {
            response = apiClient.getLatestRates(apiKey, sourceCurrency.value());
        } catch (FeignException exception) {
            String errorType = providerErrorType(exception);
            log.warn("Error al consultar tipo de cambio: monedaOrigen={} status={} errorType={}",
                    sourceCurrency.value(), exception.status(), errorType);
            if (exception.status() == 404 && "unsupported-code".equals(errorType)) {
                throw new DomainException("La moneda origen no está soportada: " + sourceCurrency.value());
            }
            throw new ExchangeRateProviderException(
                    "No se pudo consultar el proveedor de tipo de cambio", errorType, exception);
        } catch (RuntimeException exception) {
            log.warn("Error de conexión con el proveedor de tipo de cambio: monedaOrigen={} tipo={}",
                    sourceCurrency.value(), exception.getClass().getSimpleName());
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
            log.warn("Respuesta de error del proveedor: monedaOrigen={} errorType={}",
                    sourceCurrency.value(), errorType);
            if ("unsupported-code".equals(errorType)) {
                throw new DomainException("La moneda origen no está soportada: " + sourceCurrency.value());
            }
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
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0
                || exchangeRate.stripTrailingZeros().scale() > 6
                || exchangeRate.compareTo(new BigDecimal("10000000000000")) >= 0) {
            throw new ExchangeRateProviderException(
                    "El proveedor devolvió un tipo de cambio inválido", INVALID_RESPONSE);
        }

        return exchangeRate;
    }

    private String providerErrorType(FeignException exception) {
        try {
            var body = ERROR_MAPPER.readTree(exception.contentUTF8());
            String errorType = body == null ? "" : body.path("error-type").asText();
            return errorType.isBlank() ? CONNECTION_ERROR : errorType;
        } catch (JacksonException exceptionParsingResponse) {
            return CONNECTION_ERROR;
        }
    }
}
