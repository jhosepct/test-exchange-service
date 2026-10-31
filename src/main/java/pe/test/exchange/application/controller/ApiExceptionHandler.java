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
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invalidRequestDetail(exception));
    }

    private String invalidRequestDetail(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof MismatchedInputException mismatch) {
                String detail = typeErrorDetail(mismatch);
                if (detail != null) {
                    return detail;
                }
            }
            cause = cause.getCause();
        }
        return "El cuerpo de la solicitud no es válido";
    }

    private String typeErrorDetail(MismatchedInputException mismatch) {
        if (BigDecimal.class.equals(mismatch.getTargetType())) {
            return "El monto debe ser un número";
        }
        if (String.class.equals(mismatch.getTargetType())) {
            for (var field : mismatch.getPath()) {
                String name = field.getPropertyName();
                if ("monedaOrigen".equals(name) || "monedaDestino".equals(name)) {
                    return name + " debe ser texto";
                }
            }
        }
        return null;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainError(DomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
