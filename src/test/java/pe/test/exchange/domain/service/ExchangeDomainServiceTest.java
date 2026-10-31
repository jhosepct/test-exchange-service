package pe.test.exchange.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ExchangeRateProvider;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeDomainServiceTest {

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @InjectMocks
    private ExchangeDomainService exchangeDomainService;

    @Test
    @DisplayName("Obtiene el tipo de cambio del proveedor y calcula la conversión")
    void shouldPerformConversionUsingExternalRate() {
        // Arrange
        BigDecimal amount = new BigDecimal("100");
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        BigDecimal expectedExchangeRate = new BigDecimal("3.50");

        when(exchangeRateProvider.getExchangeRate(sourceCurrency, targetCurrency))
                .thenReturn(expectedExchangeRate);

        // Act
        Conversion conversion = exchangeDomainService.performConversion(
                amount, sourceCurrency, targetCurrency);

        // Assert
        assertNotNull(conversion);
        assertEquals(expectedExchangeRate, conversion.getExchangeRate());
        assertEquals(new BigDecimal("350.00"), conversion.getConvertedAmount());
        verify(exchangeRateProvider, times(1))
                .getExchangeRate(sourceCurrency, targetCurrency);
    }

    @Test
    @DisplayName("Usa tasa uno cuando las monedas son iguales sin consultar al proveedor")
    void shouldConvertSameCurrencyWithoutProvider() {
        // Arrange
        BigDecimal amount = new BigDecimal("123.45");
        CurrencyCode pen = new CurrencyCode("PEN");

        // Act
        Conversion conversion = exchangeDomainService.performConversion(amount, pen, pen);

        // Assert
        assertEquals(BigDecimal.ONE, conversion.getExchangeRate());
        assertEquals(new BigDecimal("123.45"), conversion.getConvertedAmount());
        verifyNoInteractions(exchangeRateProvider);
    }
}