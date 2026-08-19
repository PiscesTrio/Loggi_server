# Loggi Server

Loggi Server is the REST API backend for a logistics management system. It provides endpoints for warehouse management, inventory tracking, distribution orders, fleet management, sales, and system administration.

> Frontend repository: https://github.com/PiscesTrio/Loggi_app

## Tech Stack

- **Java 21**
- **Spring Boot 4.1.0**
- **Spring Security** with JWT authentication
- **Spring Data JPA** (Hibernate, `ddl-auto: validate`)
- **Flyway** for schema migrations
- **MySQL**
- **Redis** for verification codes and rate limiting
- **Maven**

## Features

- **Authentication & Authorization**: JWT-based login with role-based access control (`ROLE_SUPER_ADMIN`, `ROLE_ADMIN`). Supports both password and email verification login.
- **Warehouse & Inventory**: Manage warehouses and track inventory movements (in/out records).
- **Commodities & Sales**: Product catalog and sales record management.
- **Distribution & Transport**: Distribution orders with status tracking, linked to drivers and vehicles.
- **System Logging**: Login logs and operation logs with AOP-based automatic recording.
- **Email Service**: SMTP-based email verification via QQ Mail.

## Getting Started

### Prerequisites

- JDK 11+
- Maven 3.6+
- MySQL 8.0+

### Configuration

Edit `src/main/resources/application.yaml` or set environment variables:

| Config | Env Variable | Default |
|--------|-------------|---------|
| DB Host | `DB_HOST` | `localhost` |
| DB Port | `DB_PORT` | `3306` |
| DB Name | `DB_NAME` | `loggi` |
| DB User | `DB_USERNAME` | `root` |
| DB Password | `DB_PASSWORD` | `your-db-password` |
| JWT Secret | `JWT_SECRET` | `CHANGE_ME` |
| Mail User | `MAIL_USERNAME` | `your-email@example.com` |
| Mail Password | `MAIL_PASSWORD` | `your-mail-password` |

### Run

```bash
mvn spring-boot:run
```

The server starts on port **8088**.

### Build

```bash
mvn clean package
```

### Tests

```bash
mvn test
```

Run a single test class:
```bash
mvn test -Dtest=ClassName
```

## Architecture

```
com.example.api/
├── controller/      # REST endpoints under /api/*
├── service/         # Business logic (interfaces + implementations)
├── repository/      # Spring Data JPA repositories
├── model/           # Entities, DTOs, and response wrappers
├── security/        # JWT filter and Spring Security configuration
├── handler/         # Global response wrapper and exception handler
├── aspect/          # AOP logging aspect
└── utils/           # JWT utilities, IP/browser helpers
```

## Two APIs with no screens

`Sale` and `Employee` have a complete backend - controller, service, repository, entity,
and their own `@PreAuthorize` roles - and no client calls them. The Flutter app has never
had a screen for either.

That is deliberate, and it is worth saying so plainly, because **an API-only resource and
an unfinished one look identical in a repository**. The decision was to keep them and
document them rather than build two more CRUD screens or delete two real parts of the
domain: the endpoints are described in the OpenAPI document, tagged `Sales (API only)` and
`Employees (API only)`, and that is where their contract lives.

If you are looking for the screens, there aren't any. That is the answer, not an omission.

## Database schema

The schema is owned by Flyway, not by Hibernate. Migrations live in
`src/main/resources/db/migration` and run before anything else touches the datasource:

| Script | What it does |
| --- | --- |
| `V1__baseline.sql` | The schema as the entities define it, captured when Flyway took over |
| `V2__unique_constraints_and_indexes.sql` | The unique constraints and indexes `ddl-auto` could never create |

Two rules follow from that:

- **An applied script is never edited.** Flyway stores a checksum per script; changing one
  makes every existing database fail validation. Schema changes are appended as `V3`, `V4`, ...
- **`ddl-auto` is `validate` in every profile.** Hibernate no longer changes the database, it
  only checks that the entities and the migrations agree — and refuses to start if they do
  not, so a missing migration fails in CI rather than drifting in production.

An existing database with no `flyway_schema_history` table is adopted rather than rejected:
`baseline-on-migrate` stamps it as version 1 and applies from `V2` onward.

## API Notes

- All endpoints are prefixed with `/api`.
- Successful responses are automatically wrapped into `ResponseResult<T>` (`{ code: 200, status: true, data: ... }`).
- Authentication endpoints such as `/api/admin/login` return raw maps without the wrapper.
- The JWT token must be sent in the `Authorization` header with the prefix `logistics:`.

## Default Admin Setup

Call `POST /api/admin/init` to create the first super-admin account if no admin exists. Check `GET /api/admin/hasInit` first.

## Run with Docker (S00)

A multi-stage `Dockerfile` and a `docker-compose.yml` (app + MySQL 8) are provided for one-command, reproducible startup:

```bash
cp .env.example .env   # then fill in real values
docker compose up --build
```

The API will be available at `http://localhost:8088`. The `app` service waits for MySQL to become healthy before it starts.

## Environment Variables

`.env.example` is the canonical list of every variable this service reads; copy it to `.env` and fill in
real values. `.env` is gitignored and must never be committed — only `.env.example` is tracked.

The defaults in `src/main/resources/application.yaml` (table above) are deliberately non-functional
placeholders: `JWT_SECRET` must be replaced with a secret of at least 32 bytes, and `MAIL_PASSWORD`
expects a QQ-mail authorization code, not the account password. `docker compose` reads `.env`
automatically; for a bare `mvn spring-boot:run` export the variables yourself.

## Testing (S00 safety net)

```bash
./mvnw test                 # unit tests only — no Docker required
./mvnw verify -DskipITs     # unit tests + JaCoCo report and coverage gate, still no Docker
./mvnw verify               # + *IT integration tests (Testcontainers → requires Docker)
```

The Maven Wrapper pins Maven 3.9.6, so only a JDK 11 is needed locally.

Integration tests (`*IT`) start a real `mysql:8.0` via Testcontainers, so Docker must be running. Against
Docker Engine 29+ add `-Dapi.version=1.44` — the docker-java client bundled with Testcontainers 1.20.x
still defaults to API 1.32, which the daemon rejects.

JaCoCo HTML report: `target/site/jacoco/index.html`. The coverage gate in `pom.xml` is a floor measured
from a real run rather than a target; each refactor slice ratchets it up, and it only ever goes up.
