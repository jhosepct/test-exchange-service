package pe.test.exchange.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionApplicationService;

@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ConversionResponse convert(@RequestBody ConversionRequest request) {
        return applicationService.convertCurrency(request);
    }
}
