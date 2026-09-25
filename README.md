# GraphQL Security POC

A runnable local POC demonstrating authentication, operation contracts, root-field capability validation, role permissions, tenant isolation, ownership, and field-level authorization.

## Prerequisites

- Java 21+
- Node.js 20+
- Podman
- tmux
- npm

On macOS, start the Podman machine once before the first run:

```bash
podman machine start
```

Verify that the Podman connection is available:

```bash
podman info
```

## Start dependencies

```bash
podman compose -f compose.yaml -p graphql-security-poc up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- Keycloak on `http://localhost:8081`
- Realm `security-poc`

The development users are listed in [keycloak/realm-export.json](keycloak/realm-export.json). Every user password is the username. For example, `alice` / `alice` is a customer in tenant A.

## Start the backend

```bash
cd backend
./gradlew bootRun
```

The GraphQL endpoint is `http://localhost:8080/graphql` and GraphiQL is available there when `GRAPHQL_GRAPHIQL_ENABLED=true`.

Flyway creates the schema and deterministic seed data. The application uses the database and JWT settings in `backend/src/main/resources/application.yml`.

## Start everything in tmux

The complete development stack can be started in one tmux session:

```bash
./scripts/start-tmux.sh
```

The script starts PostgreSQL and Keycloak with Podman, waits for PostgreSQL, and opens these panes:

- Backend: `./gradlew bootRun`
- Frontend: `npm run dev`
- Infrastructure logs: `podman compose -f compose.yaml -p graphql-security-poc logs -f`
- Shell: interactive project shell

The script attaches to the `graphql-security-poc` tmux session. Open `http://localhost:5173` after the frontend is ready. Vite proxies `/graphql` to the backend.

Stop the application panes and containers with:

```bash
./scripts/stop.sh
```

To run services manually instead:

```bash
cd backend
./gradlew bootRun
```

and:

```bash
cd frontend
npm install
npm run dev
```

## Development accounts

| User | Role | Tenant |
|---|---|---|
| `alice` | CUSTOMER | A |
| `bob` | CUSTOMER | A |
| `charlie` | CUSTOMER | B |
| `support-a` | SUPPORT | A |
| `admin-a` | ADMIN | A |
| `support-b` | SUPPORT | B |
| `admin-b` | ADMIN | B |

All passwords equal the usernames in the development realm. These credentials are not suitable for production.

## Security demonstration

1. Log in as `alice` and view Alice's orders.
2. Request Bob's order using the playground: denied by ownership.
3. Request Charlie's order: denied by tenant boundary.
4. Run an unknown or anonymous operation: denied by the operation contract.
5. Run `query GetOrder { orders { id } }`: the full policy rejects the root-field mismatch.
6. Request `internalNotes` as Alice: denied by field authorization.
7. Log in as `admin-a` and request an internal note from tenant A: allowed.
8. Request tenant B's order as `admin-a`: denied; administrators are tenant administrators.

The name-only operation vulnerability is available only as an explicit development setting:

```bash
NAME_ONLY_OPERATIONS_ENABLED=true ./gradlew bootRun
```

Do not use that setting outside an isolated demonstration. The default is the full structural policy.

## Tests

Backend unit tests run without a container runtime:

```bash
cd backend
./gradlew test
```

The test suite covers operation policy behavior and authorization policy behavior. The unit tests do not require a container runtime.

Frontend tests and production build:

```bash
cd frontend
npm test
npm run build
```

## Project layout

```text
backend/       Spring Boot, Spring GraphQL, JPA, Flyway
frontend/      React, TypeScript, Vite, Keycloak JS
keycloak/      Development realm import
scripts/       Start and stop helpers
docs/          Architecture, security model, and demo notes
```

## Production hardening

This repository is a POC, not a production template. Before production use, add query depth/complexity and result-size limits, explicit timeout and rate limiting, centralized audit logging, secret management, TLS, a real user-provisioning process, and a reviewed policy engine where appropriate.
