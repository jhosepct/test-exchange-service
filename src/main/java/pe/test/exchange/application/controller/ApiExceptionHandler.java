package pe.test.exchange.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ExchangeRateProviderException.class)
    public ProblemDetail handleProviderError(ExchangeRateProviderException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainError(DomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
