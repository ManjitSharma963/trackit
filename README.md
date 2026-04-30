# TrackIt - Spring Boot Backend

Spring Boot project with MySQL connection and schema initialization for the TrackIt app.

## Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8+

## Configuration

Set DB values using environment variables (recommended):

- `DB_URL` (optional)
- `DB_USERNAME`
- `DB_PASSWORD`

Defaults in `application.properties` are:

- username: `trackit_user`
- password: `trackit_password`

### MySQL user setup (required)

If you see `Access denied for user ...`, create/grant a dedicated user in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS TrackIt;
CREATE USER IF NOT EXISTS 'trackit_user'@'%' IDENTIFIED BY 'trackit_password';
GRANT ALL PRIVILEGES ON TrackIt.* TO 'trackit_user'@'%';
FLUSH PRIVILEGES;
```

If your app runs in Docker and MySQL is on host, use:

- `DB_URL=jdbc:mysql://host.docker.internal:3306/TrackIt?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

## Run

```bash
mvn spring-boot:run
```

Server starts at:

- `http://localhost:8081`
- Health check: `http://localhost:8081/api/health`

## APIs

### Auth (signup / signin)

- `POST /api/auth/signup` — body: `name`, `email`, `password` (password min 6 chars). Returns `id`, `name`, `email`, `createdAt`, **`token`** (JWT).
- `POST /api/auth/signin` — body: `email`, `password`. Same response shape including **`token`**. `401` if credentials are wrong.

### JWT (all protected APIs)

After signup or signin, send the token on every request:

- Header: `Authorization: Bearer <token>`

Public routes (no token): `/api/auth/**`, `/api/health`, `/actuator/**`.

Configure signing secret in production:

- `JWT_SECRET` — must be long enough for HS256 (UTF-8 length ≥ 32 bytes recommended).
- `JWT_EXPIRATION_MS` — access token lifetime (default `86400000` = 24h).

### API Versioning

- Primary routes are under `/api/...`
- Backward-compatible versioned aliases are available under `/api/v1/...`
- Deprecated routes removed: `expenses`, `incomes`, `reminders`
- User identity for business APIs comes from the **JWT** (`Authorization: Bearer ...`), not from `userId` query parameters.

### Cash Entries (income + expense in one source of truth)

- `POST /api/cash-entries` (Bearer required)
- `GET /api/cash-entries?direction={INCOME|EXPENSE}&page=0&size=20` (Bearer required)
- `GET /api/cash-entries/summary`
- `GET /api/cash-entries/{id}`
- `PUT /api/cash-entries/{id}`
- `DELETE /api/cash-entries/{id}`
- Create/update/delete automatically adjust account balance transactionally.

### Notes (paginated list)

- `POST /api/notes`
- `GET /api/notes?page=0&size=20`
- `GET /api/notes/{id}`
- `PUT /api/notes/{id}`
- `DELETE /api/notes/{id}`

### Ledger (paginated list)

- `POST /api/ledger`
- `GET /api/ledger?page=0&size=20`
- `GET /api/ledger/{id}`
- `PUT /api/ledger/{id}`
- `DELETE /api/ledger/{id}`

### Accounts

- `POST /api/accounts`
- `GET /api/accounts` (Bearer required)
- `GET /api/accounts/{id}`
- `PUT /api/accounts/{id}`
- `DELETE /api/accounts/{id}`

### Schedules (Bearer required)

- `POST /api/schedules`
- `GET /api/schedules?date={yyyy-mm-dd}&page=0&size=20`
- `GET /api/schedules/{id}`
- `PUT /api/schedules/{id}`
- `PUT /api/schedules/{id}/done`
- `DELETE /api/schedules/{id}`

### Secrets

- `POST /api/secrets`
- `GET /api/secrets` (Bearer required)
- `GET /api/secrets/{id}`
- `PUT /api/secrets/{id}`
- `DELETE /api/secrets/{id}`

## Database schema

Baseline schema is applied by **Flyway** (`src/main/resources/db/migration`). Legacy `schema.sql` is not used at runtime. Tables include:

- `users`
- `accounts`
- `notes`
- `cash_entries`
- `ledger`
- `schedules`
- `secrets`

## Migrations

- Flyway is enabled.
- Migration files live in `src/main/resources/db/migration`.
- Initial baseline: `V1__init_schema.sql`.

## Observability

- Request id + structured request logs are enabled.
- Actuator endpoints exposed: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.

## Pagination Envelope

- Paginated list APIs return:
  - `items`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
