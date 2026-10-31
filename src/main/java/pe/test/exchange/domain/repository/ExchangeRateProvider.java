package pe.test.exchange.domain.repository;

import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;

public interface ExchangeRateProvider {
    BigDecimal getExchangeRate(CurrencyCode sourceCurrency, CurrencyCode targetCurrency);
}