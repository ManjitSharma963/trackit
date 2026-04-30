# API Migration Guide

## Versioning

- Current routes are available in both:
  - `/api/...`
  - `/api/v1/...`

## Removed Endpoints

- `/api/expenses` (replaced by `/api/cash-entries?direction=EXPENSE`)
- `/api/incomes` (replaced by `/api/cash-entries?direction=INCOME`)
- `/api/reminders` (replaced by `/api/schedules`)

## Authentication (JWT)

- Sign in or sign up returns a **`token`** (JWT).
- Call protected APIs with: `Authorization: Bearer <token>`
- **`X-User-Id` is no longer used** for authorization.

## Replacement Mapping

- Create expense/income:
  - old: `POST /api/expenses` or `POST /api/incomes`
  - new: `POST /api/cash-entries` with `direction=EXPENSE|INCOME`

- List expense/income:
  - old: `GET /api/expenses?userId=...` or `GET /api/incomes?userId=...`
  - new: `GET /api/cash-entries?direction=EXPENSE|INCOME&page=0&size=20` with `Authorization: Bearer <token>`

- Reminders:
  - old: `/api/reminders`
  - new: `/api/schedules`

## Deprecation Policy

- Keep `/api/v1` aliases for all supported resources during client migration.
- Breaking changes must include:
  - updated migration guide
  - updated README endpoint examples
  - at least one release cycle notice before route removal
