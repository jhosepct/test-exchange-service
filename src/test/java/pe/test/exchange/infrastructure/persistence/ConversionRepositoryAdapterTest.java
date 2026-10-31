package pe.test.exchange.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionRepositoryAdapterTest {

    @Mock
    private JpaConversionRepository jpaRepository;

    @InjectMocks
    private ConversionRepositoryAdapter adapter;

    @Test
    @DisplayName("Guarda la conversión y devuelve el registro con su identificador")
    void shouldSaveConversionInDatabase() {
        // Arrange
        Conversion domainConversion = Conversion.create(
                new BigDecimal("100.00"),
                new CurrencyCode("USD"),
                new CurrencyCode("PEN"),
                new BigDecimal("3.70")
        );

        ConversionEntity savedEntity = ConversionEntity.builder()
                .id(1L)
                .originalAmount(new BigDecimal("100.00"))
                .sourceCurrency("USD")
                .targetCurrency("PEN")
                .exchangeRate(new BigDecimal("3.70"))
                .convertedAmount(new BigDecimal("370.00"))
                .operationDate(LocalDateTime.now())
                .build();

        when(jpaRepository.save(any(ConversionEntity.class))).thenReturn(savedEntity);

        // Act
        Conversion savedConversion = adapter.save(domainConversion);

        // Assert
        assertNotNull(savedConversion);
        assertEquals(1L, savedConversion.getId());
        assertEquals(domainConversion.getOriginalAmount(), savedConversion.getOriginalAmount());
        assertEquals(domainConversion.getSourceCurrency(), savedConversion.getSourceCurrency());
        assertEquals(domainConversion.getTargetCurrency(), savedConversion.getTargetCurrency());
        assertEquals(domainConversion.getExchangeRate(), savedConversion.getExchangeRate());
        assertEquals(domainConversion.getConvertedAmount(), savedConversion.getConvertedAmount());
        assertEquals(savedEntity.getOperationDate(), savedConversion.getOperationDate());

        // Capture the entity sent to the repository
        ArgumentCaptor<ConversionEntity> entityCaptor = ArgumentCaptor.forClass(ConversionEntity.class);
        verify(jpaRepository, times(1)).save(entityCaptor.capture());
        assertEquals(domainConversion.getOriginalAmount(), entityCaptor.getValue().getOriginalAmount());
        assertEquals("USD", entityCaptor.getValue().getSourceCurrency());
        assertEquals("PEN", entityCaptor.getValue().getTargetCurrency());
        assertEquals(domainConversion.getExchangeRate(), entityCaptor.getValue().getExchangeRate());
        assertEquals(domainConversion.getConvertedAmount(), entityCaptor.getValue().getConvertedAmount());
        assertEquals(domainConversion.getOperationDate(), entityCaptor.getValue().getOperationDate());
    }
}
