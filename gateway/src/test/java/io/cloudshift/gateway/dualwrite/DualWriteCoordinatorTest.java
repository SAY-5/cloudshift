package io.cloudshift.gateway.dualwrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DualWriteCoordinatorTest {

    private static final ReservationRecord REQUEST =
            new ReservationRecord(1L, "Ada", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));

    private final ConsistencyCheck consistencyCheck = new ConsistencyCheck();

    @Test
    void consistentDualWritePasses() {
        DualWriteCoordinator coordinator =
                new DualWriteCoordinator(
                        request -> withId(request, 10L),
                        request -> withId(request, 99L),
                        consistencyCheck);

        DualWriteCoordinator.DualWriteOutcome outcome = coordinator.write(REQUEST);

        assertThat(outcome.consistency().consistent()).isTrue();
        assertThat(coordinator.divergenceCount()).isZero();
        // The monolith stays the system of record during the dual-write phase.
        assertThat(outcome.systemOfRecord().roomId()).isEqualTo(1L);
    }

    @Test
    void divergentDualWriteIsDetected() {
        DualWriteCoordinator coordinator =
                new DualWriteCoordinator(
                        request -> withId(request, 10L),
                        // the service stores a different guest name for the same write
                        request ->
                                new ReservationRecord(
                                        request.roomId(),
                                        "someone-else",
                                        request.checkIn(),
                                        request.checkOut()),
                        consistencyCheck);

        DualWriteCoordinator.DualWriteOutcome outcome = coordinator.write(REQUEST);

        assertThat(outcome.consistency().isDivergent()).isTrue();
        assertThat(outcome.consistency().divergentFields()).containsExactly("guestName");
        assertThat(coordinator.divergenceCount()).isEqualTo(1);
    }

    private static ReservationRecord withId(ReservationRecord request, long ignoredId) {
        // The id differs between backends and is intentionally not part of the comparison.
        return new ReservationRecord(
                request.roomId(), request.guestName(), request.checkIn(), request.checkOut());
    }
}
