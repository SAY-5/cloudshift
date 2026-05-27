package io.cloudshift.gateway.dualwrite;

/**
 * Writes a reservation to one backend and returns what that backend stored. Implementations talk to
 * the monolith or the extracted service; tests substitute in-memory backends.
 */
public interface ReservationWriter {

    ReservationRecord write(ReservationRecord request);
}
