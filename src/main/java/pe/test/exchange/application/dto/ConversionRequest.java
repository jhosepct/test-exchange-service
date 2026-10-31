package pe.test.exchange.application.dto;

import java.math.BigDecimal;

public record ConversionRequest(
        BigDecimal monto,
        String monedaOrigen,
        String monedaDestino
) {}