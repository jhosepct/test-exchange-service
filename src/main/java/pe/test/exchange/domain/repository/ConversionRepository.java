package pe.test.exchange.domain.repository;

import pe.test.exchange.domain.model.Conversion;

public interface ConversionRepository {
    Conversion save(Conversion conversion);
}