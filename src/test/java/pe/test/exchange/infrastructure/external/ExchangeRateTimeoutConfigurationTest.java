package pe.test.exchange.infrastructure.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "exchange-rate.api.key=test-api-key")
class ExchangeRateTimeoutConfigurationTest {

    @Autowired
    private FeignClientProperties feignClientProperties;

    @Test
    @DisplayName("Configura los tiempos de conexión y lectura del proveedor de cambio")
    void shouldConfigureExchangeRateClientTimeouts() {
        var config = feignClientProperties.getConfig().get("exchangeRateApi");

        assertNotNull(config);
        assertEquals(2000, config.getConnectTimeout());
        assertEquals(5000, config.getReadTimeout());
    }
}
