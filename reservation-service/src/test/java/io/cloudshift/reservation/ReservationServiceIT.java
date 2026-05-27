package io.cloudshift.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationServiceIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private TestRestTemplate rest;

    @Test
    void createsAndReadsReservationAgainstOwnDatabase() {
        Map<String, Object> body =
                Map.of(
                        "roomId",
                        7L,
                        "guestName",
                        "Grace",
                        "checkIn",
                        LocalDate.now().toString(),
                        "checkOut",
                        LocalDate.now().plusDays(3).toString());
        ResponseEntity<Map> created = rest.postForEntity("/reservations", body, Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = (Number) created.getBody().get("id");

        ResponseEntity<Map> fetched =
                rest.getForEntity("/reservations/" + id.longValue(), Map.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("guestName")).isEqualTo("Grace");
        assertThat(fetched.getBody().get("roomId")).isEqualTo(7);
    }
}
