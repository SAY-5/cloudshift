package io.cloudshift.monolith;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationFlowIT extends PostgresContainerSupport {

    @Autowired private TestRestTemplate rest;

    @Test
    void createsRoomThenReservationAgainstPostgres() {
        ResponseEntity<Map> room =
                rest.postForEntity("/rooms", Map.of("name", "Cedar", "capacity", 2), Map.class);
        assertThat(room.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number roomId = (Number) room.getBody().get("id");

        Map<String, Object> body =
                Map.of(
                        "roomId", roomId.longValue(),
                        "guestName", "Ada",
                        "checkIn", LocalDate.now().toString(),
                        "checkOut", LocalDate.now().plusDays(2).toString());
        ResponseEntity<Map> created = rest.postForEntity("/reservations", body, Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("guestName")).isEqualTo("Ada");
        Number id = (Number) created.getBody().get("id");

        ResponseEntity<Map> fetched =
                rest.getForEntity("/reservations/" + id.longValue(), Map.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("roomId")).isEqualTo(roomId.intValue());
    }
}
