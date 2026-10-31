package pe.test.exchange.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeDomainService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionApplicationServiceTest {

    @Mock
    private ExchangeDomainService exchangeDomainService;

    @Mock
    private ConversionRepository conversionRepository;

    private ConversionApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ConversionApplicationService(
                exchangeDomainService,
                conversionRepository,
                new ConversionMapper()
        );
    }

    @Test
    @DisplayName("Orquesta la conversión, la guarda y devuelve la respuesta")
    void shouldConvertPersistAndReturnResponse() {
        // Arrange
        BigDecimal amount = new BigDecimal("100");
        CurrencyCode sourceCurrency = new CurrencyCode("USD");
        CurrencyCode targetCurrency = new CurrencyCode("PEN");
        ConversionRequest request = new ConversionRequest(amount, "USD", "PEN");

        Conversion calculatedConversion = Conversion.create(
                amount, sourceCurrency, targetCurrency, new BigDecimal("3.70"));
        Conversion persistedConversion = new Conversion(
                1L,
                amount,
                sourceCurrency,
                targetCurrency,
                new BigDecimal("3.70"),
                new BigDecimal("370.00"),
                LocalDateTime.now()
        );

        when(exchangeDomainService.performConversion(amount, sourceCurrency, targetCurrency))
                .thenReturn(calculatedConversion);
        when(conversionRepository.save(any(Conversion.class))).thenReturn(persistedConversion);

        // Act
        ConversionResponse response = applicationService.convertCurrency(request);

        // Assert
        assertNotNull(response);
        assertEquals(amount, response.monto());
        assertEquals(new BigDecimal("370.00"), response.montoConvertido());
        assertEquals("USD", response.monedaOrigen());
        assertEquals("PEN", response.monedaDestino());
        assertEquals(new BigDecimal("3.70"), response.tipoCambio());
        verify(conversionRepository, times(1)).save(calculatedConversion);
    }
}
