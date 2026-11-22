# pta-www
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-www.yml/badge.svg)

Public web front of the agency. It hosts the participant SPA (Angular, in `src/main/webui`, served by Quinoa)
and relays its API calls (`/api/**`) to the backing services. It stores no data: **pta-booking is the source of truth**
for training sessions and their seats.

## Run locally

```sh
# 1. pta-booking on :8080 (starts a PostgreSQL container with seeded sessions)
mvn -f pta-booking quarkus:dev

# 2. pta-www on :8081 (starts ng serve behind Quarkus, with live reload)
mvn -f pta-www quarkus:dev
```

Then open http://localhost:8081.

To work on the SPA alone, `npm start` in `src/main/webui` serves it on :4200 and proxies `/api` to :8081.

## Configuration

| Property / variable | Default | Purpose |
|---|---|---|
| `PTA_BOOKING_URL` | `http://localhost:8080` | Base URL of pta-booking |

## API

`GET /api/sessions?from=YYYY-MM-DD&to=YYYY-MM-DD`: sessions taking place in the period, in chronological order,
with their seat availability. Answers `502` with a problem document when pta-booking is unavailable.
