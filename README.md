# cloudshift

A reference toolkit for migrating a monolithic Spring Boot application into
containerized microservices using a strangler-fig API gateway and a phased,
config-driven cutover. The migration process itself is the subject: the
repository shows how to carve a capability out of a monolith and shift traffic
to it one route at a time without taking the application down.

## Domain

A small bookings domain keeps the example readable:

- `rooms`: rooms that can be reserved.
- `reservations`: reservations against a room.

The monolith owns both capabilities in a single deployable backed by one
database. The `reservations` capability is then extracted into its own service
with its own database, and the gateway is used to move traffic over.

## Topology

```
            client
              |
        +-----v------+
        |  gateway   |   strangler-fig facade (Spring Cloud Gateway)
        +-----+------+
              |  routes each path to exactly one backend
      +-------+-----------------+
      |                         |
+-----v------+          +-------v----------+
|  monolith  |          | reservation-     |
|  (rooms +  |          | service          |
|  reserv.)  |          | (reservations)   |
+-----+------+          +-------+----------+
      |                         |
+-----v------+          +-------v----------+
| monolith-db|          | reservation-db   |
+------------+          +------------------+
```

The gateway routes `/reservations/**` to either the monolith or the extracted
service depending on externalized routing state, and sends every other path
(for example `/rooms/**`) to the monolith. Capabilities that have not been
extracted keep being served by the monolith, so traffic flows throughout the
migration.

## Modules

| Module | Description |
| --- | --- |
| `monolith` | Reference monolith owning rooms and reservations in one deployable. |
| `reservation-service` | The reservations capability extracted into its own service and database. |
| `gateway` | The strangler-fig facade that routes each path to one backend. |

## Routing and cutover

Routing state is externalized through the `cloudshift.routing` configuration
(see `gateway/src/main/resources/application.yml`). Each capability declares a
path prefix, the URI of its extracted service, and a current `target` of either
`MONOLITH` or `SERVICE`.

Cutting a capability over is a configuration change, not a code change. With the
topology running under Docker Compose:

```bash
# default: reservations served by the monolith
docker compose up -d

# cut reservations over to the extracted service
RESERVATIONS_TARGET=SERVICE docker compose up -d gateway

# roll back to the monolith
RESERVATIONS_TARGET=MONOLITH docker compose up -d gateway
```

See [docs/migration-runbook.md](docs/migration-runbook.md) for the phased
cutover procedure.

## Running locally

Build and test everything:

```bash
mvn verify
```

The test suite uses Testcontainers, so a running Docker engine is required.

Run the full topology:

```bash
mvn -DskipTests package
docker compose up --build
```

The gateway then serves the domain on `http://localhost:8080`. A smoke check
that exercises the gateway end to end lives in `scripts/e2e-smoke.sh`.

## Cloud target

The intended deployment target is Azure (Azure Container Apps or AKS), with the
two databases on Azure Database for PostgreSQL and routing state supplied as
environment configuration. The manifests and notes in [deploy/azure](deploy/azure)
describe that target. This repository does not provision or run a live cloud
deployment; the manifests are a reference for the intended shape.

## How this differs

cloudshift is about the migration mechanics, not a finished system:

- It keeps a real monolith in the picture and demonstrates the strangler-fig
  facade, a config-toggled phased cutover, dual-write consistency checking, and
  rollback safety. The artifact is the process of getting from monolith to
  microservices without downtime.
- `shopflow` is a finished microservices system (an e-commerce platform already
  decomposed behind a gateway with a database per service). It shows the end
  state; cloudshift shows the transition.
- `live-events-spa` is a single-page front-end application. cloudshift is a
  backend migration toolkit and shares no scope with it.

## Layout

- `monolith/`, `reservation-service/`, `gateway/`: the three deployables.
- `docker-compose.yml`: the full topology.
- `docs/`: migration runbook and design notes.
- `deploy/azure/`: Azure deployment notes and reference manifests.
- `scripts/`: the end-to-end smoke check.
