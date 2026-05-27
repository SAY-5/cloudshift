package io.cloudshift.gateway;

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

/**
 * With the reservations capability cut over to {@code SERVICE}, the gateway must route {@code
 * /reservations} to the extracted service rather than the monolith.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlippedRouteReachesServiceIT {

    static final MockWebServer MONOLITH = new MockWebServer();
    static final MockWebServer SERVICE = new MockWebServer();

    static {
        MONOLITH.setDispatcher(tagged("monolith"));
        SERVICE.setDispatcher(tagged("service"));
        try {
            MONOLITH.start();
            SERVICE.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Dispatcher tagged(String tag) {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setHeader("X-Served-By", tag);
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
        registry.add("cloudshift.routing.capabilities.reservations.target", () -> "SERVICE");
    }

    @AfterAll
    static void shutdown() throws IOException {
        MONOLITH.shutdown();
        SERVICE.shutdown();
    }

    @Autowired private WebTestClient client;

    @Test
    void reservationsReachTheExtractedServiceAfterCutover() {
        client.get()
                .uri("/reservations/1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Served-By", "service");
    }

    @Test
    void roomsStillReachTheMonolithAfterCutover() {
        client.get()
                .uri("/rooms")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Served-By", "monolith");
    }
}
