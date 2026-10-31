package pe.test.exchange.application.dto;

import tools.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;

public record ConversionRequest(
        @JsonDeserialize(using = StrictBigDecimalDeserializer.class) BigDecimal monto,
        @JsonDeserialize(using = StrictCurrencyCodeDeserializer.class) String monedaOrigen,
        @JsonDeserialize(using = StrictCurrencyCodeDeserializer.class) String monedaDestino
) {}