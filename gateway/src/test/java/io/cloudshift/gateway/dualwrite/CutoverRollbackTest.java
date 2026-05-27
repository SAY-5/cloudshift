package io.cloudshift.gateway.dualwrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CutoverRollbackTest {

    /** In-memory backend that records every write so we can check for loss or duplication. */
    private static final class RecordingStore implements ReservationWriter {
        private final List<ReservationRecord> stored = new ArrayList<>();

        @Override
        public ReservationRecord write(ReservationRecord request) {
            stored.add(request);
            return request;
        }

        List<ReservationRecord> contents() {
            return List.copyOf(stored);
        }
    }

    private static ReservationRecord reservation(long roomId, String guest) {
        return new ReservationRecord(
                roomId, guest, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));
    }

    @Test
    void cutoverThenRollbackLeavesTheMonolithConsistent() {
        RecordingStore monolith = new RecordingStore();
        RecordingStore service = new RecordingStore();
        CutoverCoordinator coordinator = new CutoverCoordinator(monolith, service);

        // Write before any migration.
        coordinator.write(reservation(1, "before"));

        // Open the migration window and write while dual-writing.
        coordinator.beginDualWrite();
        coordinator.write(reservation(2, "dual"));

        // Flip reads to the service and write while cut over.
        coordinator.cutOver();
        coordinator.write(reservation(3, "cutover"));
        assertThat(coordinator.readBackend()).isSameAs(service);

        // Roll back to the monolith.
        coordinator.rollBack();
        coordinator.write(reservation(4, "after"));
        assertThat(coordinator.readBackend()).isSameAs(monolith);

        // The monolith saw every write exactly once: nothing lost, nothing duplicated.
        List<ReservationRecord> monolithView = monolith.contents();
        assertThat(monolithView)
                .containsExactly(
                        reservation(1, "before"),
                        reservation(2, "dual"),
                        reservation(3, "cutover"),
                        reservation(4, "after"));
        assertThat(monolithView).doesNotHaveDuplicates();

        // The service only holds the writes made from the migration window onward.
        assertThat(service.contents())
                .containsExactly(reservation(2, "dual"), reservation(3, "cutover"));
    }
}
