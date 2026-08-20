# Loggi Server

[![Backend CI](https://github.com/PiscesTrio/Loggi_server/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/PiscesTrio/Loggi_server/actions/workflows/backend-ci.yml)
[![Java](https://img.shields.io/badge/Java-21-informational)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-informational)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-informational)](LICENSE)

> **A personal practice project, not production software.**
> Not hardened for deployment; do not point it at real data.

REST API for a logistics management system: warehouses and stock, commodities, delivery
orders and their tracking, fleet and drivers, sales, administrators and audit logs.

> Frontend repository: https://github.com/PiscesTrio/Loggi_app

> **The Flutter client is currently out of step with this API, deliberately.** The domain
> model and the API boundary were rebuilt in two slices, and the client is realigned in its
> own. Every changed field, status code and shape is listed in
> [`docs/contract-changes.md`](docs/contract-changes.md).

## Tech stack

| | |
| --- | --- |
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 7, stateless JWT |
| Persistence | Spring Data JPA / Hibernate, `ddl-auto: validate` |
| Schema | Flyway migrations |
| Database | MySQL 8 |
| Cache | Redis — one-time codes and rate-limit counters |
| Docs | springdoc OpenAPI 3, Swagger UI |
| Ops | Spring Boot Actuator |
| Build | Maven (wrapper pins 3.9.6) |
| Tests | JUnit 5, Mockito, Testcontainers 2.0, GreenMail, JaCoCo |

## What it does

- **Authentication** — JWT, issued by password login or by a one-time code sent to an
  e-mail address. Passwords are stored as delegating-encoder hashes, never in plain text.
- **Authorization** — six roles (`ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_COMMODITY`,
  `ROLE_EMPLOYEE`, `ROLE_SALE`, `ROLE_WAREHOUSE`), enforced at the URL by a default-deny
  chain and at the method by `@PreAuthorize`.
- **Warehouses and stock** — stock levels per warehouse and the movements that change them,
  written in one transaction so a partial move cannot be recorded.
- **Commodities and sales** — catalogue and sales records.
- **Delivery orders** — orders pointing at a driver, a vehicle and an origin warehouse, with
  a tracking trail; approving an order takes the driver and vehicle, completing it releases
  them.
- **Audit logs** — every audited call and every login attempt, recorded by an aspect,
  paginated on the way out.

## Getting started

### Prerequisites

- JDK 21
- Docker (for MySQL and Redis, and for the integration tests)
- Maven is not required — use the wrapper (`./mvnw`)

### Configure

`.env.example` is the canonical list of every variable this service reads. Copy it to
`.env` and fill in real values; `.env` is gitignored and must never be committed.

The defaults in `src/main/resources/application.yaml` are deliberately non-functional
placeholders rather than working values, because a default that works is a default that
ships:

| What | Variable | Default |
| --- | --- | --- |
| Database | `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | `localhost` / `3306` / `loggi` / `root` / placeholder |
| Redis | `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| JWT | `JWT_SECRET` | `CHANGE_ME` — **the application refuses to start on this**, or on anything shorter than 32 bytes |
| Mail | `MAIL_HOST` / `MAIL_PORT` / `MAIL_SSL_ENABLE` / `MAIL_STARTTLS_ENABLE` / `MAIL_USERNAME` / `MAIL_PASSWORD` | placeholders. The host defaults to a `.invalid` name that cannot resolve, so an unconfigured deployment fails clearly. Port and transport travel with the host: 465 with implicit TLS, or 587 with STARTTLS. The password is the provider's app-specific password, not the account password |
| Profile | `SPRING_PROFILES_ACTIVE` | `dev` |
| Verification codes | `VERIFICATION_CODE_TTL_SECONDS` / `VERIFICATION_SEND_COOLDOWN_SECONDS` / `VERIFICATION_MAX_ATTEMPTS` / `VERIFICATION_LOCK_SECONDS` | `300` / `60` / `5` / `900` |
| Noise | `JPA_SHOW_SQL` / `MAIL_DEBUG` | off outside `dev` |

`docker compose` reads `.env` automatically; for a bare `./mvnw spring-boot:run`, export the
variables yourself.

### Profiles

`dev` (the default) and `prod` differ only in developer conveniences, and each setting is
still an environment variable underneath — the profile sets a default and the environment
overrides it. Everything that must not vary between environments (Flyway, schema
validation, the placeholder-refusing JWT secret) lives in `application.yaml`, so no
combination of profile settings can produce an unsafe production.

### Run

```bash
cp .env.example .env          # then fill in real values
docker compose up --build     # app + MySQL 8
```

or, against a database you already have:

```bash
./mvnw spring-boot:run
```

The API listens on **8088**.

### Build

```bash
./mvnw clean package
```

## API

Everything is under `/api`, and every response carries the same envelope:

```json
{ "code": 200, "status": true, "msg": null, "data": { } }
```

`code` repeats the HTTP status rather than always saying 200 — a body that disagrees with
its own status line is how a failure comes to look like a success. `204 No Content` is the
one exception and carries no body at all.

### Authentication

Send the token as a standard bearer credential:

```
Authorization: Bearer <token>
```

Obtain one from `POST /api/admin/login/password` or `POST /api/admin/login/email`.

Public endpoints, and why each has to be:

| Endpoint | Reason |
| --- | --- |
| `POST /api/admin/login/password`, `/login/email` | where tokens come from |
| `POST /api/admin/verification-code` | first step of e-mail login |
| `GET /api/admin/hasInit` | asked on a fresh install, which has no account |
| `POST /api/admin/init` | creates the first account; guarded by `hasInit`, not by authentication, because there is nobody to authenticate as yet |
| `GET /actuator/health` | an orchestrator probes it before the application can issue a token |
| `/v3/api-docs`, `/swagger-ui.html` | see below |

Everything else is denied by default.

### Documentation

- Swagger UI — http://localhost:8088/swagger-ui.html
- OpenAPI document — http://localhost:8088/v3/api-docs

Generated from the controllers, so it cannot drift from the code the way a hand-written
file does.

It is public deliberately. Every endpoint it describes is itself authenticated, so hiding
the description is obscurity rather than a control, and being able to open the API and read
it is the point here. A real deployment would decide this the other way, because a
published surface is a shorter path to whatever is weakest in it.

### Health

- `GET /actuator/health` — `UP` or `DOWN`, and nothing else. The detailed view names which
  component is failing, and this endpoint answers anonymous callers.
- Everything else under `/actuator` requires a token.

The mail health indicator is switched off. It authenticates against SMTP, the credentials
here are placeholders by design, and a failing indicator drags the aggregate status to
`DOWN` — so a readiness probe would keep the container out of service permanently while the
application serves every request correctly. The database and Redis indicators stay on: an
unreachable third-party SMTP is not this application being unhealthy, an unreachable Redis
is.

### Conventions

| | |
| --- | --- |
| Create | `POST /resource` → **201** with the created resource |
| Read | `GET /resource`, `GET /resource/{id}` |
| Update | `PUT /resource/{id}` — the id is in the path, never in the body |
| Delete | `DELETE /resource/{id}` → **204**, no body |
| Invalid request | **400** with the failing field's own message in `msg` |
| Missing reference | **404** naming the id |
| Business refusal | **409** — for example, dispatching a driver who is already out |

Requests and responses are DTOs and view types, never entities. No request type has an
`id`: an id on a create is read by Hibernate as an existing row to update.

Two lists are paginated — `GET /api/systemlog` and `GET /api/loginlog` — because they grow
by a row per audited request and a row per login attempt, without bound:

```json
{ "items": [ ], "page": 0, "size": 20, "totalItems": 137, "totalPages": 7 }
```

They take `page` and `size` and default to twenty, newest first. The other lists are bounded
and are returned whole; wrapping them would make every caller unwrap something to find what
it already had.

### First-run setup

`GET /api/admin/hasInit` reports whether a super administrator exists. If not,
`POST /api/admin/init` creates the first one — once. A second call is refused, so the
endpoint cannot be used to mint an administrator on a running system.

The demo seed already contains one: `demo@loggi.example` / `demo1234`.

## Two APIs with no screens

`Sale` and `Employee` have a complete backend — controller, service, repository, entity,
and their own `@PreAuthorize` roles — and no client calls them. The Flutter app has never
had a screen for either.

That is deliberate, and worth saying plainly, because **an API-only resource and an
unfinished one look identical in a repository**. The decision was to keep and document them
rather than build two more CRUD screens or delete two real parts of the domain: they are
tagged `Sales (API only)` and `Employees (API only)` in the OpenAPI document, and a test
asserts those tags are there — so removing this explanation breaks a build rather than
quietly turning a decision back into an omission.

If you are looking for the screens, there aren't any. That is the answer, not an omission.

## Database schema

Owned by Flyway, not by Hibernate. Migrations live in `src/main/resources/db/migration` and
run before anything else touches the datasource:

| Script | What it does |
| --- | --- |
| `V1__baseline.sql` | The schema as the entities define it, captured when Flyway took over |
| `V2__unique_constraints_and_indexes.sql` | The unique constraints and indexes `ddl-auto` could never create |
| `V3__timestamps_as_datetime.sql` | `varchar` timestamps become real `datetime` columns |
| `V4__money_and_quantity_types.sql` | Money becomes `DECIMAL`; a quantity stops being text |
| `V5__distribution_associations.sql` | An order points at its driver, vehicle and warehouse |
| `V6__inventory_and_track_associations.sql` | The remaining bare foreign keys become real ones |
| `V7__roles_collection_and_log_enums.sql` | Roles become rows; the audit log stops storing display labels |

Two rules follow:

- **An applied script is never edited.** Flyway stores a checksum per script; changing one
  makes every existing database fail validation. Schema changes are appended as `V8`, `V9`, …
- **`ddl-auto` is `validate` in every profile.** Hibernate no longer changes the database, it
  only checks that the entities and the migrations agree — and refuses to start if they do
  not, so a missing migration fails in CI rather than drifting in production.

An existing database with no `flyway_schema_history` table is adopted rather than rejected:
`baseline-on-migrate` stamps it as version 1 and applies from `V2` onward. Note that `V2`'s
unique constraints will fail on data that already contains duplicates — clean first.

Demo data is seeded by `src/main/resources/data.sql`, which deletes and re-inserts by a
`seed-` id prefix, so it is repeatable and never touches rows created through the app.

## Architecture

```
com.example.api/
├── annotation/      # @Log, @DisableBaseResponse
├── aspect/          # the audit-logging aspect
├── config/          # JPA auditing, OpenAPI document metadata
├── controller/      # REST endpoints under /api/*
├── exception/       # BizException — a failure that carries its own status
├── handler/         # response envelope and the exception-to-status mapping
├── model/
│   ├── dto/         # request types, with Bean Validation constraints
│   ├── entity/      # JPA entities — never serialised to a client
│   ├── enums/       # domain enums, persisted by name
│   ├── support/     # the response envelope
│   └── vo/          # view types — what endpoints actually answer with
├── repository/      # Spring Data JPA repositories
├── security/        # the JWT filter and the security chain
├── service/         # business logic (interfaces + implementations)
└── utils/           # JWT, IP and browser helpers
```

## Testing

```bash
./mvnw test                 # unit tests only — no Docker required
./mvnw verify -DskipITs     # + JaCoCo report and the coverage gate, still no Docker
./mvnw verify               # + *IT integration tests (Testcontainers → needs Docker)
```

Run one class with `./mvnw test -Dtest=ClassName`.

Integration tests (`*IT`) start real `mysql:8.0` and `redis:7-alpine` containers, and the
verification-code tests start a GreenMail SMTP server. Against Docker Engine 29+ add
`-Dapi.version=1.44`; the client bundled with Testcontainers negotiates a version the daemon
rejects otherwise.

They are not a slower copy of the unit tests. Because the containers start empty, every one
of them runs Flyway from `V1` and then lets Hibernate validate the entities against what the
migrations built — so each is also a check that the schema and the model still agree.

JaCoCo HTML report: `target/site/jacoco/index.html`. The coverage gate in `pom.xml` is a
floor measured from a real run rather than a target; it only ever goes up.
