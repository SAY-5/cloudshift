package io.cloudshift.gateway.dualwrite;

/**
 * Drives a rollback-safe cutover for a single capability. The monolith receives every write in
 * every phase, so its view stays complete and a rollback to the monolith never finds lost or
 * duplicated records. The extracted service receives writes once the migration window opens, and
 * serves reads only after the route is flipped.
 */
public class CutoverCoordinator {

    private final ReservationWriter monolith;
    private final ReservationWriter service;
    private MigrationPhase phase = MigrationPhase.MONOLITH;

    public CutoverCoordinator(ReservationWriter monolith, ReservationWriter service) {
        this.monolith = monolith;
        this.service = service;
    }

    public MigrationPhase phase() {
        return phase;
    }

    public void beginDualWrite() {
        phase = MigrationPhase.DUAL_WRITE;
    }

    public void cutOver() {
        phase = MigrationPhase.SERVICE;
    }

    public void rollBack() {
        phase = MigrationPhase.MONOLITH;
    }

    /**
     * Applies a write according to the current phase. The monolith is written in every phase; the
     * service is written once the migration window has opened. Writing the monolith unconditionally
     * is what makes the rollback safe.
     */
    public void write(ReservationRecord request) {
        monolith.write(request);
        if (phase != MigrationPhase.MONOLITH) {
            service.write(request);
        }
    }

    /** The backend that currently serves reads for the capability. */
    public ReservationWriter readBackend() {
        return phase == MigrationPhase.SERVICE ? service : monolith;
    }
}
