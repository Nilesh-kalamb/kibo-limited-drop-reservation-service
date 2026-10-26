# KIBO Limited Drop Reservation Service

A reservation API for limited-inventory drops. MySQL is the source of truth: inventory allocation is serialized with a pessimistic row lock, and each hold transition is committed in a database transaction.

## Requirements and startup

For a local run without external services, start `AuthApplication` from the IDE or run:

```shell
mvn spring-boot:run -Dspring-boot.run.main-class=com.kibo.drop.AuthApplication
```

Local runs use an in-memory H2 database by default, so data is lost when the application stops. To run with persistent MySQL, Redis, and RabbitMQ using Docker Engine with Docker Compose, run:

```shell
docker-compose up --build
```

Compose waits for MySQL, Redis, and RabbitMQ health checks before starting the API. The Docker image runs the Maven test suite as part of its build. The API listens on `http://localhost:8080`; RabbitMQ management is at `http://localhost:15672`.

The default Compose credentials are for local development only. Database, Redis, RabbitMQ, application port, hold duration, and expiry scan interval are configurable through environment variables; see `docker-compose.yml` and `src/main/resources/application.yml`. Set `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD` together if changing the local RabbitMQ credentials.

On first startup, three active drops are seeded. The seed runs only when the drops table is empty.

To run unit tests locally (no database, Redis, or RabbitMQ required):

```shell
mvn test
```

## API

All JSON errors use an `error` field. Invalid request bodies return `400`, missing drops/holds return `404`, and business conflicts (for example, insufficient inventory, an expired hold, or a repeated transition) return `409`.

### Browser authentication

Open `http://localhost:8080/` for the browser login and registration page. Use the same email and password for login that you submitted when registering.

### List drops

```bash
curl http://localhost:8080/api/drops
```

### Create a drop

```bash
curl -X POST http://localhost:8080/api/drops \
  -H "Content-Type: application/json" \
  -d '{"name":"Limited Product","totalUnits":25,"startsAt":"2030-01-01T00:00:00Z"}'
```

### Place a hold

```bash
curl -X POST http://localhost:8080/api/drops/1/holds \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer-123","quantity":2}'
```

### Confirm or cancel a hold

```bash
curl -X POST http://localhost:8080/api/drops/1/holds/{HOLD_ID}/confirm
curl -X POST http://localhost:8080/api/drops/1/holds/{HOLD_ID}/cancel
```

The hold expires after `HOLD_DURATION_SECONDS` (300 seconds by default). The expiry scheduler releases inventory, and a confirm/cancel request that races after expiry also commits the expiration before returning a conflict. Hold IDs are scoped to the drop in the URL; using another drop's ID returns `404`.

## Design decisions and trade-offs

- **Inventory safety:** `POST .../holds` locks its drop row with `PESSIMISTIC_WRITE`, then checks and decrements availability in the same transaction. A competing request must wait and re-read the updated row, so it cannot reserve units already held by another transaction.
- **State transitions:** only `ACTIVE` holds can be confirmed, cancelled, or expired. Hold-row locking prevents repeated transitions from releasing inventory twice. Cancellation and expiration restore units; confirmation does not.
- **Expiry:** a scheduled scan finds candidates and locks each hold before changing state. The on-demand confirm/cancel paths also handle expiration, so correctness does not depend on scheduler timing. Expired-hold conflicts are configured not to roll back the inventory release.
- **RabbitMQ:** the service publishes `hold.created`, `hold.confirmed`, `hold.cancelled`, and `hold.expired` to the `drop.events` topic exchange; a durable audit queue is bound to `hold.*`. Publishing currently occurs in the request transaction, not through a transactional outbox. An outbox and retry/dead-letter policy would be needed to guarantee delivery across process or broker failures.
- **Redis:** included and configured, but intentionally not used as an inventory authority or needed for correctness. This small service has no read cache yet; adding one would require explicit invalidation and must never bypass MySQL's locked inventory update.
- **Persistence:** Hibernate schema update is convenient for this take-home. Production rollout should use versioned migrations and enforce operational backups.
- **Security/scope:** no authentication, authorization, or per-customer idempotency is implemented. Do not expose this deployment publicly without adding those controls.

Tests are JUnit 5/Mockito unit tests and run without live infrastructure. They cover inventory allocation/rejection, cancellation, confirmation, expiration, and drop-scoped hold operations. The tests do not claim to substitute for concurrent MySQL integration tests.

## Next steps

Add MySQL concurrency/integration tests, a transactional outbox with broker retry and dead-letter handling, idempotency keys, authentication/authorization, schema migrations, and production metrics/tracing. Redis can then be introduced for measured read-heavy paths with explicit invalidation.
