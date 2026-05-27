package io.cloudshift.gateway.dualwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares the reservation the monolith stored with the reservation the extracted service stored
 * for the same dual write and reports any business field that diverges. Used during the dual-write
 * phase of a migration to surface backends that have drifted apart before reads are cut over.
 */
public class ConsistencyCheck {

    public ConsistencyResult compare(ReservationRecord monolith, ReservationRecord service) {
        if (monolith == null || service == null) {
            List<String> missing = new ArrayList<>();
            if (monolith == null) {
                missing.add("monolith.record");
            }
            if (service == null) {
                missing.add("service.record");
            }
            return ConsistencyResult.divergent(missing);
        }

        List<String> divergent = new ArrayList<>();
        if (!Objects.equals(monolith.roomId(), service.roomId())) {
            divergent.add("roomId");
        }
        if (!Objects.equals(monolith.guestName(), service.guestName())) {
            divergent.add("guestName");
        }
        if (!Objects.equals(monolith.checkIn(), service.checkIn())) {
            divergent.add("checkIn");
        }
        if (!Objects.equals(monolith.checkOut(), service.checkOut())) {
            divergent.add("checkOut");
        }

        return divergent.isEmpty()
                ? ConsistencyResult.matched()
                : ConsistencyResult.divergent(divergent);
    }
}
