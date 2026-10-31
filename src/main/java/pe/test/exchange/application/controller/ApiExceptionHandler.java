package pe.test.exchange.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.MismatchedInputException;

import java.math.BigDecimal;
import pe.test.exchange.domain.exception.DomainException;
import pe.test.exchange.domain.exception.ExchangeRateProviderException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ExchangeRateProviderException.class)
    public ProblemDetail handleProviderError(ExchangeRateProviderException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "No fue posible obtener el tipo de cambio en este momento. Inténtalo más tarde");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidRequest(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof MismatchedInputException mismatch) {
                if (BigDecimal.class.equals(mismatch.getTargetType())) {
                    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El monto debe ser un número");
                }
                if (String.class.equals(mismatch.getTargetType())) {
                    for (var field : mismatch.getPath()) {
                        String name = field.getPropertyName();
                        if ("monedaOrigen".equals(name) || "monedaDestino".equals(name)) {
                            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                    name + " debe ser texto");
                        }
                    }
                }
            }
            cause = cause.getCause();
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido");
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainError(DomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
