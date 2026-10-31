package pe.test.exchange.domain.exception;

public class ExchangeRateNotFoundException extends DomainException {
    public ExchangeRateNotFoundException(String message) {
        super(message);
    }
}