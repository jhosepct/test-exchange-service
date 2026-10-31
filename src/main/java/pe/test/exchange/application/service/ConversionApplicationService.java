package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeDomainService;

@Service
@RequiredArgsConstructor
public class ConversionApplicationService {

    private final ExchangeDomainService exchangeDomainService;
    private final ConversionRepository conversionRepository;
    private final ConversionMapper conversionMapper;

    @Transactional
    public ConversionResponse convertCurrency(ConversionRequest request) {
        CurrencyCode sourceCurrency = new CurrencyCode(request.monedaOrigen());
        CurrencyCode targetCurrency = new CurrencyCode(request.monedaDestino());

        Conversion conversion = exchangeDomainService.performConversion(
                request.monto(), sourceCurrency, targetCurrency);
        Conversion savedConversion = conversionRepository.save(conversion);

        return conversionMapper.toResponse(savedConversion);
    }
}