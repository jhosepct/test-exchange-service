package pe.test.exchange.domain.exception;

public class ExchangeRateProviderException extends DomainException {
    private final String errorType;

    public ExchangeRateProviderException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ExchangeRateProviderException(String message, String errorType, Throwable cause) {
        super(message);
        this.errorType = errorType;
        initCause(cause);
    }

    public String getErrorType() {
        return errorType;
    }
}
