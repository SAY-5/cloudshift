package io.cloudshift.gateway.dualwrite;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Canonical view of a reservation used to compare what the monolith and the extracted service
 * stored for the same write. The generated id is deliberately excluded: the two backends assign ids
 * independently, so comparing them would report spurious divergence. Equality is on the business
 * fields that both backends must agree on.
 */
public record ReservationRecord(
        Long roomId, String guestName, LocalDate checkIn, LocalDate checkOut) {

    public boolean matches(ReservationRecord other) {
        return other != null
                && Objects.equals(roomId, other.roomId)
                && Objects.equals(guestName, other.guestName)
                && Objects.equals(checkIn, other.checkIn)
                && Objects.equals(checkOut, other.checkOut);
    }
}
