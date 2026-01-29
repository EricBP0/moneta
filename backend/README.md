# Moneta Backend

Spring Boot backend application for Moneta financial management system.

## Production Deployment

### Required Environment Variables

The following environment variables **must** be set when running with the `prod` profile:

#### Database Configuration
- `DB_URL` - JDBC connection URL (e.g., `jdbc:postgresql://db-host:5432/moneta`)
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password

**Security Note:** The application uses `sslmode=verify-full` for PostgreSQL connections. Ensure your database server has a valid SSL certificate and configure the trust store accordingly.

#### JWT Configuration
- `JWT_SECRET` - Secret key for JWT token signing
  - **Must be at least 32 characters (256 bits) for secure HS256 signing**
  - Use a strong, randomly generated secret
  - Never use default values like "change-me" or "secret"

#### Optional Configuration
- `JWT_ACCESS_TTL_MINUTES` - Access token time-to-live in minutes (default: 60)
- `JWT_REFRESH_TTL_DAYS` - Refresh token time-to-live in days (default: 30)

### Building the Docker Image

```bash
cd backend
docker build -t moneta-backend .
```

**Note:** The Docker build runs the full test suite. Ensure all tests pass before deployment.

### Running the Container

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://your-db-host:5432/moneta \
  -e DB_USER=your-db-user \
  -e DB_PASSWORD=your-db-password \
  -e JWT_SECRET=your-secure-32-char-minimum-secret \
  --name moneta-backend \
  moneta-backend
```

Or use an environment file:

```bash
docker run -p 8080:8080 --env-file .env moneta-backend
```

### Health Checks

The application exposes health check endpoints at:
- `/actuator/health` - Basic health status (public)
- `/actuator/info` - Application information (public)

The Docker image includes an automatic health check that queries `/actuator/health` every 30 seconds with a 60-second startup grace period to allow for database migrations.

### JVM Configuration

The default JVM options set `MaxRAMPercentage=75.0`. You can override these by setting the `JAVA_OPTS` environment variable:

```bash
docker run -e JAVA_OPTS="-XX:MaxRAMPercentage=80.0 -XX:+UseG1GC" ...
```

## Development

For local development, use the default application profile which uses the configuration from `application.yml`.

```bash
./mvnw spring-boot:run
```

## Database Migrations

Flyway migrations are enabled in production and run automatically on application startup. The application uses `hibernate.ddl-auto=validate` to ensure the schema matches the entity definitions without making destructive changes.
