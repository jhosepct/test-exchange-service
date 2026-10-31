// src/main/java/pe/test/exchange/infrastructure/external/ExchangeRateClientAdapter.java
package pe.test.exchange.infrastructure.external;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.ExchangeRateNotFoundException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExchangeRateClientAdapter implements ExchangeRateProvider {

    private final ExchangeRateApiClient apiClient;

    @Value("${exchange-rate.api.key}")
    private String apiKey;

    @Override
    public BigDecimal getExchangeRate(CurrencyCode origen, CurrencyCode destino) {
        try {
            ExchangeRateApiResponse response = apiClient.getLatestRates(apiKey, origen.value());

            if (response == null || response.conversionRates() == null) {
                throw new DomainException("Respuesta inválida del proveedor de tipo de cambio");
            }

            BigDecimal rate = response.conversionRates().get(destino.value());
            if (rate == null) {
                throw new ExchangeRateNotFoundException(
                        "No se encontró tipo de cambio para la moneda destino: " + destino.value()
                );
            }

            return rate;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new ExchangeRateNotFoundException(
                    "Error al consultar el servicio externo para la moneda origen: " + origen.value()
            );
        }
    }
}