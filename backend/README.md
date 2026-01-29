# Moneta Backend

Spring Boot backend application for Moneta financial management system.

## Production Deployment

### Required Environment Variables

The following environment variables **must** be set when running with the `prod` profile:

#### Database Configuration
- `DB_URL` - JDBC connection URL including SSL parameters
  - **Required format:** `jdbc:postgresql://db-host:5432/moneta?sslmode=verify-full`
  - **Must include** `sslmode=verify-full` for production security
  - Other query parameters can be added with `&` (e.g., `?sslmode=verify-full&currentSchema=public`)
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password

**Security Note:** The application requires `sslmode=verify-full` for PostgreSQL connections. This requires the database server to present a certificate that the client trusts. In most deployments you should either:
- Use a server certificate signed by a public or organization-wide trusted CA, **or**
- Add your database CA certificate to the Java trust store (e.g., via `keytool`), **or**
- Include a custom CA certificate using JDBC URL parameters such as `sslrootcert` (e.g., `jdbc:postgresql://.../moneta?sslmode=verify-full&sslrootcert=/path/to/server-ca.pem`)

See the [PostgreSQL JDBC SSL documentation](https://jdbc.postgresql.org/documentation/head/ssl/) for details and platform-specific instructions.

#### JWT Configuration
- `JWT_SECRET` - Secret key for JWT token signing
  - **Must be at least 32 characters (256 bits) for secure HS256 signing**
  - Generate a secure random secret: `openssl rand -base64 32`
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

By default, the Docker image runs with the `prod` Spring profile. Ensure all required environment variables are set:

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://your-db-host:5432/moneta?sslmode=verify-full \
  -e DB_USER=your-db-user \
  -e DB_PASSWORD=your-db-password \
  -e JWT_SECRET=your-securely-stored-32-char-secret \
  --name moneta-backend \
  moneta-backend
```

**Important:** For production deployments:
- Generate JWT_SECRET once with `openssl rand -base64 32` and store it securely (in a secrets manager, vault, or .env file)
- Do NOT use inline generation like `$(openssl rand -base64 32)` as this creates a new secret on each container restart, invalidating all existing tokens

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

### Migrating from Previous Configuration

If you are migrating from a previous version, note the following changes:

- **Environment Variables**: The production configuration now uses:
  - `DB_URL` (full JDBC URL including `?sslmode=verify-full`) instead of separate `DB_HOST`, `DB_PORT`, `DB_NAME`
  - `DB_PASSWORD` instead of `DB_PASS`
- **SSL Mode**: You must include `sslmode=verify-full` in your `DB_URL` query parameters (instead of the previously auto-appended `sslmode=require`)
- **Spring Profile**: The Docker image automatically activates the `prod` profile via the `SPRING_PROFILES_ACTIVE` environment variable

## Development

For local development, use the default application profile which uses the configuration from `application.yml`.

```bash
./mvnw spring-boot:run
```

## Database Migrations

Flyway migrations are enabled in production and run automatically on application startup. The application uses `hibernate.ddl-auto=validate` to ensure the schema matches the entity definitions without making destructive changes.
