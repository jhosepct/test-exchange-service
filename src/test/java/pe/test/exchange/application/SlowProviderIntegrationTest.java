package pe.test.exchange.application;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.test.exchange.infrastructure.persistence.JpaConversionRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "exchange-rate.api.key=test-api-key",
        "spring.cloud.openfeign.client.config.exchangeRateApi.read-timeout=200"
})
@AutoConfigureMockMvc
class SlowProviderIntegrationTest {

    private static HttpServer provider;
    private static final AtomicInteger receivedRequests = new AtomicInteger();
    private static final AtomicBoolean clientFinished = new AtomicBoolean();
    private static final AtomicReference<IOException> unexpectedFailure = new AtomicReference<>();
    private static final CountDownLatch responseAttempted = new CountDownLatch(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaConversionRepository conversionRepository;

    @DynamicPropertySource
    static void providerUrl(DynamicPropertyRegistry registry) throws IOException {
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/test-api-key/latest/USD", exchange -> {
            receivedRequests.incrementAndGet();
            try {
                Thread.sleep(1000);
                byte[] body = """
                        {"result":"success","base_code":"USD","conversion_rates":{"PEN":3.70}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException exception) {
                if (!clientFinished.get()) {
                    unexpectedFailure.set(exception);
                }
            } finally {
                exchange.close();
                responseAttempted.countDown();
            }
        });
        provider.start();
        registry.add("exchange-rate.api.base-url",
                () -> "http://127.0.0.1:" + provider.getAddress().getPort());
    }

    @BeforeEach
    void cleanDatabase() {
        conversionRepository.deleteAll();
        receivedRequests.set(0);
        clientFinished.set(false);
        unexpectedFailure.set(null);
    }

    @AfterAll
    static void stopProvider() {
        if (provider != null) {
            provider.stop(0);
        }
    }

    @Test
    @DisplayName("No guarda conversiones cuando un proveedor HTTP real responde demasiado tarde")
    void shouldNotPersistWhenProviderIsSlow() throws Exception {
        mockMvc.perform(post("/api/conversion")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "No fue posible obtener el tipo de cambio en este momento. Inténtalo más tarde"));

        clientFinished.set(true);
        assertTrue(responseAttempted.await(2, TimeUnit.SECONDS));
        assertNull(unexpectedFailure.get(), "El proveedor falso falló antes de que venciera el timeout");
        assertEquals(1, receivedRequests.get());
        assertEquals(0, conversionRepository.count());
    }
}
