package pe.test.exchange.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExchangeDomainService {

    private final ExchangeRateProvider exchangeRateProvider;

    public Conversion performConversion(BigDecimal amount, CurrencyCode sourceCurrency,
                                        CurrencyCode targetCurrency) {
        BigDecimal exchangeRate = exchangeRateProvider.getExchangeRate(sourceCurrency, targetCurrency);
        return Conversion.create(amount, sourceCurrency, targetCurrency, exchangeRate);
    }
}