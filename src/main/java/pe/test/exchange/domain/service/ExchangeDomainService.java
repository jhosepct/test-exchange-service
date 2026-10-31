package pe.test.exchange.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;

import java.math.BigDecimal;
import java.util.Currency;

@Service
@RequiredArgsConstructor
public class ExchangeDomainService {

    private final ExchangeRateProvider exchangeRateProvider;

    public Conversion performConversion(BigDecimal amount, CurrencyCode sourceCurrency,
                                        CurrencyCode targetCurrency) {
        Conversion.validateAmount(amount);
        BigDecimal exchangeRate;
        if (sourceCurrency.equals(targetCurrency)) {
            try {
                Currency.getInstance(sourceCurrency.value());
            } catch (IllegalArgumentException exception) {
                throw new DomainException("La moneda no está soportada: " + sourceCurrency.value());
            }
            exchangeRate = BigDecimal.ONE;
        } else {
            exchangeRate = exchangeRateProvider.getExchangeRate(sourceCurrency, targetCurrency);
        }
        return Conversion.create(amount, sourceCurrency, targetCurrency, exchangeRate);
    }
}