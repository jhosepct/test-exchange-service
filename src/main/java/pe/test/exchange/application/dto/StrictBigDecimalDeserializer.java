package pe.test.exchange.application.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import java.math.BigDecimal;

public class StrictBigDecimalDeserializer extends ValueDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT
                && parser.currentToken() != JsonToken.VALUE_NUMBER_FLOAT) {
            return context.reportInputMismatch(BigDecimal.class, "El monto debe ser un número");
        }
        return parser.getDecimalValue();
    }
}
