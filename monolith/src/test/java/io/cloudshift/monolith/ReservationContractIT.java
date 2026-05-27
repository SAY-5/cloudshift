package io.cloudshift.monolith;

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

/**
 * Pins the reservation contract the monolith exposes. The extracted service's matching test asserts
 * the same field set, so the migrated capability presents an equivalent contract on both backends.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationContractIT extends PostgresContainerSupport {

    static final Set<String> RESERVATION_FIELDS =
            Set.of("id", "roomId", "guestName", "checkIn", "checkOut");

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
