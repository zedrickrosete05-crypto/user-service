# User Service

Spring Boot user-management API with MySQL, JPA, Flyway migrations, BCrypt password hashing, validation, Swagger/OpenAPI, and Actuator.

## Requirements

- Java 17 or newer
- MySQL 8+
- Docker Desktop for Testcontainers integration tests

## Run locally

Create the application database:

```sql
CREATE DATABASE user_service;
```

Set database environment variables in PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/user_service"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration` on startup. Hibernate validates the schema without modifying it.

## API documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

User registration is public. All other `/users` endpoints use HTTP Basic authentication
with the registered email and password:

```powershell
curl.exe -u user@example.com:password123 http://localhost:8080/users
```

HTTP Basic is the current authentication placeholder; JWT can replace it without
changing the protected route structure. CORS allows `http://localhost:3000` by default.
Set `CORS_ALLOWED_ORIGINS` to a comma-separated list of trusted frontend origins.

## Actuator

Available endpoints:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
http://localhost:8080/actuator/metrics
```

## API examples

Create a user:

```powershell
curl.exe -X POST http://localhost:8080/users `
  -H "Content-Type: application/json" `
  -d '{"email":"user@example.com","name":"Jane Doe","password":"password123"}'
```

List users:

```powershell
curl.exe http://localhost:8080/users
```

Get a user:

```powershell
curl.exe http://localhost:8080/users/1
```

Update a user:

```powershell
curl.exe -X PUT http://localhost:8080/users/1 `
  -H "Content-Type: application/json" `
  -d '{"email":"updated@example.com","name":"Jane Updated","password":"password456"}'
```

Delete a user:

```powershell
curl.exe -X DELETE http://localhost:8080/users/1
```

## Test

Run unit, controller, and full-context tests:

```powershell
.\mvnw.cmd verify
```

Integration tests use Testcontainers MySQL and require Docker Desktop:

```powershell
.\mvnw.cmd -Dtest=UserServiceIntegrationTest test
```

The JaCoCo report is generated at `target/site/jacoco/index.html`.
