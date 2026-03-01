# Event-Driven Ticketing Platform

Microservices-based ticketing system (Java 21, Spring Boot 3) demonstrating event-driven design, distributed transaction handling, concurrency control, and production-oriented practices.

### Architecture

![System Architecture](architect.jpg)

---

## Project Description

- **Microservices + shared event library:** Split by bounded context (User, Ticket, Payment, Inventory, Notification) with a common event contract. *Result:* Domain-oriented design, easier to maintain and deploy services independently.

- **API Gateway + service discovery:** Single entry point with path-based routing, CORS, and unified error responses; Consul for registration and lookup. *Result:* One client-facing endpoint and load-balanced traffic to backend services.

- **Database-per-service:** Each service owns its own MySQL schema. *Result:* Data isolation per domain and independent scaling/deployment.

- **Event-driven choreography (Kafka):** Coordinate booking–payment–notification flows via events across services. *Result:* Cross-service consistency without distributed 2PC; eventual consistency where appropriate.

- **Idempotent consumers and clear event contracts:** Safe retries and duplicate handling; typed events with consistent payloads. *Result:* Correct state under Kafka retries or duplicates; no duplicate payment or notification records.

- **Distributed locking (Redisson):** Lock around seat reserve/release in Inventory. *Result:* No overbooking under concurrent requests.

- **Optimistic locking on Ticket:** Version check on status updates. *Result:* No lost updates under concurrent writes.

- **Cache-aside (Redis):** Cache for frequently read entities with TTL and eviction on write. *Result:* Lower DB load and faster reads while keeping cache coherent.

- **Elasticsearch:** User and ticket search by keyword. *Result:* Search workload off primary DB; better performance for query-heavy use cases.

- **Circuit breaker and retry (Resilience4j):** On calls from Ticket to Inventory with fallback. *Result:* Fail fast when downstream is slow or down; no cascading latency.

- **Health, metrics, and API docs:** Actuator, Prometheus, and OpenAPI/Swagger for all services. *Result:* Easier operations, monitoring, and API consumption.

- **Global exception handling, error DTOs, and validation:** Centralized error handling and Bean Validation on inputs. *Result:* Consistent API behavior and clearer debugging; REST contract separated from domain.

- **Maven multi-module and Docker Compose:** Single build; infrastructure (DB, cache, message broker, discovery, search) via Compose. *Result:* Simple local and containerized runs; quick onboarding.

---

## Tech Stack

- **Core** — Java 21, Spring Boot 3.2, Spring Cloud 2023.0, Maven (multi-module)
- **API & Gateway** — Spring Web (REST), Spring Cloud Gateway (reactive)
- **Messaging** — Apache Kafka 7.5, Spring Kafka
- **Data** — MySQL 8, Spring Data JPA, Redis 7, Redisson, Elasticsearch 8.x
- **Discovery** — HashiCorp Consul
- **Resilience** — Resilience4j (circuit breaker, retry)
- **Docs & Observability** — SpringDoc OpenAPI, Actuator, Micrometer/Prometheus
- **Infrastructure** — Docker, Docker Compose


