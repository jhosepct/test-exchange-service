package pe.test.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "exchange-rate.api.key=test-api-key")
class ExchangeServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
