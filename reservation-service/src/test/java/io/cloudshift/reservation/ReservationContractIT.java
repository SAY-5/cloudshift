package io.cloudshift.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Pins the reservation contract the extracted service exposes. The monolith's matching test asserts
 * the same field set, so the migrated capability presents an equivalent contract on both backends.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationContractIT {

    static final Set<String> RESERVATION_FIELDS =
            Set.of("id", "roomId", "guestName", "checkIn", "checkOut");

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
    void createdReservationExposesTheAgreedFieldSet() {
        Map<String, Object> body =
                Map.of(
                        "roomId",
                        3L,
                        "guestName",
                        "Linus",
                        "checkIn",
                        LocalDate.now().toString(),
                        "checkOut",
                        LocalDate.now().plusDays(1).toString());
        ResponseEntity<Map> created = rest.postForEntity("/reservations", body, Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().keySet()).isEqualTo(RESERVATION_FIELDS);
    }
}
