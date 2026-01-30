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
docker compose -f docker-compose.dev.yml up -d postgres
```

### Backend tests (Testcontainers)
Os testes de integração usam Testcontainers, então é necessário ter Docker disponível para executá-los. Os testes
unitários rodam sem Docker.

## CI/CD (Backend)

### GitHub Actions workflows
- **CI** (`.github/workflows/ci.yml`): roda testes unitários sempre e testes de integração (Testcontainers) apenas quando o Docker está disponível no runner.
- **Deploy** (`.github/workflows/deploy.yml`): no push para `main`, conecta via SSH na VM e faz deploy via Docker Compose, com healthcheck pós-deploy.

### Secrets necessários no GitHub
Configure os seguintes secrets no repositório (Settings → Secrets and variables → Actions):
- `VM_HOST`: IP público da VM.
- `VM_USER`: usuário SSH (ex: `ubuntu`).
- `VM_SSH_KEY`: conteúdo da chave privada.
- `VM_SSH_PORT`: porta SSH (ex: `22`).
- `VM_DEPLOY_PATH`: caminho do repo na VM (ex: `/home/ubuntu/moneta/moneta`).

### Deploy manual na VM
Caso precise executar manualmente na VM:
```bash
cd /home/ubuntu/moneta/moneta
git fetch origin main
git reset --hard origin/main
docker compose down
docker compose up -d --build
curl -fsS http://localhost:8080/actuator/health
```

### Rodando o CI localmente
Testes unitários (sempre):
```bash
mvn -B -f backend/pom.xml test
```

Testes de integração (Testcontainers, requer Docker):
```bash
mvn -B -f backend/pom.xml -Pit verify
```
