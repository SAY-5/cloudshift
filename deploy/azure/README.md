# Azure deployment notes

These notes describe the intended Azure deployment target for the topology. They
are a reference for shape and configuration. This repository does not provision
or run a live Azure environment, and the values below are placeholders.

## Target services

| Component | Azure service |
| --- | --- |
| `gateway`, `monolith`, `reservation-service` | Azure Container Apps |
| `monolith-db`, `reservation-db` | Azure Database for PostgreSQL flexible server |
| Routing state and connection strings | Container Apps environment variables and secrets |
| Container images | Azure Container Registry |

## Configuration

Routing state is supplied through environment variables on the gateway container,
mirroring the local Docker Compose setup:

- `MONOLITH_URI`: internal URL of the monolith container app.
- `RESERVATION_SERVICE_URI`: internal URL of the reservation-service container app.
- `RESERVATIONS_TARGET`: `MONOLITH` or `SERVICE`.

Cutting over in this target is a revision update that changes
`RESERVATIONS_TARGET`. Container Apps keeps the previous revision available, so a
rollback is a revision switch.

## Reference manifest

`containerapp-gateway.yaml` shows the shape of the gateway container app,
including how the routing target is passed as configuration. It is a template,
not an applied resource.

## What is not included

- No live resource group, registry, or database is created.
- No credentials or endpoints are present.
- The manifest is not wired into CI and is not applied anywhere.
