# Loggi Server

Loggi Server is the REST API backend for a logistics management system. It provides endpoints for warehouse management, inventory tracking, distribution orders, fleet management, sales, and system administration.

> **Personal Graduation Project (2023) — Backend**
>
> Frontend repository: https://github.com/PiscesTrio/Loggi_app

## Tech Stack

- **Java 11**
- **Spring Boot 2.7.2**
- **Spring Security** with JWT authentication
- **Spring Data JPA**
- **MySQL**
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
| DB Name | `DB_NAME` | `test` |
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
├── task/            # Background tasks
└── utils/           # JWT utilities, IP/browser helpers
```

## API Notes

- All endpoints are prefixed with `/api`.
- Successful responses are automatically wrapped into `ResponseResult<T>` (`{ code: 200, status: true, data: ... }`).
- Authentication endpoints such as `/api/admin/login` return raw maps without the wrapper.
- The JWT token must be sent in the `Authorization` header with the prefix `logistics:`.

## Default Admin Setup

Call `POST /api/admin/init` to create the first super-admin account if no admin exists. Check `GET /api/admin/hasInit` first.
