package pe.test.exchange.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.test.exchange.domain.exception.DomainException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConversionTest {
    @Test
    @DisplayName("Debe calcular correctamente el monto convertido con redondeo a 2 decimales")
    void testCalculateConvertedAmount() {
        BigDecimal amount = new BigDecimal("100.00");
        CurrencyCode sourceCurrency = new CurrencyCode("usd");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        BigDecimal exchangeRate = new BigDecimal("3.756");

        // Act
        Conversion conversion = Conversion.create(amount, sourceCurrency, targetCurrency, exchangeRate);

        // Assert
        assertNotNull(conversion);
        assertEquals("USD", conversion.getSourceCurrency().value());
        assertEquals("PEN", conversion.getTargetCurrency().value());
        assertEquals(new BigDecimal("375.60"), conversion.getConvertedAmount());
        assertNotNull(conversion.getOperationDate());
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el monto es cero o negativo")
    void testCalculateConvertedAmountWithNegativeAmount() {
        CurrencyCode usd = new CurrencyCode("USD");
        CurrencyCode pen = new CurrencyCode("PEN");
        BigDecimal exchangeRate = new BigDecimal("3.70");

        assertThrows(DomainException.class, () ->
                Conversion.create(BigDecimal.ZERO, usd, pen, exchangeRate)
        );
        assertThrows(DomainException.class, () ->
                Conversion.create(new BigDecimal("-50"), usd, pen, exchangeRate)
        );
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el código de moneda no tiene 3 caracteres")
    void testCalculateConvertedAmountWithInvalidCurrencyCode() {
        assertThrows(DomainException.class, () -> new CurrencyCode("US"));
        assertThrows(DomainException.class, () -> new CurrencyCode("123"));
        assertThrows(DomainException.class, () -> new CurrencyCode(""));
    }
}