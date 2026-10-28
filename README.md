# Secure Product API

A role-based product management REST API built with Spring Boot, JWT authentication, PostgreSQL, Redis caching, email OTP verification, and audit logging. Docker Compose is used to run the API and its infrastructure locally.

> **Verification note:** This README is based on the supplied `pom.xml` and controller screenshots. Confirm the product controller's class-level `@RequestMapping`, DTO fields, exact response codes, and Docker image versions against the final branch before submission.

## Tech stack

| Technology | Version / details |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.2 |
| Lombok | 1.18.42 |
| Spring Data JPA | Managed by Spring Boot parent |
| Spring Security | Managed by Spring Boot parent |
| Spring Data Redis | Managed by Spring Boot parent |
| Spring Validation | Managed by Spring Boot parent |
| Spring Mail | Managed by Spring Boot parent |
| OAuth2 Resource Server | Managed by Spring Boot parent |
| Spring AOP | Explicitly declared as `7.1.0-M2` in the current POM |
| PostgreSQL | Version defined by Docker configuration |
| Redis | Version defined by Docker configuration |
| Maven | Build tool; version is not pinned in `pom.xml` |
| Docker Compose | Local container orchestration |

## Features

- JWT-based authentication.
- `ADMIN` and `USER` roles with role-aware authorization.
- Custom method-level authorization annotation.
- Product CRUD with Bean Validation.
- Admin-only create/update/delete; authenticated users can read products.
- Email OTP generation and verification.
- Redis caching for product list and product-by-ID reads, with a 60-second TTL.
- Cache eviction after successful product mutations.
- Audit logging to a separate `audit_log` table for mutating product operations.
- Docker Compose services for Spring Boot, PostgreSQL, and Redis.
- Demo admin initialization from environment variables when configured and when the email does not already exist.

Only describe a feature as complete if it is present and working in the final branch.

## Prerequisites

- Git
- Docker Engine or Docker Desktop with Docker Compose v2

When using Docker Compose, you do not need to install Java, Maven, PostgreSQL, or Redis locally.

## Run with Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/RounakGope/SECURE-PRODUCT-API.git
cd SECURE-PRODUCT-API
```

### 2. Create `.env` from `.env.example`

Linux / macOS:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Open `.env` and replace placeholders. Example variable names from the Compose configuration include:

```dotenv
APP_PORT=8080
DEMO_ADMIN_EMAIL=admin@example.com
DEMO_ADMIN_PASSWORD=replace_with_a_strong_password
JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRATION_DURATION=600
POSTGRES_DB=productsecure
POSTGRES_USER=appuser
POSTGRES_PASSWORD=replace_with_a_database_password
REDIS_TIMEOUT=500ms
```

This is an illustrative subset, not a replacement for the repository's complete `.env.example`. Keep all required variables from that file, including mail/SMTP configuration if needed for OTP delivery. Make sure values and names match `compose.yaml` and Spring configuration.

**Never commit `.env` or real secrets.** Commit `.env.example` with placeholders only. Rotate any secret that was previously pushed to a public repository; deleting the file in a later commit does not remove it from Git history.

### 3. Validate and start the stack

Run from the repository root:

```bash
docker compose config
docker compose up --build -d
```

Check services and logs:

```bash
docker compose ps
docker compose logs -f api
```

Use the actual service names defined in `compose.yaml` if they differ.

### 4. Access the API

Assuming `APP_PORT=8080`:

- API base URL: `http://localhost:8080`
- Swagger UI, if enabled: `http://localhost:8080/swagger-ui/index.html`

If the Compose file defines a frontend service, access it at the host port mapped by that service. The backend can also be tested directly using Postman.

### 5. Demo admin

Set `DEMO_ADMIN_EMAIL` and `DEMO_ADMIN_PASSWORD` in `.env`. The initializer creates an admin only when both values are configured and the email does not already exist. If the email already exists, it skips initialization rather than changing that account's role or password.

Log in using `POST /api/auth/login`. Keep demo credentials private or use dedicated non-sensitive demo credentials.

## API contract

Example base URL: `http://localhost:8080`

The authentication routes below are visible in the provided `AuthController` screenshot. Product routes are based on the product method mappings shown; confirm the class-level mapping in `ProductController`.

### Authentication endpoints

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a user | Public |
| `POST` | `/api/auth/login` | Log in and receive a JWT | Public |
| `POST` | `/api/auth/generate-otp` | Generate email verification OTP | Authenticated principal appears to be used |
| `POST` | `/api/auth/verify-email` | Verify email using OTP | Confirm security configuration |

Example registration request (confirm exact field names in `UserRegistrationRequest`):

```json
{
  "name": "Demo User",
  "email": "user@example.com",
  "password": "StrongPassword123!"
}
```

Example login request (confirm exact field names in `LoginRequest`):

```json
{
  "email": "user@example.com",
  "password": "StrongPassword123!"
}
```

Use the returned JWT on protected endpoints:

```http
Authorization: Bearer <JWT>
```

The `generate-otp` controller method accepts a `java.security.Principal`, so it appears to identify the current user from the authenticated principal rather than a request body.

Example verification request (confirm exact fields in `OtpVerificationRequest`):

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

### Product endpoints

| Method | Endpoint | Purpose | Role |
|---|---|---|---|
| `POST` | `/api/products` | Create product | `ADMIN` |
| `GET` | `/api/products` | List products | `USER` / `ADMIN` |
| `GET` | `/api/products/{id}` | Get product by ID | `USER` / `ADMIN` |
| `PUT` | `/api/products/{id}` | Update product | `ADMIN` |
| `DELETE` | `/api/products/{id}` | Delete product | `ADMIN` |

Confirm that `/api/products` matches the class-level mapping in your controller.

Example create request (confirm exact fields in `ProductCreate`):

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with adjustable DPI.",
  "price": 799.00,
  "stock": 50,
  "category": "Electronics"
}
```

Update requests use `ProductUpdate`; inspect that DTO for its exact fields and validation constraints. Product controller responses use `ProductResponse` and `List<ProductResponse>`.

### Role matrix

| Operation | ADMIN | USER |
|---|---:|---:|
| Register / log in | Yes | Yes |
| Read product list | Yes | Yes |
| Read product by ID | Yes | Yes |
| Create product | Yes | No |
| Update product | Yes | No |
| Delete product | Yes | No |

### Validation and errors

- Invalid request fields should be rejected by validation.
- Missing or invalid JWT should prevent access to protected routes.
- A `USER` attempting an admin-only mutation should be denied.
- A missing product should return the status configured by the controller/service.
- Confirm exact status codes and error response JSON against the running application before treating them as a strict API contract.

## Redis caching strategy

- Cache the product list and individual product reads by ID.
- Use a 60-second time-to-live (TTL).
- Evict or invalidate relevant cache entries after successful product create/update/delete operations.
- PostgreSQL remains the source of truth; Redis is not permanent product storage.

## Audit logging

Product mutation audit events are intended to be recorded in the separate `audit_log` table. The assignment calls for actor/user ID, action, timestamp, and before/after state. Verify the final audit entity and actual stored fields against the implementation.

## Postman collection

Import `postman/Secure-Product-API.postman_collection.json` into Postman.

Collection variables include:

- `baseUrl`: `http://localhost:8080`
- `token`: populated by a login request if the response contains `token` or `accessToken`
- `adminEmail` / `adminPassword`: use the credentials configured in `.env`
- `userEmail` / `userPassword`: use a registered normal user
- `productId`: replace with an existing product ID

Recommended test flow:

1. Register a normal user, or use the configured demo admin.
2. Log in and capture the JWT.
3. As `ADMIN`, create, list, fetch, update, and delete products.
4. As `USER`, verify product reads work and mutations are denied.
5. Test OTP generation and verification if email delivery is configured.

The collection's routes and request bodies are based on screenshots and typical DTO fields. Adjust them if the actual DTOs or controller mappings differ.

## Assignment goals and implementation summary

| Assignment goal | Implementation to demonstrate |
|---|---|
| JWT authentication with `ADMIN` and `USER` roles | JWT login and role-aware Spring Security authorization |
| Custom method-level authorization annotation | Custom annotation used for method-level access checks |
| Product CRUD with Bean Validation | Create/list/get/update/delete endpoints and validated request DTOs |
| Audit logging | AOP/interceptor-based mutation audit records in `audit_log` |
| Redis read caching | Product list and get-by-ID caching with 60-second TTL |
| Cache invalidation | Eviction after successful product mutations |
| Dockerization | Multi-stage backend Dockerfile and Compose services for API, PostgreSQL, and Redis |
| Postman demonstration | Importable collection for authentication, CRUD, and role-based checks |
| Documentation | API contract, role matrix, caching strategy, and local startup guide |

This table maps the assignment requirements to the intended project design. Confirm each item against the final branch before submission.

## Troubleshooting

View logs:

```bash
docker compose logs -f api
docker compose logs -f postgres
docker compose logs -f redis
```

Rebuild after code changes:

```bash
docker compose up --build -d
```

Validate Compose configuration:

```bash
docker compose config
```

**Destructive reset:** The following removes named volumes, including persisted PostgreSQL data. Use only if you intentionally want a fresh database.

```bash
docker compose down -v
docker compose up --build -d
```

## Security notes

- Do not commit `.env`, SMTP passwords, JWT secrets, or real database passwords.
- Use a long, randomly generated JWT secret.
- Rotate any secret exposed in Git history.
- Use dedicated demo credentials; never reuse personal passwords.
