# AI Usage

GitHub Copilot was used as an engineering assistant for inspecting this starter project, identifying correctness/build issues, proposing focused fixes, and drafting targeted tests and documentation. The working context was the KIBO take-home specification, the existing Java/Spring source, and observed Maven/Docker configuration and test results. Changes were reviewed against the transaction boundaries and API behavior rather than accepted solely from generated code.

## Concrete decisions

1. **Accepted — database row locks for inventory.** Keep MySQL as the inventory authority and lock the drop row while checking/decrementing availability. This directly serializes concurrent requests for one drop; Redis counters would require reconciliation and would not share the MySQL transaction.
2. **Accepted with a correction — release expired reservations on confirm/cancel.** An expired hold encountered by an API request must release its units even while the request returns a conflict. The initial implementation threw a runtime business exception in a normal transaction, rolling back the release. A dedicated exception with `noRollbackFor` fixes that boundary; tests check that expiration restores inventory and emits the expiration event.
3. **Accepted — wait for infrastructure health and avoid RabbitMQ's guest account.** Compose now waits for Redis and RabbitMQ health, and uses a configurable non-guest RabbitMQ user by default. The RabbitMQ `guest` account is restricted to local connections and would fail when the API connects from its separate container.
4. **Accepted after startup verification — run RabbitMQ as its image's service user.** The first real Compose startup showed `.erlang.cookie: eacces` in this Docker environment because root-owned data-volume files were not readable by the broker process. Running the service as the image's `rabbitmq` UID (`999`) prevents the mismatch on a fresh volume.
5. **Rejected — use Redis as the authoritative inventory counter.** Redis is provided and configurable, but it is not on the reservation correctness path. The existing MySQL row-lock transaction is the single source of truth and avoids cross-system inventory consistency and recovery problems.
6. **Rejected — treat publishing a RabbitMQ message inside the DB transaction as guaranteed delivery.** Current publishing behavior is retained to keep the focused example small, but a DB commit and broker publish are not atomic. The README calls this limitation out; a transactional outbox and retry/DLQ handling are the production solution rather than claiming the current implementation guarantees delivery.

## How correctness was checked

- Ran the existing Maven test command first; it exposed missing `LockModeType` imports that prevented compilation.
- Added Mockito tests for available/insufficient inventory, active cancellation/confirmation, expiration, invalid quantity, and attempts to operate on a hold through the wrong drop.
- Re-read the transaction code and verified the expired-hold exception uses `noRollbackFor`, so a real Spring transaction can commit inventory restoration before the HTTP layer maps the conflict.
- The tests are isolated from MySQL, Redis, and RabbitMQ. They do not establish real MySQL concurrency behavior; concurrent database integration tests remain an explicit next step.
