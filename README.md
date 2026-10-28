# Product Secure API

A full-stack product inventory application built with **Spring Boot, Spring Security, JWT, PostgreSQL, Redis, React, and Docker Compose**.

The project demonstrates authenticated product management, role-based authorization, caching, OTP-based email verification, and audit logging.

> **Before running:** Review `.env.example` and the actual `compose.yaml` in this repository. Fill in the required local environment values. Never commit `.env` or real secrets.

## 1. Run the application first

### Prerequisites

- Docker Desktop (with Docker Compose) installed and running
- Git
- A copy of this repository

Node.js is not required to run the containerized frontend, because Docker builds it from the frontend source.

### Step 1 — Clone the repository

```bash
git clone https://github.com/RounakGope/SECURE-PRODUCT-API.git
cd SECURE-PRODUCT-API
```

### Step 2 — Create your local environment file

Copy the example file:

**PowerShell**
```powershell
Copy-Item .env.example .env
```

**macOS / Linux**
```bash
cp .env.example .env
```

Open `.env` and set the values required by your `compose.yaml` and `application.properties`, including the PostgreSQL database name, username, password, JWT secret, and any other required configuration.

- Use a strong, unique JWT secret.
- Do not commit `.env`.
- `.env.example` should contain placeholders, not real credentials.
- If email delivery uses SMTP, configure the relevant SMTP variables as required by your backend.

### Step 3 — Build and start the services

Run this command from the repository root, where `compose.yaml` is located:

```bash
docker compose up --build -d
```

This builds the application images and starts the services defined in Compose.

Check their status:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f
```

Or view one service at a time (service names must match `compose.yaml`):

```bash
docker compose logs -f frontend
docker compose logs -f api
docker compose logs -f postgres
docker compose logs -f redis
```

### Step 4 — Open the application

With the example port mappings used by this project:

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html

If you change the host port mappings in `compose.yaml`, use the ports configured there.

### Step 5 — Try the application

1. Register an account from the frontend.
2. Log in with that account.
3. View the product inventory.
4. Test product creation, editing, and deletion according to the logged-in user's role.
5. Test OTP generation and email verification if enabled in the current backend configuration.
6. Open Swagger UI to inspect and try the API endpoints. For protected endpoints, authenticate using the JWT issued by the login endpoint.

### Stop the application

Stop and remove the containers:

```bash
docker compose down
```

To also delete the PostgreSQL named volume and its stored database data, run the following only if you intentionally want to reset the local database:

```bash
docker compose down -v
```

**Warning:** `-v` deletes the database volume and its data.

---

## 2. Architecture and request flow

```text
Browser
  |
  | http://localhost:5173
  v
React frontend served by Nginx
  |
  | /api/... forwarded inside the Docker network
  v
Spring Boot REST API (api:8080)
  |                         |
  |                         +--> Redis (cache / OTP, where configured)
  |
  +--> PostgreSQL (persistent application data)
  |
  +--> Audit logging (database audit table)
```

### Request flow

1. The user opens the React frontend.
2. The user registers or logs in.
3. The backend validates credentials and issues a JWT.
4. The frontend sends the token in the `Authorization: Bearer <token>` header for protected API requests.
5. Spring Security validates the token and enforces authorization rules.
6. Product requests are handled by the backend and persisted in PostgreSQL.
7. Redis can serve cached product data and store short-lived OTP data according to the implemented service logic and TTL settings.
8. The audit aspect records the actions configured for auditing.
9. Nginx serves the frontend and proxies `/api/` requests to the backend container, so the browser can use the frontend's origin.

Inside Docker Compose, services should communicate using their **Compose service names** (for example, `api`, `postgres`, and `redis`) rather than `localhost`. `localhost` inside a container refers to that same container.

---

## 3. Technology stack

### Backend
- Java and Spring Boot
- Spring Security
- JWT bearer-token authentication
- Spring Data JPA
- PostgreSQL
- Redis
- Spring AOP for configured audit logging
- OTP-based email verification flow
- Springdoc / Swagger UI for API documentation

### Frontend
- React
- Vite
- Axios for API requests
- Responsive inventory dashboard

### DevOps
- Docker
- Docker Compose
- Multi-stage backend and frontend builds
- Nginx to serve the frontend production build and proxy API requests

---

## 4. Main features

- User registration and login
- JWT-based authentication
- Role-based access control for protected operations
- Product inventory listing and management
- Product create, update, and delete operations, subject to permissions
- Redis caching for configured product queries
- Configurable cache TTL and Redis eviction policy
- OTP generation and email verification flow
- Audit logging for actions covered by the audit aspect
- Swagger UI for exploring the API
- Docker Compose setup for running the application and its supporting services

The exact available endpoints and authorization rules are defined by the backend controllers and Spring Security configuration in this repository.

---

## 5. API documentation and authentication

When the backend is running, open:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON (if enabled): http://localhost:8080/v3/api-docs

Use the login endpoint to obtain a JWT. For protected requests, send:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

Replace `YOUR_JWT_TOKEN` with the token returned by your API. Do not paste real tokens into GitHub issues, screenshots, or public documentation.

For exact endpoint paths, request payloads, response schemas, and required roles, use Swagger UI or inspect the controller and DTO classes.

---

## 6. Configuration

The backend reads configuration from Spring properties and environment variables. The exact variable names required at runtime are the ones referenced by `application.properties` and `compose.yaml`.

Common configuration categories include:

| Category | Purpose |
|---|---|
| Application port | Port used by the Spring Boot API |
| PostgreSQL | Database URL, database name, username, and password |
| Redis | Redis host, port, and timeout |
| JWT | Signing secret and token expiration |
| Email / OTP | SMTP configuration if real email delivery is enabled |

For local Docker Compose, the API container should connect to PostgreSQL using the Compose service hostname and container port (commonly `postgres:5432`), and Redis using its Compose service hostname and container port (commonly `redis:6379`). The host machine's published ports are for access from outside the Compose network.

Do not use production credentials in a public repository. Use secret management appropriate to the deployment environment.

---

## 7. Development workflow

### Run the frontend outside Docker (optional)

If you want to work on the React frontend directly, install a compatible Node.js version, then run:

```bash
cd frontend
npm ci
npm run dev
```

The Vite development proxy in `frontend/vite.config.js` should point to the backend address used for local development. Check that file before running the frontend this way.

### Rebuild after code changes

After changing backend or frontend source code, rebuild the relevant images:

```bash
docker compose up --build -d
```

Follow the logs to diagnose startup or runtime issues:

```bash
docker compose logs --tail=100 api
docker compose logs --tail=100 frontend
```

---

## 8. Repository structure

```text
SECURE-PRODUCT-API/
├── src/                   # Spring Boot backend source and resources
├── frontend/
│   ├── src/               # React application
│   ├── Dockerfile          # Frontend production image
│   ├── nginx.conf          # Static serving and API proxy
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.js
├── Dockerfile              # Backend image
├── compose.yaml             # Application and supporting services
├── .dockerignore
├── .gitignore
├── .env.example             # Safe template; no real secrets
├── pom.xml
└── README.md
```

This tree describes the intended layout. Keep it aligned with the actual files in the repository.

---

## 9. Troubleshooting

### A service does not start

```bash
docker compose ps
docker compose logs --tail=150 api
docker compose logs --tail=150 frontend
```

Check the logs for missing environment variables, build failures, database connection errors, or port conflicts.

### The frontend loads but API calls fail

- Check that the `api` service is running.
- Check the Nginx `location /api/` proxy configuration in `frontend/nginx.conf`.
- Confirm that the proxy target uses the actual backend Compose service name and container port.
- Check browser Developer Tools → Network for the failed request and response.
- Inspect backend logs for authentication errors or endpoint mismatches.

### Database or Redis connection errors

From inside the API container, use the Compose service hostname and container port—not `localhost`. Verify the service names, health checks, credentials, and environment variables in `compose.yaml`.

### Port already in use

If a host port is already occupied, update the host side of the relevant port mapping in `compose.yaml`, then restart the services.

---

## 10. Security notes

- Never commit `.env`, JWT secrets, database passwords, SMTP credentials, or real tokens.
- Keep `.env.example` limited to placeholder values.
- Rotate any credentials that were accidentally exposed in Git history.
- Client-side UI checks are not a security boundary; enforce authorization in Spring Security and backend service/controller logic.
- Use HTTPS and deployment-appropriate secret management when hosting publicly.
- Treat OTPs and authentication tokens as sensitive information.

---

## License

Add a license file if you intend to grant others permission to reuse or modify this project. If no license is provided, do not assume the repository is open for unrestricted reuse.
