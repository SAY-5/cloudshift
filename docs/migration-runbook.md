# Migration runbook

This runbook describes the phased cutover of a capability from the monolith to
an extracted service. The reservations capability is used as the worked example.

## Principles

- One capability moves at a time. Everything else keeps running on the monolith.
- Each step is reversible. A capability can be cut over and rolled back through
  configuration alone.
- The gateway is the single place where routing is decided, so a path always
  resolves to exactly one backend.

## Phases

### Phase 0: facade in place

The gateway sits in front of the monolith. Every path, including
`/reservations/**`, routes to the monolith. Clients only ever talk to the
gateway. This is the starting state and adds the facade without changing
behaviour.

State: `cloudshift.routing.capabilities.reservations.target = MONOLITH`.

### Phase 1: extract the service

The reservations capability is built as a standalone service
(`reservation-service`) with its own database and the same external contract as
the monolith's reservations endpoints. It is deployed alongside the monolith but
receives no production traffic yet.

Verification: the contract tests assert that the monolith and the extracted
service expose an equivalent contract for the migrated capability.

### Phase 2: dual-write and shadow

Before flipping reads, writes are mirrored to both backends and a consistency
check compares the results. Divergence is surfaced rather than silently
accepted. This builds confidence that the extracted service behaves like the
monolith for the same inputs.

Verification: a divergence injected into the comparison is detected, and a
consistent dual-write passes.

### Phase 3: cut over

The routing target for reservations is flipped to `SERVICE`. The gateway now
sends `/reservations/**` to the extracted service. Because the flip is a
configuration change applied to the gateway, no client and no other capability
is affected.

```bash
RESERVATIONS_TARGET=SERVICE docker compose up -d gateway
```

Verification: a flipped route reaches the extracted service.

### Phase 4: rollback if needed

If a problem appears after cutover, the target is flipped back to `MONOLITH`.
Because reads and writes during the dual-write phase kept the monolith's view
current, rolling back leaves a consistent monolith with no lost or duplicated
records.

```bash
RESERVATIONS_TARGET=MONOLITH docker compose up -d gateway
```

Verification: cutting over, writing, then rolling back leaves the monolith with
a consistent view.

### Phase 5: decommission

Once the extracted service has run cleanly for long enough, the monolith's copy
of the capability can be removed and the dual-write disabled. This step is out
of scope for the reference toolkit, which stops at a proven, reversible cutover.

## Routing invariants

- Every request path resolves to exactly one backend.
- A path under a declared capability prefix routes to that capability's current
  target.
- Every other path falls through to the monolith.

These invariants are enforced by the routing contract and property tests in the
gateway module. The contract tests check that every sampled path resolves to
exactly one backend, that flipping a target moves only the matching capability,
and that the monolith and the extracted service expose the same reservation
field set so the migrated capability presents an equivalent contract on both
backends.
