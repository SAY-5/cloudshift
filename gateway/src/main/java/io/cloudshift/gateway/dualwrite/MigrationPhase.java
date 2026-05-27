package io.cloudshift.gateway.dualwrite;

/**
 * Phases a capability moves through during a rollback-safe cutover.
 *
 * <ul>
 *   <li>{@code MONOLITH}: writes go only to the monolith.
 *   <li>{@code DUAL_WRITE}: writes go to both backends so the monolith stays current and can be
 *       rolled back to.
 *   <li>{@code SERVICE}: reads are served by the extracted service while writes continue to mirror
 *       to the monolith, keeping rollback safe.
 * </ul>
 */
public enum MigrationPhase {
    MONOLITH,
    DUAL_WRITE,
    SERVICE
}
