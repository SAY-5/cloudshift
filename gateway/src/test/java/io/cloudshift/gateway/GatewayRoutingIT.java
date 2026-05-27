package io.cloudshift.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIT {

    static final MockWebServer MONOLITH = new MockWebServer();
    static final MockWebServer SERVICE = new MockWebServer();

    static {
        MONOLITH.setDispatcher(taggedDispatcher("monolith"));
        SERVICE.setDispatcher(taggedDispatcher("service"));
        try {
            MONOLITH.start();
            SERVICE.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Dispatcher taggedDispatcher(String tag) {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("X-Served-By", tag)
                        .setBody("[]");
            }
        };
    }

    @DynamicPropertySource
    static void routing(DynamicPropertyRegistry registry) {
        registry.add(
                "cloudshift.routing.monolith-uri", () -> "http://localhost:" + MONOLITH.getPort());
        registry.add(
                "cloudshift.routing.capabilities.reservations.path-prefix", () -> "/reservations");
        registry.add(
                "cloudshift.routing.capabilities.reservations.service-uri",
                () -> "http://localhost:" + SERVICE.getPort());
        registry.add("cloudshift.routing.capabilities.reservations.target", () -> "MONOLITH");
    }

    @AfterAll
    static void shutdown() throws IOException {
        MONOLITH.shutdown();
        SERVICE.shutdown();
    }

    @Autowired private WebTestClient client;

    @Test
    void servesReservationsThroughMonolithByDefault() {
        client.get()
                .uri("/reservations")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Served-By", "monolith");
    }

    @Test
    void servesRoomsThroughMonolithFallback() {
        client.get()
                .uri("/rooms")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Served-By", "monolith");
    }

    @Test
    void reservationsRouteIsConfiguredAsExpected() {
        assertThat(SERVICE.getPort()).isNotEqualTo(MONOLITH.getPort());
    }
}
