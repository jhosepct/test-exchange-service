package pe.test.exchange.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.test.exchange.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Conversion {

    private final Long id;
    private final BigDecimal originalAmount;
    private final CurrencyCode sourceCurrency;
    private final CurrencyCode targetCurrency;
    private final BigDecimal exchangeRate;
    private final BigDecimal convertedAmount;
    private final LocalDateTime operationDate;

    public static Conversion create(BigDecimal originalAmount, CurrencyCode sourceCurrency,
                                    CurrencyCode targetCurrency, BigDecimal exchangeRate) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El monto a convertir debe ser mayor a cero");
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El tipo de cambio debe ser mayor a cero");
        }

        BigDecimal convertedAmount = originalAmount.multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);

        return new Conversion(
                null,
                originalAmount,
                sourceCurrency,
                targetCurrency,
                exchangeRate,
                convertedAmount,
                LocalDateTime.now()
        );
    }
}