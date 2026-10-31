package pe.test.exchange.domain.model;
import pe.test.exchange.domain.exception.DomainException;

public record CurrencyCode(String value) {
    public CurrencyCode {
        if(value == null || !value.trim().matches("^[A-Za-z]{3}$")) {
            throw new DomainException("El código de moneda debe tener exactamente 3 letras ISO (ej. USD, PEN)");
        }
        value = value.trim().toUpperCase();
    }
}
