package io.cloudshift.gateway.dualwrite;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates the dual-write phase of a migration. A reservation write is applied to both the
 * monolith and the extracted service, the two stored records are compared, and any divergence is
 * counted so it can be surfaced. The monolith remains the system of record during this phase, so
 * its stored record is returned to the caller.
 */
public class DualWriteCoordinator {

    private final ReservationWriter monolith;
    private final ReservationWriter service;
    private final ConsistencyCheck consistencyCheck;
    private final AtomicLong divergenceCount = new AtomicLong();

    public DualWriteCoordinator(
            ReservationWriter monolith,
            ReservationWriter service,
            ConsistencyCheck consistencyCheck) {
        this.monolith = monolith;
        this.service = service;
        this.consistencyCheck = consistencyCheck;
    }

    public DualWriteOutcome write(ReservationRecord request) {
        ReservationRecord monolithRecord = monolith.write(request);
        ReservationRecord serviceRecord = service.write(request);
        ConsistencyResult result = consistencyCheck.compare(monolithRecord, serviceRecord);
        if (result.isDivergent()) {
            divergenceCount.incrementAndGet();
        }
        return new DualWriteOutcome(monolithRecord, serviceRecord, result);
    }

    public long divergenceCount() {
        return divergenceCount.get();
    }

    public record DualWriteOutcome(
            ReservationRecord monolithRecord,
            ReservationRecord serviceRecord,
            ConsistencyResult consistency) {

        public ReservationRecord systemOfRecord() {
            return monolithRecord;
        }
    }
}
