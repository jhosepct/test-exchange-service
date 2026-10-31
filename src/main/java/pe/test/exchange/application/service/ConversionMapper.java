package pe.test.exchange.application.service;

import org.springframework.stereotype.Component;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;

@Component
public class ConversionMapper {

    public ConversionResponse toResponse(Conversion conversion) {
        return new ConversionResponse(
                conversion.getOriginalAmount(),
                conversion.getConvertedAmount(),
                conversion.getSourceCurrency().value(),
                conversion.getTargetCurrency().value(),
                conversion.getExchangeRate()
        );
    }
}