package pe.test.exchange.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pe.test.exchange.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    public static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El monto a convertir debe ser mayor a cero");
        }
        if (amount.stripTrailingZeros().scale() > 4 || amount.compareTo(new BigDecimal("1000000000000000")) >= 0) {
            throw new DomainException("El monto supera la precisión permitida (15 enteros y 4 decimales)");
        }
    }

    public static Conversion create(BigDecimal originalAmount, CurrencyCode sourceCurrency,
                                    CurrencyCode targetCurrency, BigDecimal exchangeRate) {
        validateAmount(originalAmount);
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El tipo de cambio debe ser mayor a cero");
        }

        BigDecimal convertedAmount = originalAmount.multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
        if (convertedAmount.compareTo(new BigDecimal("100000000000000000")) >= 0) {
            throw new DomainException("El monto convertido supera la capacidad de almacenamiento");
        }

        return new Conversion(
                null,
                originalAmount,
                sourceCurrency,
                targetCurrency,
                exchangeRate,
                convertedAmount,
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }
}