# pt-agency
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-booking.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-shopping.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-customer.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-invoicing.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-designer.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-common.yml/badge.svg)
![build workflow](https://github.com/vondacho/pt-agency/actions/workflows/ci-www.yml/badge.svg)

A system which supports the daily business of a personal training agency

## What participants can do

On **pta-www**, participants browse the training sessions of the coming weeks with their seat availability,
sign in, and book a session, join its waiting list or cancel. A booking needs an active subscription giving access
to the session.

| Module | Role | Dev port |
|---|---|---|
| pta-www | Participant web front (Angular SPA), sign-in (OpenID Connect), relays to pta-booking | 8081 |
| pta-booking | Sessions and bookings, source of truth | 8080 |
| pta-customer | Participants and their subscriptions | 8082 |
| pta-shopping | Articles: sessions, passes and what they give access to | 8083 |

## Run locally

Requirements: Java 21+, Maven 3.9, Docker (each service starts its own PostgreSQL, pta-www a Keycloak, via Quarkus Dev Services).

```sh
scripts/dev-all.sh
# with Rancher Desktop, if /var/run/docker.sock is not available:
DOCKER_HOST=unix://$HOME/.rd/docker.sock scripts/dev-all.sh
```

Then open http://localhost:8081 and sign in with one of the development users of the Keycloak realm
(`pta-www/src/main/resources/keycloak/pta-realm.json`, test credentials only):

| User | Situation |
|---|---|
| alice | holds an "Unlimited pass": can book every session |
| bob | holds no subscription: bookings are refused |
| carol | account not linked to a participant |

Seed data (sessions, articles, customers) lives in each module's `src/main/resources/db/dev`, dated from today.

## Configuration in production

| Variable | Module | Purpose |
|---|---|---|
| `PTA_BOOKING_URL` | www | base URL of pta-booking |
| `PTA_OIDC_URL`, `PTA_OIDC_SECRET` | www | OpenID Connect provider (realm URL) and client secret of `pta-www` |
| `PTA_CUSTOMER_URL`, `PTA_SHOPPING_URL` | booking | base URLs of pta-customer and pta-shopping |
| `PTA_<MODULE>_DB_USERNAME`, `_PASSWORD`, `_REACTIVE_URL`, `_JDBC_URL` | booking, customer, shopping | PostgreSQL datasource |

The `pta-www` client and the `customer_id` user attribute and claim of the production identity provider must match
the development realm. pta-customer can also run against the managed DigitalOcean database with the opt-in `do` profile.
