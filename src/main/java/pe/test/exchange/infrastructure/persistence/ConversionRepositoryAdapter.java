package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;

@Repository
@RequiredArgsConstructor
public class ConversionRepositoryAdapter implements ConversionRepository {

    private final JpaConversionRepository jpaRepository;

    @Override
    public Conversion save(Conversion conversion) {
        ConversionEntity entity = ConversionEntity.builder()
                .originalAmount(conversion.getOriginalAmount())
                .sourceCurrency(conversion.getSourceCurrency().value())
                .targetCurrency(conversion.getTargetCurrency().value())
                .exchangeRate(conversion.getExchangeRate())
                .convertedAmount(conversion.getConvertedAmount())
                .operationDate(conversion.getOperationDate())
                .build();

        ConversionEntity savedEntity = jpaRepository.save(entity);

        return new Conversion(
                savedEntity.getId(),
                savedEntity.getOriginalAmount(),
                new CurrencyCode(savedEntity.getSourceCurrency()),
                new CurrencyCode(savedEntity.getTargetCurrency()),
                savedEntity.getExchangeRate(),
                savedEntity.getConvertedAmount(),
                savedEntity.getOperationDate()
        );
    }
}
