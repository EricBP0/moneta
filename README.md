# Moneta

[![Deploy](https://github.com/EricBP0/moneta/actions/workflows/deploy.yml/badge.svg?branch=main&event=deployment)](https://github.com/EricBP0/moneta/actions/workflows/deploy.yml)

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
docker compose -f docker-compose.dev.yml up -d postgres
```

### Backend tests (Testcontainers)
Integration tests use Testcontainers, so Docker must be available to run them. Unit tests run without Docker.

## CI/CD (Backend)

### GitHub Actions workflows
- **CI** (`.github/workflows/ci.yml`): runs unit tests always and integration tests (Testcontainers) only when Docker is available on the runner.
- **Deploy** (`.github/workflows/deploy.yml`): on push to `main`, connects via SSH to the VM and deploys via Docker Compose, with post-deploy healthcheck.

### Required GitHub Secrets
Configure the following secrets in the repository (Settings → Secrets and variables → Actions):
- `VM_HOST`: Public IP of the VM.
- `VM_USER`: SSH user (e.g., `ubuntu`).
- `VM_SSH_KEY`: Private key content.
- `VM_HOST_KEY`: Pinned host key line from known_hosts (e.g., output from `ssh-keyscan -H -p 22 <VM_HOST>`).
- `VM_SSH_PORT`: SSH port (e.g., `22`).
- `VM_DEPLOY_PATH`: Repository path on the VM (e.g., `/home/ubuntu/moneta/moneta`).

### Manual deploy on the VM
If you need to run manually on the VM:
```bash
cd /home/ubuntu/moneta/moneta
git fetch origin main
git reset --hard origin/main
docker compose down
docker compose up -d --build
curl -fsS http://localhost:8080/actuator/health
```

### Running CI locally
Unit tests (always):
```bash
mvn -B -f backend/pom.xml test
```

Integration tests (Testcontainers, requires Docker):
```bash
mvn -B -f backend/pom.xml -Pit verify
```
