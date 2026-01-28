# Moneta

Moneta is a personal finance management application designed to provide clear and structured control over expenses, budgets and financial goals.

The project focuses on:
- Transaction tracking (manual and imports)
- Automated expense categorization
- Monthly budgets with alerts
- Financial goals and projections
- Clean and objective dashboards

Built with:
- **Backend:** Java + Spring Boot
- **Frontend:** React.js
- **Database:** PostgreSQL

This project targets multiple users with isolation by user_id via JWT (no multi-tenant sharing).

## Local development

### Postgres (local stack)
```bash
docker compose up -d postgres
```

### Backend tests (Testcontainers)
The backend test suite runs against a real Postgres container via Testcontainers. Ensure Docker is running before
executing:

```bash
cd backend
mvn clean test
```
