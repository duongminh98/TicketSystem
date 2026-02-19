# PROJECT BLUEPRINT - Event-Driven Ticketing Platform

> **File này là TÀI LIỆU THAM CHIẾU ĐẦY ĐỦ** chứa mọi chi tiết kỹ thuật của dự án.
> Dùng kèm với file `5_DAY_PLAN.md` để xây dựng lại repo từ đầu.

---

## CÁCH SỬ DỤNG 2 FILE NÀY

```
Bạn có 2 file:

📋 PROJECT_BLUEPRINT.md (file này)
   = TÀI LIỆU THAM CHIẾU - chứa MỌI chi tiết kỹ thuật
   - Cấu trúc file, logic code, application.yml, dependencies, database schema
   - Dùng để tra cứu khi code: "field nào?", "config gì?", "logic ra sao?"

📅 5_DAY_PLAN.md
   = KẾ HOẠCH THỰC HIỆN - chia theo ngày, bước cụ thể
   - Mỗi bước có checkpoint test
   - Dùng để biết "hôm nay làm gì?", "làm theo thứ tự nào?"

Cách dùng:
1. Mở 5_DAY_PLAN.md → xem bước hiện tại cần làm
2. Mở PROJECT_BLUEPRINT.md → tra cứu chi tiết kỹ thuật cho bước đó
3. Code theo → test theo checkpoint → tick ✅ → tiếp bước sau
4. Đưa cả 2 file cho AI assistant để được hỗ trợ code từng bước
```

---

## MỤC LỤC

**CORE (Phase 1-3, ưu tiên):**
1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [Tech Stack Đầy Đủ](#2-tech-stack-đầy-đủ)
3. [Luồng Dữ Liệu & Event Flow](#3-luồng-dữ-liệu--event-flow)
4. [Cấu Trúc Thư Mục Tổng Thể](#4-cấu-trúc-thư-mục-tổng-thể)
5. [Root-Level Files (pom.xml, docker-compose, init-db)](#5-root-level-files)
6. [Common Event Library (Thư viện dùng chung)](#6-common-event-library)
7. [API Gateway Service](#7-api-gateway-service) ← có đầy đủ application.yml + deps
8. [User Service](#8-user-service) ← có đầy đủ application.yml + deps
9. [Ticket Service](#9-ticket-service) ← có đầy đủ application.yml + deps
10. [Payment Service](#10-payment-service) ← có đầy đủ application.yml + deps
11. [Inventory Service](#11-inventory-service) ← có đầy đủ application.yml + deps
12. [Notification Service](#12-notification-service) ← có đầy đủ application.yml + deps
13. [Saga Orchestrator Service](#13-saga-orchestrator-service) ← có đầy đủ application.yml + deps

**INFRASTRUCTURE (Phase 4-5, làm sau):**
14. [NGINX (Edge Layer)](#14-nginx-edge-layer)
15. [Kubernetes Manifests](#15-kubernetes-manifests)
16. [Monitoring (Prometheus + Grafana)](#16-monitoring-prometheus--grafana)
17. [CI/CD Pipelines](#17-cicd-pipelines)

**REFERENCE:**
18. [Database Schema](#18-database-schema)
19. [Kafka Topics & Event Map](#19-kafka-topics--event-map)
20. [Thứ Tự Xây Dựng Đề Xuất](#20-thứ-tự-xây-dựng-đề-xuất)
21. [Ghi Chú Quan Trọng + Test Config](#ghi-chú-quan-trọng-khi-code-lại)

---

## 1. TỔNG QUAN KIẾN TRÚC

```
┌─────────────┐
│   Client     │
└──────┬───────┘
       │ HTTP/HTTPS
┌──────▼───────┐
│    NGINX      │  ← Load Balancer, SSL Termination, Rate Limiting
│  (Edge Layer) │
└──────┬───────┘
       │
┌──────▼───────────┐
│   API Gateway     │  ← Spring Cloud Gateway, Service Discovery, Circuit Breaker
│   (port 8080)     │
└──────┬───────────┘
       │ Route theo path
       ├──────────────────────┬─────────────────┬──────────────────┬──────────────────┐
       ▼                      ▼                 ▼                  ▼                  ▼
┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │ Ticket Service│  │Payment Service│  │  Inventory   │  │ Notification │
│  (port 8082) │  │  (port 8081)  │  │  (port 8083)  │  │   Service    │  │   Service    │
│              │  │  CQRS Pattern │  │               │  │  (port 8085) │  │  (port 8084) │
└──────┬───────┘  └───────┬───────┘  └───────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                  │                   │                  │                  │
       └──────────────────┴───────────────────┴──────────────────┴──────────────────┘
                                        │
                               ┌────────▼────────┐
                               │   Apache Kafka   │  ← Event Bus (Async Communication)
                               │   (port 9092)    │
                               └────────┬────────┘
                                        │
                               ┌────────▼────────┐
                               │ Saga Orchestrator│  ← Điều phối Distributed Transaction
                               │  (port 8086)     │
                               └─────────────────┘

Các hạ tầng hỗ trợ:
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  MySQL 8.0   │  │  Redis 7     │  │ Elasticsearch│  │   Consul     │  │ Prometheus + │
│  (port 3306) │  │ (port 6379)  │  │ (port 9200)  │  │  (port 8500) │  │   Grafana    │
│  5 databases │  │ Cache + Lock │  │ Search/Log   │  │  Discovery   │  │  Monitoring  │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

### Các Design Pattern được sử dụng:
| Pattern | Nơi áp dụng | Mục đích |
|---------|-------------|----------|
| **Event-Driven Architecture** | Toàn bộ hệ thống | Giao tiếp async giữa services qua Kafka |
| **Saga Pattern** | saga-orchestrator-service | Quản lý distributed transaction |
| **CQRS** | ticket-service | Tách Command và Query thành các handler riêng |
| **Event Sourcing** | common-event-library | Lưu lại lịch sử event (AggregateRoot, EventStore) |
| **Circuit Breaker** | ticket-service, api-gateway | Ngăn cascade failure |
| **API Gateway** | api-gateway | Điểm vào duy nhất, routing, load balancing |
| **Service Discovery** | Consul | Tự động tìm và đăng ký service |
| **Distributed Locking** | inventory-service (Redisson) | Đảm bảo tính nhất quán khi reserve/release seat |
| **Dead Letter Queue** | common-event-library | Xử lý event thất bại |

---

## 2. TECH STACK ĐẦY ĐỦ

### Core
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Java | 21 | Ngôn ngữ chính |
| Spring Boot | 3.2.0 | Framework chính |
| Spring Cloud | 2023.0.0 | Microservices toolkit |
| Maven | 3.9+ | Build tool, multi-module project |
| Lombok | (managed by Spring) | Giảm boilerplate code |

### Communication & Messaging
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Spring Cloud Gateway | (Spring Cloud BOM) | API Gateway (reactive, dựa trên WebFlux) |
| Apache Kafka | 7.5.0 (Confluent) | Event streaming, async messaging |
| Spring Kafka | (managed by Spring) | Kafka integration |
| WebClient (WebFlux) | (managed by Spring) | HTTP client cho inter-service sync call |

### Data Storage
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| MySQL | 8.0 | Database chính (5 databases riêng biệt) |
| Spring Data JPA + Hibernate | (managed) | ORM layer |
| Redis | 7-alpine | Caching + Distributed locking |
| Redisson | (managed) | Redis client nâng cao (distributed lock) |
| Elasticsearch | 8.11.0 | Search & analytics |

### Service Discovery & Config
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| HashiCorp Consul | latest | Service discovery + health check |

### Resilience
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Resilience4j | (Spring Cloud BOM) | Circuit breaker, retry, rate limiting |

### Monitoring
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Spring Actuator | (managed) | Health endpoints, metrics |
| Micrometer | (managed) | Metrics abstraction |
| Prometheus | latest | Thu thập metrics |
| Grafana | latest | Dashboard visualization |
| Kibana | 8.11.0 | Elasticsearch UI |

### API Documentation
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| SpringDoc OpenAPI | 2.3.0 | Tạo API docs tự động |
| Swagger UI | (bundled) | Giao diện test API |

### Infrastructure
| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Docker | - | Containerization |
| Docker Compose | - | Multi-container orchestration |
| NGINX | alpine | Reverse proxy, SSL, rate limiting |
| Kubernetes | - | Container orchestration (production) |

### CI/CD & Quality
| Công nghệ | Mục đích |
|-----------|----------|
| GitHub Actions | CI pipeline |
| CodeQL | Security scanning |
| SonarCloud | Code quality |
| JaCoCo | Test coverage |
| Codecov | Coverage reporting |
| Checkstyle (Google) | Code style |
| Trivy | Security vulnerability scan |
| Dependabot | Auto dependency update |

---

## 3. LUỒNG DỮ LIỆU & EVENT FLOW

### Flow chính: Đặt vé (Ticket Booking)

```
1. Client gửi POST /tickets
       │
2. NGINX → API Gateway → Ticket Service
       │
3. Ticket Service:
   ├── Validate input (userId, eventName, quantity, price)
   ├── Gọi Inventory Service qua WebClient (HTTP sync): POST /inventory/event/{name}/reserve
   │   └── Inventory Service: Distributed lock (Redisson) → kiểm tra & trừ available seats
   ├── Lưu Ticket vào DB (status = PENDING)
   └── Publish "ticket-created" event lên Kafka
       │
4. Kafka "ticket-created" topic → 3 consumer cùng lắng nghe:
   │
   ├── Saga Orchestrator (group: saga-orchestrator-group):
   │   ├── Tạo SagaInstance (status = STARTED)
   │   ├── Execute step: inventory-reservation → COMPLETED
   │   ├── Execute step: payment-initiation → COMPLETED
   │   ├── Execute step: notification-sending → COMPLETED
   │   └── Update saga status → IN_PROGRESS
   │
   ├── Payment Service (group: payment-service-group):
   │   ├── Tạo Payment record (status = PENDING, method = CREDIT_CARD)
   │   └── Lưu vào paymentdb
   │
   └── Notification Service (group: notification-service-group):
       ├── Tạo Notification (type = EMAIL, "Your ticket has been booked")
       └── Gửi notification (stub → return true)

5. Khi Payment được xử lý (processPayment):
   ├── Payment status → SUCCESS
   └── Publish "payment-completed" event
       │
6. Kafka "payment-completed" → 2 consumer:
   │
   ├── Ticket Service (group: ticket-service-group):
   │   └── Update ticket status: PENDING → CONFIRMED
   │
   └── Notification Service (group: notification-service-group):
       └── Gửi notification "Payment processed successfully"
```

### Flow: Huỷ vé (Cancel Ticket)

```
1. Client gửi PATCH /tickets/{id}/cancel
       │
2. Ticket Service:
   ├── Kiểm tra ticket không bị CANCELLED sẵn
   ├── Gọi Inventory Service: POST /inventory/event/{name}/release (release seats)
   ├── Update ticket status → CANCELLED
   └── Publish "ticket-cancelled" event
```

### Flow: Saga Compensation (khi có lỗi)

```
1. Saga step thất bại
       │
2. CompensationHandler được gọi:
   ├── Duyệt các step COMPLETED theo thứ tự ngược (reversed by executedAt)
   ├── Step "notification-sending" → Không cần compensation
   ├── Step "payment-initiation" → Cancel payment (stub)
   └── Step "inventory-reservation" → Release inventory (stub)
       │
3. Saga status → COMPENSATED (hoặc FAILED nếu compensation thất bại)
```

### Flow: User Registration

```
1. POST /users → User Service
   ├── Validate uniqueness (username, email)
   ├── Lưu vào userdb
   └── Publish "user-created" event
       │
2. Notification Service lắng nghe:
   └── Gửi "Welcome!" notification
```

---

## 4. CẤU TRÚC THƯ MỤC TỔNG THỂ

```
event-driven-ticketing-platform/
│
├── pom.xml                          ← Parent POM (multi-module Maven)
├── docker-compose.yml               ← Định nghĩa tất cả containers
├── init-db.sql                      ← Script tạo 5 databases
├── prometheus.yml                   ← Cấu hình scrape metrics
├── .env.example                     ← Template biến môi trường
├── sonar-project.properties         ← Cấu hình SonarCloud
├── architect.jpg                    ← Ảnh kiến trúc hệ thống
├── LICENSE                          ← MIT License
├── README.md                        ← Documentation chính
├── .gitignore                       ← Ignore rules
│
├── .github/
│   ├── dependabot.yml               ← Auto-update dependencies
│   └── workflows/
│       ├── ci.yml                   ← Build + Test + Security scan
│       ├── codeql.yml               ← CodeQL security analysis
│       └── sonarcloud.yml           ← SonarCloud quality scan
│
├── common-event-library/            ← Shared library (KHÔNG phải Spring Boot app)
├── api-gateway/                     ← Spring Cloud Gateway (WebFlux-based)
├── user-service/                    ← CRUD User + Event publishing
├── ticket-service/                  ← CQRS + Event publishing + Circuit Breaker
├── payment-service/                 ← Payment processing + Event handling
├── inventory-service/               ← Seat management + Distributed locking
├── notification-service/            ← Event consumer + Send notifications
├── saga-orchestrator-service/       ← Saga pattern orchestration
│
├── nginx/                           ← NGINX reverse proxy config
│   ├── Dockerfile
│   ├── nginx.conf
│   └── ssl/README.md
│
├── grafana/
│   └── provisioning/datasources/
│       └── prometheus.yml           ← Auto-provision Prometheus datasource
│
└── k8s/                             ← Kubernetes manifests
    ├── namespace.yaml
    ├── deployments/
    ├── services/
    ├── ingress/
    ├── nginx/
    └── autoscaling/
```

---

## 5. ROOT-LEVEL FILES

### 5.1 `pom.xml` (Parent POM)
**Mục đích:** Multi-module Maven project. Tất cả services kế thừa từ đây.

```
Parent: spring-boot-starter-parent:3.2.0
Group: com.heditra
Artifact: ticketing-system
Version: 1.0.0
Packaging: pom

Properties:
  - java.version: 21
  - spring-cloud.version: 2023.0.0
  - checkstyle.version: 3.3.1
  - jacoco.version: 0.8.11
  - sonar.version: 3.10.0.2594

Modules: (8 modules)
  - common-event-library
  - api-gateway
  - ticket-service
  - user-service
  - payment-service
  - notification-service
  - inventory-service
  - saga-orchestrator-service

Plugins:
  - spring-boot-maven-plugin
  - maven-checkstyle-plugin (google_checks.xml)
  - jacoco-maven-plugin
  - sonar-maven-plugin
```

### 5.2 `docker-compose.yml`
**Mục đích:** Khởi động TOÀN BỘ hệ thống bằng 1 lệnh.

**Services (15 containers):**

| Container | Image | Port | Depends On | Health Check |
|-----------|-------|------|-----------|-------------|
| consul | hashicorp/consul:latest | 8500 | - | consul info |
| zookeeper | confluentinc/cp-zookeeper:7.5.0 | 2181 | - | nc -z localhost 2181 |
| kafka | confluentinc/cp-kafka:7.5.0 | 9092, 29092 | zookeeper | kafka-broker-api-versions |
| mysql | mysql:8.0 | 3306 | - | mysqladmin ping |
| redis | redis:7-alpine | 6379 | - | redis-cli ping |
| elasticsearch | elasticsearch:8.11.0 | 9200 | - | curl cluster/health |
| kibana | kibana:8.11.0 | 5601 | elasticsearch | - |
| prometheus | prom/prometheus:latest | 9090 | - | - |
| grafana | grafana/grafana:latest | 3000 | prometheus | - |
| api-gateway | ./api-gateway/Dockerfile | 8080 | consul, redis, all services | - |
| nginx | ./nginx/Dockerfile | 80, 443 | api-gateway | - |
| ticket-service | ./ticket-service/Dockerfile | 8081 | consul, mysql, kafka, redis, elasticsearch | - |
| user-service | ./user-service/Dockerfile | 8082 | consul, mysql, kafka, redis, elasticsearch | - |
| payment-service | ./payment-service/Dockerfile | 8083 | consul, mysql, kafka, redis | - |
| notification-service | ./notification-service/Dockerfile | 8084 | consul, mysql, kafka, redis | - |
| inventory-service | ./inventory-service/Dockerfile | 8085 | consul, mysql, redis | - |
| saga-orchestrator | ./saga-orchestrator-service/Dockerfile | 8086 | consul, mysql, kafka | - |

**Network:** ticketing-network (bridge)
**Volumes:** mysql-data, redis-data, elasticsearch-data, prometheus-data, grafana-data

**Lưu ý quan trọng:** Các service application (ticket, user, ...) dùng `context: .` (root) vì Dockerfile cần copy `common-event-library` trước khi build.

**docker-compose.yml CHO GIAI ĐOẠN DEVELOPMENT (chỉ infrastructure):**
> Khi phát triển core services (Phase 1-3), chỉ cần chạy infrastructure containers.
> Các microservice sẽ chạy trực tiếp từ IDE hoặc `mvn spring-boot:run`.

```yaml
services:
  consul:
    image: hashicorp/consul:latest
    container_name: consul
    ports:
      - "8500:8500"
    command: agent -dev -ui -client=0.0.0.0
    networks:
      - ticketing-network
    healthcheck:
      test: ["CMD", "consul", "info"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - ticketing-network
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      zookeeper:
        condition: service_healthy
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_LOG_RETENTION_HOURS: 168
    networks:
      - ticketing-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "kafka:9092"]
      interval: 10s
      timeout: 10s
      retries: 10
      start_period: 40s

  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    networks:
      - ticketing-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-prootpassword"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - ticketing-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes

networks:
  ticketing-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
```

> **QUAN TRỌNG khi chạy local:** Trong application.yml, thay hostname Docker sang localhost:
> - `mysql:3306` → `localhost:3306`
> - `redis:6379` → `localhost:6379`
> - `kafka:9092` → `localhost:29092` (dùng PLAINTEXT_HOST listener)
> - `consul:8500` → `localhost:8500`

### 5.3 `init-db.sql`
```sql
CREATE DATABASE IF NOT EXISTS ticketdb;
CREATE DATABASE IF NOT EXISTS userdb;
CREATE DATABASE IF NOT EXISTS paymentdb;
CREATE DATABASE IF NOT EXISTS notificationdb;
CREATE DATABASE IF NOT EXISTS inventorydb;
```
Được mount vào `/docker-entrypoint-initdb.d/` của MySQL container → chạy tự động khi khởi tạo.

### 5.4 `.env.example`
Template cho các biến môi trường:
- MySQL: root password, host, port, user, password
- Redis: host, port, password
- Kafka: bootstrap-servers, zookeeper
- Consul: host, port
- Elasticsearch: uris, username, password
- JWT: secret (min 512 bits), expiration (86400000ms = 24h)
- Ports: 8080-8086
- Prometheus, Grafana, Spring profiles

---

## 6. COMMON EVENT LIBRARY

**Mục đích:** Thư viện dùng chung cho TẤT CẢ microservices. Chứa định nghĩa events, event infrastructure, và event sourcing base classes.

**KHÔNG PHẢI Spring Boot app** - chỉ là library JAR, được install vào local Maven repo.

### Cấu trúc:
```
common-event-library/
├── pom.xml
└── src/main/java/com/heditra/events/
    ├── core/                        ← Base interfaces & classes
    │   ├── DomainEvent.java         ← Abstract base class cho mọi event
    │   ├── EventEnvelope.java       ← Wrapper: metadata + payload
    │   ├── EventHandler.java        ← Interface để xử lý event
    │   ├── EventMetadata.java       ← Metadata: eventId, source, correlationId
    │   ├── EventProcessingException.java  ← Custom exception
    │   └── EventPublisher.java      ← Interface publish event
    │
    ├── infrastructure/              ← Kafka implementation
    │   ├── EventKafkaConfig.java    ← Cấu hình Kafka Producer
    │   ├── KafkaEventPublisher.java ← Implement EventPublisher bằng KafkaTemplate
    │   ├── KafkaEventListener.java  ← Dispatch event đến handler phù hợp
    │   └── DeadLetterQueueHandler.java  ← Xử lý event thất bại
    │
    ├── sourcing/                    ← Event Sourcing base classes
    │   ├── AggregateRoot.java       ← Base class cho aggregate
    │   ├── EventStore.java          ← Interface event store
    │   ├── KafkaEventStore.java     ← Implement event store bằng Kafka
    │   ├── EventReplayer.java       ← Replay events
    │   └── EventSourcingRepository.java  ← Save/load aggregate
    │
    ├── ticket/                      ← Ticket domain events
    │   ├── TicketCreatedEvent.java
    │   ├── TicketConfirmedEvent.java
    │   └── TicketCancelledEvent.java
    │
    ├── payment/                     ← Payment domain events
    │   ├── PaymentInitiatedEvent.java
    │   ├── PaymentCompletedEvent.java
    │   ├── PaymentFailedEvent.java
    │   └── PaymentRefundedEvent.java
    │
    ├── user/                        ← User domain events
    │   ├── UserCreatedEvent.java
    │   ├── UserUpdatedEvent.java
    │   └── UserDeletedEvent.java
    │
    ├── inventory/                   ← Inventory domain events
    │   ├── InventoryReservedEvent.java
    │   └── InventoryReleasedEvent.java
    │
    └── notification/                ← Notification domain events
        └── NotificationSentEvent.java
```

### Chi tiết từng class:

#### 6.1 `DomainEvent` (abstract)
```
Fields:
  - eventId: String (UUID)
  - eventType: String ("TicketCreated", "PaymentCompleted", ...)
  - occurredAt: LocalDateTime (tự set = now())
  - aggregateId: String (ID của entity liên quan)
  - version: Integer (phiên bản event)

Annotations:
  - @JsonTypeInfo(use = Id.CLASS) ← để deserialize đúng class khi nhận từ Kafka
  - Implements Serializable
  - @Data, @NoArgsConstructor, @AllArgsConstructor (Lombok)
```

#### 6.2 `EventPublisher` (interface)
```java
<T extends DomainEvent> CompletableFuture<Void> publish(T event);          // Tự suy topic
<T extends DomainEvent> CompletableFuture<Void> publish(String topic, T event);  // Chỉ định topic
```

#### 6.3 `KafkaEventPublisher` (implementation)
```
- Validate: event != null, aggregateId != null
- Dùng kafkaTemplate.send(topic, event.getAggregateId(), event)
- Key = aggregateId (đảm bảo ordering cho cùng 1 aggregate)
- deriveTopicFromEvent: "TicketCreated" → "ticket-created" (regex camelCase to kebab-case)
- Trả về CompletableFuture<Void>
```

#### 6.4 `EventKafkaConfig`
```
Producer config:
  - bootstrap-servers từ application.yml (default: kafka:9092)
  - Key serializer: StringSerializer
  - Value serializer: JsonSerializer
  - ACKS_CONFIG: "all" (tất cả replica phải xác nhận)
  - RETRIES: 3
  - ENABLE_IDEMPOTENCE: true (tránh duplicate)
  - ADD_TYPE_INFO_HEADERS: false

ObjectMapper:
  - Register JavaTimeModule (để serialize LocalDateTime)
  - Disable WRITE_DATES_AS_TIMESTAMPS
```

#### 6.5 `KafkaEventListener`
```
- Nhận List<EventHandler> qua constructor → build Map<Class, Handler>
- processEvent(event): tìm handler theo event.getClass() → gọi handle()
- Nếu không tìm thấy handler → log warning
```

#### 6.6 `DeadLetterQueueHandler`
```
- Lắng nghe topic: "event-dlq" (configurable)
- Log error message + acknowledge
- Trong production nên lưu vào DB hoặc gửi alert
```

#### 6.7 Event Sourcing Classes
```
AggregateRoot:
  - aggregateId, version
  - uncommittedEvents: List<DomainEvent>
  - applyEvent(): handleEvent() + add to uncommitted + increment version
  - loadFromHistory(): replay events
  
EventStore (interface):
  - save(event), saveAll(events)
  - getEvents(aggregateId), getEvents(aggregateId, fromVersion)
  - getEventsByType(eventType)

KafkaEventStore:
  - Publish event vào topic "event-store"
  - In-memory cache (ConcurrentHashMap) - demo purpose
  - Trong production: dùng database thật (EventStoreDB, PostgreSQL, ...)

EventSourcingRepository<T extends AggregateRoot>:
  - save(): lấy uncommitted events → eventStore.saveAll() → clearUncommitted
  - load(): tạo instance bằng reflection → loadFromHistory
```

#### 6.8 Domain Events Chi Tiết

| Event Class | eventType | Fields đặc thù |
|-------------|-----------|----------------|
| **TicketCreatedEvent** | "TicketCreated" | ticketId, userId, eventName, quantity, pricePerTicket, totalAmount |
| **TicketConfirmedEvent** | "TicketConfirmed" | ticketId, userId, eventName |
| **TicketCancelledEvent** | "TicketCancelled" | ticketId, userId, eventName, quantity, cancellationReason |
| **PaymentInitiatedEvent** | "PaymentInitiated" | paymentId, ticketId, userId, amount (BigDecimal), paymentMethod, transactionId |
| **PaymentCompletedEvent** | "PaymentCompleted" | paymentId, ticketId, userId, amount, transactionId |
| **PaymentFailedEvent** | "PaymentFailed" | paymentId, ticketId, userId, amount, transactionId, failureReason |
| **PaymentRefundedEvent** | "PaymentRefunded" | paymentId, ticketId, userId, amount, transactionId, refundReason |
| **UserCreatedEvent** | "UserCreated" | userId, username, email, role |
| **UserUpdatedEvent** | "UserUpdated" | userId, username, email, role |
| **UserDeletedEvent** | "UserDeleted" | userId |
| **InventoryReservedEvent** | "InventoryReserved" | eventName, quantity, ticketId, reservationId |
| **InventoryReleasedEvent** | "InventoryReleased" | eventName, quantity, ticketId, reservationId, releaseReason |
| **NotificationSentEvent** | "NotificationSent" | notificationId, userId, notificationType, recipient, subject, message, successful |

### 6.9 pom.xml (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: common-event-library | Version: 1.0.0
Java: 21

Dependencies:
  1.  spring-kafka
  2.  spring-boot-starter
  3.  jackson-databind
  4.  jackson-datatype-jsr310              ← Serialize LocalDateTime
  5.  lombok (optional)
  6.  slf4j-api
  7.  jakarta.validation-api

Build Plugin:
  - maven-compiler-plugin (source/target: 21)
    + annotationProcessorPaths: lombok

QUAN TRỌNG:
  - KHÔNG có spring-boot-maven-plugin (vì đây là library, không phải executable JAR)
  - KHÔNG có spring-cloud-dependencies
  - Build bằng: mvn clean install (install vào local Maven repo ~/.m2)
```

---

## 7. API GATEWAY SERVICE

**Port:** 8080
**Mục đích:** Điểm vào duy nhất của hệ thống. Route request đến đúng service.

### Cấu trúc:
```
api-gateway/
├── Dockerfile
├── pom.xml
└── src/main/
    ├── java/com/heditra/apigateway/
    │   ├── ApiGatewayApplication.java      ← @SpringBootApplication + @EnableDiscoveryClient
    │   ├── config/
    │   │   └── GatewayConfig.java          ← Route config + CORS
    │   └── exception/
    │       ├── GlobalErrorAttributes.java   ← Custom error response
    │       └── ApiErrorResponse.java        ← Error DTO
    └── resources/
        └── application.yml
```

### Chi tiết:

#### 7.1 Routing (2 cách cấu hình, cả 2 đều có)

**Cách 1 - application.yml:**
```yaml
spring.cloud.gateway.routes:
  - id: ticket-service
    uri: lb://ticket-service          ← "lb://" = load balanced qua Consul
    predicates:
      - Path=/tickets/**
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/users/**
  - id: payment-service → /payments/**
  - id: notification-service → /notifications/**
  - id: inventory-service → /inventory/**
```

**Cách 2 - GatewayConfig.java (programmatic):**
```java
builder.routes()
  .route("user-service", r -> r
    .path("/users/**")
    .filters(f -> f
      .stripPrefix(0)                            // Không strip path prefix
      .addRequestHeader("X-Gateway-Request", "API-Gateway"))  // Thêm header
    .uri("lb://user-service"))
  // ... tương tự cho ticket, payment, notification, inventory
```

#### 7.2 CORS Configuration
```java
- AllowedOrigins: "*"
- AllowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- AllowedHeaders: "*"
- AllowCredentials: false
- MaxAge: 3600s (1 giờ)
```

#### 7.3 Error Handling
```
GlobalErrorAttributes extends DefaultErrorAttributes:
  - Thêm success: false
  - Thêm errorCode
  - Thêm timestamp
```

#### 7.4 Dependencies Đặc Biệt
```
- spring-cloud-starter-gateway (REACTIVE, WebFlux-based, KHÔNG phải MVC)
- spring-cloud-starter-consul-discovery
- spring-boot-starter-data-redis-reactive
- spring-cloud-starter-circuitbreaker-reactor-resilience4j
- resilience4j-spring-boot3
- springdoc-openapi-starter-webflux-ui (WebFlux version của Swagger)
- micrometer-registry-prometheus
```

#### 7.5 Dockerfile
```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
  # Copy pom.xml → dependency:go-offline → Copy src → package
FROM eclipse-temurin:21-jre-alpine
  # Copy jar → EXPOSE 8080 → java -jar
```
**Lưu ý:** API Gateway build STANDALONE (không cần common-event-library).

#### 7.6 application.yml (Đầy đủ)
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  data:
    redis:
      host: redis
      port: 6379
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
    gateway:
      discovery:
        locator:
          enabled: true
      routes:
        - id: ticket-service
          uri: lb://ticket-service
          predicates:
            - Path=/tickets/**
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/users/**
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/payments/**
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/notifications/**
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/inventory/**

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

#### 7.7 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: api-gateway | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  spring-cloud-starter-gateway              ← REACTIVE (WebFlux), KHÔNG phải MVC
  2.  spring-cloud-starter-consul-discovery
  3.  spring-boot-starter-actuator
  4.  spring-boot-starter-data-redis-reactive   ← Reactive Redis
  5.  spring-cloud-starter-circuitbreaker-reactor-resilience4j
  6.  resilience4j-spring-boot3
  7.  springdoc-openapi-starter-webflux-ui:2.3.0  ← WebFlux version!
  8.  micrometer-registry-prometheus
  9.  lombok (optional)
  10. spring-boot-starter-test (test)

KHÔNG CÓ: common-event-library, JPA, MySQL, Kafka, Validation
(Gateway chỉ route request, không xử lý business logic)

DependencyManagement: spring-cloud-dependencies:2023.0.0
Build Plugins: (giống các service khác)
```

---

## 8. USER SERVICE

**Port:** 8082 | **Database:** userdb | **Kafka Group:** user-service-group

### Cấu trúc:
```
user-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/userservice/
    ├── UserServiceApplication.java          ← @SpringBootApplication + @EnableDiscoveryClient
    │
    ├── controller/
    │   └── UserController.java              ← REST API endpoints
    │
    ├── service/
    │   ├── UserService.java                 ← Interface
    │   └── impl/
    │       └── UserServiceImpl.java         ← Implementation + Event publishing
    │
    ├── repository/
    │   └── UserRepository.java              ← JpaRepository
    │
    ├── model/
    │   ├── User.java                        ← JPA Entity
    │   └── UserRole.java                    ← Enum: ADMIN, USER, MANAGER
    │
    ├── dto/
    │   ├── request/
    │   │   ├── CreateUserRequest.java       ← Validation annotations
    │   │   └── UpdateUserRequest.java
    │   ├── response/
    │   │   ├── UserResponse.java
    │   │   └── ApiResponse.java             ← Generic wrapper
    │   └── common/
    │       └── ApiErrorResponse.java
    │
    ├── mapper/
    │   └── UserMapper.java                  ← Manual mapping (không dùng MapStruct)
    │
    ├── config/
    │   ├── RedisConfig.java                 ← Cache + RedisTemplate config
    │   ├── KafkaConfig.java                 ← Producer config
    │   ├── EventPublisherConfig.java        ← Bean EventPublisher
    │   ├── OpenApiConfig.java               ← Swagger config
    │   └── JpaAuditingConfig.java           ← @EnableJpaAuditing
    │
    └── exception/
        ├── BusinessException.java           ← Base business exception
        ├── TechnicalException.java          ← Base technical exception
        ├── ValidationException.java         ← Validation errors
        ├── UserNotFoundException.java       ← extends BusinessException
        ├── UserAlreadyExistsException.java  ← extends BusinessException
        ├── InvalidUserDataException.java    ← extends ValidationException
        └── handler/
            └── GlobalExceptionHandler.java  ← @RestControllerAdvice
```

### 8.1 User Entity
```
Table: users
Fields:
  - id: Long (auto-generated, IDENTITY)
  - username: String (unique, not null, max 50)
  - email: String (unique, not null)
  - password: String (not null) ← LƯU Ý: lưu plain text, chưa có BCrypt
  - firstName: String (not null, max 50)
  - lastName: String (not null, max 50)
  - role: UserRole enum (ADMIN, USER, MANAGER) ← EnumType.STRING
  - createdAt: LocalDateTime (@CreatedDate, auto)
  - updatedAt: LocalDateTime (@LastModifiedDate, auto)

Cần @EnableJpaAuditing để @CreatedDate/@LastModifiedDate hoạt động.
```

### 8.2 API Endpoints
| Method | Path | Mô tả | Request Body | Response |
|--------|------|--------|-------------|----------|
| POST | /users | Tạo user mới | CreateUserRequest | 201 + ApiResponse\<UserResponse\> |
| GET | /users/{id} | Lấy user theo ID | - | ApiResponse\<UserResponse\> |
| GET | /users/username/{username} | Lấy theo username | - | ApiResponse\<UserResponse\> |
| GET | /users/email/{email} | Lấy theo email | - | ApiResponse\<UserResponse\> |
| GET | /users | Lấy tất cả users | - | ApiResponse\<List\<UserResponse\>\> |
| GET | /users/role/{role} | Lấy theo role | - | ApiResponse\<List\<UserResponse\>\> |
| PUT | /users/{id} | Cập nhật user | UpdateUserRequest | ApiResponse\<UserResponse\> |
| DELETE | /users/{id} | Xoá user | - | 204 No Content |

### 8.3 Service Logic Chi Tiết

**createUser:**
1. Validate uniqueness: kiểm tra username & email chưa tồn tại
2. Map request → entity bằng UserMapper
3. Save vào DB
4. Publish `UserCreatedEvent` lên topic "user-created"
5. Return UserResponse

**updateUser:**
1. Find by ID (throw UserNotFoundException nếu không có)
2. Update fields từ request
3. Save
4. Publish `UserUpdatedEvent` lên topic "user-updated"
5. Cache: @CachePut(value = "users", key = "#id")

**deleteUser:**
1. Find by ID
2. Delete
3. Publish `UserDeletedEvent` lên topic "user-deleted"
4. Cache: @CacheEvict(value = "users", allEntries = true)

### 8.4 Caching Strategy
```
- getUserById: @Cacheable(value = "users", key = "#id")
- getUserByUsername: @Cacheable(value = "users", key = "'username:' + #username")
- getUserByEmail: @Cacheable(value = "users", key = "'email:' + #email")
- updateUser: @CachePut(value = "users", key = "#id")
- deleteUser: @CacheEvict(value = "users", allEntries = true)

Redis config:
  - TTL: 10 phút
  - Key serializer: StringRedisSerializer
  - Value serializer: GenericJackson2JsonRedisSerializer
  - disableCachingNullValues
```

### 8.5 Validation (CreateUserRequest)
```
- username: @NotBlank, @Size(3-50)
- email: @NotBlank, @Email
- password: @NotBlank, @Size(min=6)
- firstName: @NotBlank, @Size(max=50)
- lastName: @NotBlank, @Size(max=50)
- role: @NotNull (UserRole enum)
```

### 8.6 Exception Handling (Pattern dùng chung cho nhiều services)
```
GlobalExceptionHandler (@RestControllerAdvice):
  - BusinessException → 400 BAD_REQUEST
  - ValidationException → 400 BAD_REQUEST
  - MethodArgumentNotValidException → 400 (collect field errors)
  - TechnicalException → 500 INTERNAL_SERVER_ERROR
  - Exception (catch-all) → 500
  
Response format: ApiErrorResponse {success, errorCode, message, timestamp, path, details}
```

### 8.7 Dockerfile Pattern (dùng chung cho tất cả services trừ API Gateway)
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Bước 1: Build common-event-library trước
COPY common-event-library /common-event-library
WORKDIR /common-event-library
RUN mvn clean install -DskipTests

# Bước 2: Build service
WORKDIR /build
COPY user-service/pom.xml .
COPY user-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /build/target/*.jar user-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "user-service.jar"]
```

### 8.8 application.yml (Đầy đủ)
```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://mysql:3306/userdb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
    elasticsearch:
      repositories:
        enabled: true
  elasticsearch:
    uris: http://elasticsearch:9200
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: user-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

logging:
  level:
    com.heditra: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 8.9 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: user-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-kafka
  6.  spring-cloud-starter-consul-discovery
  7.  spring-boot-starter-actuator
  8.  spring-boot-starter-validation
  9.  spring-boot-starter-data-redis
  10. spring-boot-starter-cache
  11. redisson-spring-boot-starter:3.25.0
  12. spring-boot-starter-data-elasticsearch
  13. spring-cloud-starter-circuitbreaker-resilience4j
  14. resilience4j-spring-boot3
  15. springdoc-openapi-starter-webmvc-ui:2.3.0
  16. micrometer-registry-prometheus
  17. lombok (optional)
  18. spring-boot-starter-test (test)
  19. spring-kafka-test (test)
  20. h2 (test)

DependencyManagement: spring-cloud-dependencies:2023.0.0
Build Plugins: (giống ticket-service)
```

---

## 9. TICKET SERVICE

**Port:** 8081 | **Database:** ticketdb | **Kafka Group:** ticket-service-group
**ĐẶC BIỆT:** Áp dụng CQRS Pattern + Circuit Breaker + WebClient (inter-service call)

### Cấu trúc:
```
ticket-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/ticketservice/
    ├── TicketServiceApplication.java
    │
    ├── controller/
    │   └── TicketController.java
    │
    ├── service/
    │   ├── TicketService.java               ← Interface
    │   └── impl/
    │       └── TicketServiceImpl.java       ← Circuit Breaker + WebClient calls
    │
    ├── repository/
    │   └── TicketRepository.java
    │
    ├── model/
    │   ├── Ticket.java                      ← @Version (optimistic locking)
    │   └── TicketStatus.java                ← PENDING, CONFIRMED, CANCELLED, COMPLETED
    │
    ├── cqrs/                                ← CQRS Pattern Implementation
    │   ├── commands/
    │   │   ├── CreateTicketCommand.java
    │   │   ├── CancelTicketCommand.java
    │   │   └── UpdateTicketStatusCommand.java
    │   ├── commands/handlers/
    │   │   ├── CreateTicketCommandHandler.java
    │   │   ├── CancelTicketCommandHandler.java
    │   │   └── UpdateTicketStatusCommandHandler.java
    │   ├── queries/
    │   │   ├── GetTicketQuery.java
    │   │   ├── GetTicketsByUserQuery.java
    │   │   └── GetTicketsByStatusQuery.java
    │   ├── queries/handlers/
    │   │   ├── GetTicketQueryHandler.java
    │   │   ├── GetTicketsByUserQueryHandler.java
    │   │   └── GetTicketsByStatusQueryHandler.java
    │   └── common/
    │       ├── CommandResult.java
    │       └── QueryResult.java
    │
    ├── events/handlers/
    │   ├── PaymentCompletedEventHandler.java   ← Lắng nghe "payment-completed"
    │   └── PaymentFailedEventHandler.java      ← Lắng nghe "payment-failed"
    │
    ├── config/
    │   ├── KafkaConfig.java
    │   ├── RedisConfig.java
    │   ├── EventPublisherConfig.java
    │   ├── OpenApiConfig.java
    │   └── RestClientConfig.java               ← WebClient bean cho HTTP calls
    │
    ├── dto/common/
    │   └── ApiErrorResponse.java
    │
    └── exception/
        ├── BusinessException.java
        ├── TechnicalException.java
        ├── ValidationException.java
        ├── TicketNotFoundException.java
        ├── TicketAlreadyCancelledException.java
        ├── InvalidTicketStatusException.java
        └── handler/
            └── GlobalExceptionHandler.java
```

### 9.1 Ticket Entity
```
Table: tickets
Fields:
  - id: Long (IDENTITY)
  - userId: Long (not null)
  - eventName: String (not null, max 200)
  - quantity: Integer (not null, min 1)
  - totalPrice: BigDecimal (not null, precision 10, scale 2)
  - pricePerTicket: BigDecimal (not null, precision 10, scale 2)
  - totalAmount: BigDecimal (not null, precision 10, scale 2)
  - status: TicketStatus enum (STRING)
  - bookingDate: LocalDateTime (@CreationTimestamp)
  - createdAt: LocalDateTime (@CreationTimestamp)
  - updatedAt: LocalDateTime (@UpdateTimestamp)
  - eventDate: LocalDateTime (not null, @Future)
  - version: Long (@Version ← optimistic locking)
```

### 9.2 API Endpoints
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /tickets | Tạo ticket mới |
| GET | /tickets/{id} | Lấy ticket theo ID |
| GET | /tickets | Lấy tất cả tickets |
| GET | /tickets/user/{userId} | Lấy tickets theo user |
| GET | /tickets/status/{status} | Lấy tickets theo status |
| GET | /tickets/event/{eventName} | Lấy tickets theo event |
| PATCH | /tickets/{id}/status?status=X | Cập nhật status |
| PATCH | /tickets/{id}/cancel | Huỷ ticket |
| DELETE | /tickets/{id} | Xoá ticket |

**Lưu ý:** Controller nhận/trả trực tiếp Ticket entity (không dùng DTO riêng).

### 9.3 Service Logic Chi Tiết

**createTicket:**
1. Validate: ticket != null, userId/eventName/quantity/pricePerTicket != null
2. Validate: quantity > 0, pricePerTicket > 0
3. Set status = PENDING
4. Tính totalAmount = pricePerTicket * quantity
5. **Gọi Inventory Service (HTTP):** `POST /inventory/event/{eventName}/reserve?quantity={quantity}`
   - Dùng WebClient (reactive HTTP client)
   - Có `@CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveSeatsFallback")`
   - Có `@Retry(name = "inventory-service")`
   - Fallback: return false → throw BusinessException
6. Save ticket
7. Publish `TicketCreatedEvent` lên "ticket-created"

**cancelTicket:**
1. Kiểm tra không phải CANCELLED
2. **Gọi Inventory Service (HTTP):** `POST /inventory/event/{eventName}/release?quantity={quantity}`
3. Set status = CANCELLED
4. Publish `TicketCancelledEvent` lên "ticket-cancelled"

**updateTicketStatus:**
1. Không cho phép thay đổi status của ticket đã CANCELLED
2. Save
3. Nếu status mới = CONFIRMED → Publish `TicketConfirmedEvent`

### 9.4 Event Handlers (Consumer)

**PaymentCompletedEventHandler:**
```
Lắng nghe: topic "payment-completed", group "ticket-service-group"
Logic: ticketService.updateTicketStatus(event.getTicketId(), CONFIRMED)
```

**PaymentFailedEventHandler:**
```
Lắng nghe: topic "payment-failed", group "ticket-service-group"
Logic: Xử lý khi payment thất bại (có thể cancel ticket)
```

### 9.5 CQRS Pattern

**Commands (Write operations):**
```
CreateTicketCommand { userId, eventName, quantity, pricePerTicket, eventDate }
CancelTicketCommand { ticketId, cancellationReason }
UpdateTicketStatusCommand { ticketId, newStatus }

Handlers:
  - CreateTicketCommandHandler → return CommandResult<Long> (ticket ID)
  - CancelTicketCommandHandler → return CommandResult<Void>
  - UpdateTicketStatusCommandHandler → return CommandResult<Void>

CommandResult<T> { success: boolean, data: T, errorMessage: String }
```

**Queries (Read operations):**
```
GetTicketQuery { ticketId }
GetTicketsByUserQuery { userId }
GetTicketsByStatusQuery { status }

Handlers:
  - GetTicketQueryHandler → return QueryResult<Ticket>
  - GetTicketsByUserQueryHandler → return QueryResult<List<Ticket>>
  - GetTicketsByStatusQueryHandler → return QueryResult<List<Ticket>>

QueryResult<T> { success: boolean, data: T, errorMessage: String }
```

### 9.6 RestClientConfig (WebClient)
```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .baseUrl("http://inventory-service")  // Resolved by Consul
        .build();
}
```

### 9.7 Dependencies Đặc Biệt
```
- spring-boot-starter-webflux (cho WebClient)
- spring-cloud-starter-circuitbreaker-reactor-resilience4j
- common-event-library (dependency)
```

### 9.8 application.yml (Đầy đủ)
```yaml
server:
  port: 8081

spring:
  application:
    name: ticket-service
  datasource:
    url: jdbc:mysql://mysql:3306/ticketdb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ticket-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
    elasticsearch:
      repositories:
        enabled: true
  elasticsearch:
    uris: http://elasticsearch:9200

resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
  retry:
    instances:
      inventory-service:
        max-attempts: 3
        wait-duration: 1000

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### 9.9 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: ticket-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-kafka
  6.  spring-cloud-starter-consul-discovery
  7.  spring-boot-starter-actuator
  8.  spring-boot-starter-validation
  9.  spring-boot-starter-webflux              ← Cho WebClient
  10. spring-boot-starter-data-redis
  11. spring-boot-starter-cache
  12. redisson-spring-boot-starter:3.25.0
  13. spring-boot-starter-data-elasticsearch
  14. spring-cloud-starter-circuitbreaker-resilience4j
  15. resilience4j-spring-boot3
  16. springdoc-openapi-starter-webmvc-ui:2.3.0
  17. micrometer-registry-prometheus
  18. lombok (optional)
  19. spring-boot-starter-test (test)
  20. spring-kafka-test (test)
  21. h2 (test)

DependencyManagement: spring-cloud-dependencies:2023.0.0

Build Plugins:
  - spring-boot-maven-plugin (exclude lombok)
  - maven-checkstyle-plugin:3.3.1 (google_checks.xml)
  - jacoco-maven-plugin:0.8.11
  - sonar-maven-plugin:3.10.0.2594
```

---

## 10. PAYMENT SERVICE

**Port:** 8083 | **Database:** paymentdb | **Kafka Group:** payment-service-group

### Cấu trúc:
```
payment-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/paymentservice/
    ├── PaymentServiceApplication.java
    ├── controller/PaymentController.java
    ├── service/
    │   ├── PaymentService.java
    │   └── impl/PaymentServiceImpl.java
    ├── repository/PaymentRepository.java
    ├── model/
    │   ├── Payment.java
    │   ├── PaymentStatus.java     ← PENDING, SUCCESS, FAILED, REFUNDED
    │   └── PaymentMethod.java     ← CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, DIGITAL_WALLET
    ├── dto/
    │   ├── request/CreatePaymentRequest.java
    │   └── response/
    │       ├── PaymentResponse.java
    │       └── ApiResponse.java
    ├── mapper/PaymentMapper.java
    ├── events/handlers/
    │   └── TicketCreatedEventHandler.java    ← Consumer "ticket-created"
    ├── config/
    │   ├── KafkaConfig.java                  ← CẢ Producer + Consumer config
    │   ├── EventPublisherConfig.java
    │   ├── JpaAuditingConfig.java
    │   └── OpenApiConfig.java
    └── exception/ (giống pattern user-service)
```

### 10.1 Payment Entity
```
Table: payments
Fields:
  - id: Long (IDENTITY)
  - ticketId: Long (not null)
  - userId: Long (not null)
  - amount: BigDecimal (not null, precision 10, scale 2)
  - paymentMethod: PaymentMethod enum (not null, max 20)
  - status: PaymentStatus enum (not null, max 20)
  - transactionId: String (unique, max 100)
  - createdAt: LocalDateTime (@CreatedDate)
```

### 10.2 API Endpoints
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /payments | Tạo payment mới |
| GET | /payments/{id} | Lấy payment theo ID |
| GET | /payments/ticket/{ticketId} | Lấy payment theo ticket |
| GET | /payments/transaction/{transactionId} | Lấy theo transaction ID |
| GET | /payments | Lấy tất cả |
| GET | /payments/user/{userId} | Lấy theo user |
| GET | /payments/status/{status} | Lấy theo status |
| POST | /payments/{id}/process | Xử lý thanh toán |
| POST | /payments/{id}/refund | Hoàn tiền |
| DELETE | /payments/{id} | Xoá |

### 10.3 Service Logic Chi Tiết

**createPayment:**
1. Validate request (null check, amount > 0)
2. Map → Payment entity, set status = PENDING
3. Generate transactionId = UUID
4. Save
5. Publish `PaymentInitiatedEvent`

**processPayment:**
1. Find payment, check status == PENDING
2. executePaymentGateway() → **STUB: always return true** (mock)
3. Nếu success: status → SUCCESS, publish `PaymentCompletedEvent`
4. Nếu fail: status → FAILED, publish `PaymentFailedEvent`

**refundPayment:**
1. Check status == SUCCESS
2. Set status → REFUNDED
3. Publish `PaymentRefundedEvent`

### 10.4 Event Handler (Consumer)

**TicketCreatedEventHandler:**
```
Lắng nghe: topic "ticket-created", group "payment-service-group"
Implements: EventHandler<TicketCreatedEvent>
Logic:
  1. Validate event (ticketId, userId, totalAmount != null)
  2. Tạo Payment record: ticketId, userId, amount = totalAmount
  3. Set paymentMethod = CREDIT_CARD (mặc định)
  4. Set status = PENDING
  5. Save vào DB
```

### 10.5 KafkaConfig (CẢ Producer + Consumer)
```java
Producer: StringSerializer + JsonSerializer
Consumer:
  - group-id: payment-service-group
  - Key: StringDeserializer
  - Value: JsonDeserializer
  - trusted.packages: * (cho phép deserialize tất cả class)
  - spring.json.type.mapping: TicketCreatedEvent → com.heditra.events.ticket.TicketCreatedEvent
```

### 10.6 application.yml (Đầy đủ)
```yaml
server:
  port: 8083

spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:mysql://mysql:3306/paymentdb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: payment-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

logging:
  level:
    com.heditra: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 10.7 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: payment-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-kafka
  6.  spring-cloud-starter-consul-discovery
  7.  spring-boot-starter-actuator
  8.  spring-boot-starter-validation
  9.  spring-boot-starter-data-redis
  10. spring-boot-starter-cache
  11. redisson-spring-boot-starter:3.25.0
  12. spring-cloud-starter-circuitbreaker-resilience4j
  13. resilience4j-spring-boot3
  14. springdoc-openapi-starter-webmvc-ui:2.3.0
  15. micrometer-registry-prometheus
  16. lombok (optional)
  17. spring-boot-starter-test (test)
  18. spring-kafka-test (test)
  19. h2 (test)

DependencyManagement: spring-cloud-dependencies:2023.0.0

Build Plugins: (giống ticket-service)
  - spring-boot-maven-plugin (exclude lombok)
  - maven-checkstyle-plugin:3.3.1
  - jacoco-maven-plugin:0.8.11
  - sonar-maven-plugin:3.10.0.2594
```

---

## 11. INVENTORY SERVICE

**Port:** 8085 | **Database:** inventorydb
**ĐẶC BIỆT:** Distributed Locking với Redisson, KHÔNG dùng Kafka (chỉ nhận HTTP calls)

### Cấu trúc:
```
inventory-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/inventoryservice/
    ├── InventoryServiceApplication.java
    ├── controller/InventoryController.java
    ├── service/
    │   ├── InventoryService.java
    │   └── impl/InventoryServiceImpl.java     ← Redisson distributed locking
    ├── repository/InventoryRepository.java
    ├── model/Inventory.java
    ├── config/
    │   ├── RedisConfig.java
    │   └── RedissonConfig.java                ← Cấu hình Redisson client
    ├── dto/common/ApiErrorResponse.java
    └── exception/
        ├── BusinessException.java
        ├── InventoryNotFoundException.java
        ├── InsufficientInventoryException.java
        └── handler/GlobalExceptionHandler.java
```

### 11.1 Inventory Entity
```
Table: inventory
Fields:
  - id: Long (IDENTITY)
  - eventName: String (not null, max 200, @NotBlank)
  - eventDate: LocalDateTime (not null, @Future)
  - totalSeats: Integer (not null, min 1)
  - availableSeats: Integer (not null, min 0)
  - price: BigDecimal (not null, > 0, precision 10, scale 2)
  - location: String (not null, max 300)
  - createdAt: LocalDateTime (@CreationTimestamp)
  - updatedAt: LocalDateTime (@UpdateTimestamp)
```

### 11.2 API Endpoints
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /inventory | Tạo inventory mới |
| GET | /inventory/{id} | Lấy theo ID |
| GET | /inventory/event/{eventName} | Lấy theo event name |
| GET | /inventory | Lấy tất cả |
| GET | /inventory/available | Lấy events còn chỗ |
| GET | /inventory/date-range?start=X&end=Y | Lấy theo khoảng ngày |
| PUT | /inventory/{id} | Cập nhật |
| POST | /inventory/{id}/reserve?quantity=X | **Reserve seats** (distributed lock) |
| POST | /inventory/{id}/release?quantity=X | **Release seats** (distributed lock) |
| DELETE | /inventory/{id} | Xoá |

### 11.3 Distributed Locking Logic (QUAN TRỌNG)

**reserveSeats(inventoryId, quantity):**
```
1. Lock key = "inventory:lock:{inventoryId}"
2. redissonClient.getLock(lockKey)
3. tryLock(waitTime=10s, leaseTime=5s)
4. Nếu lock thành công:
   a. Lấy inventory từ DB
   b. Kiểm tra availableSeats >= quantity
   c. Trừ: availableSeats -= quantity
   d. Save
   e. Unlock
   f. Return true
5. Nếu không lock được → return false
6. Transaction Isolation: SERIALIZABLE

@CacheEvict(value = {"inventory", "availableEvents"}, allEntries = true)
```

**releaseSeats(inventoryId, quantity):** - Logic tương tự nhưng cộng seats
```
- Kiểm tra newAvailableSeats <= totalSeats (không vượt quá tổng)
```

### 11.4 RedissonConfig
```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer()
        .setAddress("redis://redis:6379")
        .setConnectionPoolSize(10)
        .setConnectionMinimumIdleSize(5);
    return Redisson.create(config);
}
```

### 11.5 Caching
```
- getInventoryById: @Cacheable(value = "inventory", key = "#id")
- getInventoryByEventName: @Cacheable(value = "inventory", key = "'event:' + #eventName")
- getAvailableEvents: @Cacheable(value = "availableEvents")
- updateInventory: @CachePut + @CacheEvict("availableEvents")
- reserveSeats/releaseSeats: @CacheEvict({"inventory", "availableEvents"})
```

### 11.6 application.yml (Đầy đủ)
```yaml
server:
  port: 8085

spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:mysql://mysql:3306/inventorydb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: inventory-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

logging:
  level:
    com.heditra: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 11.7 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: inventory-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-cloud-starter-consul-discovery
  6.  spring-boot-starter-actuator
  7.  spring-boot-starter-validation
  8.  spring-boot-starter-data-redis
  9.  spring-boot-starter-cache
  10. redisson-spring-boot-starter:3.25.0        ← Distributed Locking
  11. spring-cloud-starter-circuitbreaker-resilience4j
  12. resilience4j-spring-boot3
  13. springdoc-openapi-starter-webmvc-ui:2.3.0
  14. micrometer-registry-prometheus
  15. lombok (optional)
  16. spring-boot-starter-test (test)
  17. spring-kafka-test (test)
  18. h2 (test)

LƯU Ý: Inventory KHÔNG dùng spring-kafka trực tiếp,
nhưng vẫn có kafka config trong application.yml vì common-event-library cần.

DependencyManagement: spring-cloud-dependencies:2023.0.0
Build Plugins: (giống các service khác)
```

---

## 12. NOTIFICATION SERVICE

**Port:** 8084 | **Database:** notificationdb | **Kafka Group:** notification-service-group
**ĐẶC BIỆT:** Chủ yếu là event CONSUMER, lắng nghe 3 topics

### Cấu trúc:
```
notification-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/notificationservice/
    ├── NotificationServiceApplication.java
    ├── controller/NotificationController.java
    ├── service/
    │   ├── NotificationService.java
    │   └── impl/NotificationServiceImpl.java    ← Kafka listeners
    ├── repository/NotificationRepository.java
    ├── model/
    │   ├── Notification.java
    │   ├── NotificationType.java      ← EMAIL, SMS, PUSH
    │   └── NotificationStatus.java    ← PENDING, SENT, FAILED
    ├── config/KafkaConfig.java
    └── exception/ (giống pattern khác)
```

### 12.1 Notification Entity
```
Table: notifications
Fields:
  - id: Long (IDENTITY)
  - userId: Long (not null)
  - message: String (not null, max 1000)
  - type: NotificationType enum (EMAIL, SMS, PUSH)
  - status: NotificationStatus enum (PENDING, SENT, FAILED)
  - sentAt: LocalDateTime (@CreationTimestamp)
```

### 12.2 Kafka Listeners (3 topics)

| Topic | Event Class | Hành động |
|-------|-----------|----------|
| `user-created` | UserCreatedEvent | Tạo notification "Welcome! Your account has been created" (EMAIL) |
| `ticket-created` | TicketCreatedEvent | Tạo notification "Your ticket has been booked" (EMAIL) |
| `payment-completed` | PaymentCompletedEvent | Tạo notification "Payment processed successfully" (EMAIL) |

**Logic cho mỗi listener:**
1. Validate event (null check, required fields)
2. Build Notification (userId, message, type=EMAIL, status=PENDING)
3. Save → createNotification()
4. Send → sendNotification()
   - executeNotificationSending() → **STUB: always return true** (mock)
   - Nếu thành công: status → SENT
   - Nếu thất bại: status → FAILED

### 12.3 API Endpoints
| Method | Path | Mô tả |
|--------|------|--------|
| POST | /notifications | Tạo notification |
| GET | /notifications/{id} | Lấy theo ID |
| GET | /notifications | Lấy tất cả |
| GET | /notifications/user/{userId} | Lấy theo user |
| GET | /notifications/status/{status} | Lấy theo status |
| GET | /notifications/type/{type} | Lấy theo type |
| POST | /notifications/{id}/send | Gửi notification |
| DELETE | /notifications/{id} | Xoá |

### 12.4 application.yml (Đầy đủ)
```yaml
server:
  port: 8084

spring:
  application:
    name: notification-service
  datasource:
    url: jdbc:mysql://mysql:3306/notificationdb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: redis
      port: 6379
      timeout: 2000ms
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: notification-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

logging:
  level:
    com.heditra: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 12.5 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: notification-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies:
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-kafka
  6.  spring-cloud-starter-consul-discovery
  7.  spring-boot-starter-actuator
  8.  spring-boot-starter-validation
  9.  spring-boot-starter-data-redis
  10. spring-boot-starter-cache
  11. redisson-spring-boot-starter:3.25.0
  12. spring-cloud-starter-circuitbreaker-resilience4j
  13. resilience4j-spring-boot3
  14. springdoc-openapi-starter-webmvc-ui:2.3.0
  15. micrometer-registry-prometheus
  16. lombok (optional)
  17. spring-boot-starter-test (test)
  18. spring-kafka-test (test)
  19. h2 (test)

DependencyManagement: spring-cloud-dependencies:2023.0.0
Build Plugins: (giống các service khác)
```

---

## 13. SAGA ORCHESTRATOR SERVICE

**Port:** 8086 | **Database:** sagadb (tạo tự động bởi JPA) | **Kafka Group:** saga-orchestrator-group
**ĐẶC BIỆT:** Điều phối distributed transaction theo Saga Pattern

### Cấu trúc:
```
saga-orchestrator-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/heditra/saga/
    ├── SagaOrchestratorApplication.java   ← @EnableKafka
    │
    ├── orchestrator/
    │   └── TicketBookingSaga.java          ← CORE: Kafka listener + saga execution
    │
    ├── compensation/
    │   └── CompensationHandler.java        ← Xử lý rollback
    │
    ├── model/
    │   ├── SagaInstance.java               ← Entity: thông tin saga
    │   ├── SagaStep.java                   ← Entity: từng bước trong saga
    │   ├── SagaStatus.java                 ← STARTED, IN_PROGRESS, COMPLETED, COMPENSATING, COMPENSATED, FAILED
    │   └── StepStatus.java                 ← IN_PROGRESS, COMPLETED, FAILED, COMPENSATED
    │
    ├── repository/
    │   └── SagaInstanceRepository.java
    │
    ├── config/
    │   ├── KafkaConfig.java
    │   └── EventPublisherConfig.java
    │
    └── exception/
        ├── BusinessException.java
        ├── SagaNotFoundException.java
        └── handler/GlobalExceptionHandler.java
```

### 13.1 Database Schema

**saga_instances table:**
```
- sagaId: String (PK, UUID)
- ticketId: Long
- status: SagaStatus enum
- startedAt: LocalDateTime
- completedAt: LocalDateTime
- compensationReason: String
- steps: OneToMany → SagaStep
```

**saga_steps table:**
```
- id: Long (IDENTITY)
- saga_id: FK → saga_instances
- stepName: String ("inventory-reservation", "payment-initiation", "notification-sending")
- status: StepStatus enum
- compensationAction: String
- executedAt: LocalDateTime
- compensatedAt: LocalDateTime
- errorMessage: String
```

### 13.2 TicketBookingSaga (Core Logic)

```
@KafkaListener(topics = "ticket-created", groupId = "saga-orchestrator-group")
onTicketCreated(TicketCreatedEvent):

1. Validate event (ticketId != null)
2. Tạo SagaInstance:
   - sagaId = UUID
   - ticketId = event.getTicketId()
   - status = STARTED
   - startedAt = now()
3. Save saga
4. Execute 3 steps (sequential):
   a. executeStep("inventory-reservation", () -> true)   ← STUB
   b. executeStep("payment-initiation", () -> true)      ← STUB
   c. executeStep("notification-sending", () -> true)    ← STUB
5. Nếu tất cả OK: status → IN_PROGRESS
6. Nếu bất kỳ step nào fail: compensate(saga, errorMessage)
```

**executeStep(saga, stepName, executor):**
```
1. Tạo SagaStep (status = IN_PROGRESS, executedAt = now())
2. Thêm step vào saga
3. Gọi executor.execute()
4. Nếu true: step.status → COMPLETED
5. Nếu false hoặc exception: step.status → FAILED, throw
```

### 13.3 CompensationHandler

```
compensate(saga):
1. Lọc steps có status = COMPLETED
2. Sort NGƯỢC theo executedAt (compensation theo thứ tự ngược)
3. Với mỗi step:
   - "inventory-reservation" → compensateInventoryReservation() (stub: log)
   - "payment-initiation" → compensatePaymentInitiation() (stub: log)
   - "notification-sending" → skip (no compensation needed)
4. Set step.status → COMPENSATED, compensatedAt = now()
```

### 13.4 SagaStatus Flow
```
STARTED → IN_PROGRESS → COMPLETED
                      → COMPENSATING → COMPENSATED
                                     → FAILED
```

### 13.5 application.yml (Đầy đủ)
```yaml
server:
  port: 8086

spring:
  application:
    name: saga-orchestrator-service
  datasource:
    url: jdbc:mysql://mysql:3306/sagadb?createDatabaseIfNotExist=true
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  cloud:
    consul:
      host: consul
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: saga-orchestrator-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

logging:
  level:
    com.heditra: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 13.6 pom.xml Dependencies (Đầy đủ)
```
Parent: spring-boot-starter-parent 3.2.0
GroupId: com.heditra | ArtifactId: saga-orchestrator-service | Version: 1.0.0
Java: 21 | Spring Cloud: 2023.0.0

Dependencies (NHẸ hơn các service khác):
  1.  com.heditra:common-event-library:1.0.0
  2.  spring-boot-starter-web
  3.  spring-boot-starter-data-jpa
  4.  mysql-connector-j (runtime)
  5.  spring-kafka
  6.  spring-cloud-starter-consul-discovery
  7.  spring-boot-starter-actuator
  8.  micrometer-registry-prometheus
  9.  lombok (optional)
  10. spring-boot-starter-test (test)
  11. spring-kafka-test (test)
  12. h2 (test)

KHÔNG CÓ: validation, redis, cache, redisson, resilience4j, springdoc
(Saga orchestrator chỉ lắng nghe Kafka và ghi DB, không cần các thành phần phức tạp)

DependencyManagement: spring-cloud-dependencies:2023.0.0
Build Plugins: (giống các service khác)
```

---

## 14. NGINX (Edge Layer)

**Mục đích:** Reverse proxy, SSL termination, rate limiting, security headers

### Cấu hình Chi Tiết (nginx.conf):

**Performance:**
- worker_processes: auto
- worker_connections: 1024
- epoll + multi_accept
- sendfile, tcp_nopush, tcp_nodelay
- keepalive_timeout: 65s
- gzip compression (level 6)

**Rate Limiting:**
- api_limit: 100 requests/minute per IP (burst 20)
- login_limit: 10 requests/minute per IP (burst 5) - cho /users/login, /users/register
- conn_limit: 10 concurrent connections per IP

**Upstream:**
```nginx
upstream api_gateway {
    least_conn;                                      # Load balancing algorithm
    server api-gateway:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

**SSL (self-signed, tạo trong Dockerfile):**
```
- TLSv1.2 + TLSv1.3
- Strong cipher suites
- Session cache: shared:SSL:10m
- Session timeout: 10m
```

**Security Headers:**
```
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
```

**Proxy Settings:**
```
proxy_connect_timeout: 60s
proxy_send_timeout: 60s
proxy_read_timeout: 60s
proxy_next_upstream: error timeout invalid_header http_500 http_502 http_503
proxy_next_upstream_tries: 2
```

**Health Check:** `/health` → return 200 "healthy\n"

**WebSocket Support:** `/ws` → proxy_set_header Upgrade + Connection "upgrade"

**HTTP → HTTPS Redirect:** Port 80 → 301 to https

---

## 15. KUBERNETES MANIFESTS

### Cấu trúc:
```
k8s/
├── namespace.yaml                      ← Namespace: ticketing-system
├── deployments/
│   ├── api-gateway-deployment.yaml     ← 2 replicas, 512Mi-1Gi RAM
│   └── saga-orchestrator-deployment.yaml ← 2 replicas, 512Mi-1Gi RAM
├── services/
│   ├── api-gateway-service.yaml        ← LoadBalancer, port 8080
│   └── saga-orchestrator-service.yaml  ← ClusterIP, port 8086
├── ingress/
│   └── api-ingress.yaml               ← NGINX Ingress, TLS, rate limiting
├── nginx/
│   ├── configmap.yaml                  ← NGINX config as ConfigMap
│   ├── deployment.yaml                 ← 3 replicas, anti-affinity
│   ├── service.yaml                    ← LoadBalancer, port 80/443
│   └── hpa.yaml                        ← Auto-scale 3-10 pods
└── autoscaling/
    └── hpa.yaml                        ← API Gateway HPA: 2-10 pods
```

### Chi Tiết:

**HPA (Horizontal Pod Autoscaler):**
```yaml
API Gateway: min=2, max=10, CPU target=70%, Memory target=80%
NGINX: min=3, max=10, CPU=70%, Memory=80%
  - scaleDown: stabilization 300s, max 50%/60s
  - scaleUp: stabilization 0s, max 100%/30s or 2 pods/30s
```

**Liveness/Readiness Probes:**
```yaml
livenessProbe: /actuator/health, initialDelay=30-60s, period=10-30s
readinessProbe: /actuator/health, initialDelay=20-30s, period=5-10s
```

**Ingress:**
```yaml
Host: ticketing.heditra.com
TLS: ticketing-tls-secret
Rate limit: 100 rps, 10 connections
Proxy timeouts: 60s
SSL redirect: force
```

---

## 16. MONITORING (Prometheus + Grafana)

### Prometheus (prometheus.yml)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs: (6 jobs)
  - api-gateway:8080/actuator/prometheus
  - user-service:8082/actuator/prometheus
  - ticket-service:8081/actuator/prometheus
  - payment-service:8083/actuator/prometheus
  - notification-service:8084/actuator/prometheus
  - inventory-service:8085/actuator/prometheus
```

### Grafana
- Auto-provision Prometheus datasource (grafana/provisioning/datasources/prometheus.yml)
- Default credentials: admin/admin
- Port: 3000

### Spring Actuator Config (trong mỗi service)
```yaml
management:
  endpoints.web.exposure.include: health, info, prometheus
  metrics.export.prometheus.enabled: true
```

---

## 17. CI/CD PIPELINES

### 17.1 CI Pipeline (ci.yml)
```
Trigger: push/PR to master, develop

Job 1: code-quality
  ├── Checkout → JDK 21
  ├── Checkstyle check (continue-on-error)
  ├── Tests + JaCoCo report
  └── Upload coverage to Codecov

Job 2: build-and-test (depends on code-quality)
  ├── Maven clean install
  └── Upload JAR artifacts (7 days retention)

Job 3: security-scan (depends on build-and-test)
  ├── Trivy filesystem scan (CRITICAL, HIGH, MEDIUM)
  └── Upload SARIF to GitHub Security
```

### 17.2 CodeQL (codeql.yml)
```
Trigger: push/PR to master + weekly Monday 00:00
Language: Java
Steps: Init → Build → Analyze
```

### 17.3 SonarCloud (sonarcloud.yml)
```
Trigger: push/PR to master, develop + manual dispatch
Steps: Build → SonarQube Scan
```

### 17.4 Dependabot (dependabot.yml)
```
Maven: weekly (Monday), limit 10 PRs, labels: dependencies, security
GitHub Actions: monthly, labels: ci/cd
```

---

## 18. DATABASE SCHEMA

### 18.1 userdb.users
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL |
| first_name | VARCHAR(50) | NOT NULL |
| last_name | VARCHAR(50) | NOT NULL |
| role | VARCHAR(20) | NOT NULL (ADMIN/USER/MANAGER) |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### 18.2 ticketdb.tickets
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL |
| event_name | VARCHAR(200) | NOT NULL |
| quantity | INT | NOT NULL |
| total_price | DECIMAL(10,2) | NOT NULL |
| price_per_ticket | DECIMAL(10,2) | NOT NULL |
| total_amount | DECIMAL(10,2) | NOT NULL |
| status | VARCHAR(20) | NOT NULL (PENDING/CONFIRMED/CANCELLED/COMPLETED) |
| booking_date | DATETIME | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |
| event_date | DATETIME | NOT NULL |
| version | BIGINT | NOT NULL (optimistic locking) |

### 18.3 paymentdb.payments
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| ticket_id | BIGINT | NOT NULL |
| user_id | BIGINT | NOT NULL |
| amount | DECIMAL(10,2) | NOT NULL |
| payment_method | VARCHAR(20) | NOT NULL (CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER/DIGITAL_WALLET) |
| status | VARCHAR(20) | NOT NULL (PENDING/SUCCESS/FAILED/REFUNDED) |
| transaction_id | VARCHAR(100) | UNIQUE |
| created_at | DATETIME | NOT NULL |

### 18.4 notificationdb.notifications
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL |
| message | VARCHAR(1000) | NOT NULL |
| type | VARCHAR(20) | NOT NULL (EMAIL/SMS/PUSH) |
| status | VARCHAR(20) | NOT NULL (PENDING/SENT/FAILED) |
| sent_at | DATETIME | NOT NULL |

### 18.5 inventorydb.inventory
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| event_name | VARCHAR(200) | NOT NULL |
| event_date | DATETIME | NOT NULL |
| total_seats | INT | NOT NULL |
| available_seats | INT | NOT NULL |
| price | DECIMAL(10,2) | NOT NULL |
| location | VARCHAR(300) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### 18.6 sagadb (tự tạo bởi JPA)

**saga_instances:**
| Column | Type | Constraints |
|--------|------|-------------|
| saga_id | VARCHAR(255) | PK |
| ticket_id | BIGINT | |
| status | VARCHAR(20) | (STARTED/IN_PROGRESS/COMPLETED/COMPENSATING/COMPENSATED/FAILED) |
| started_at | DATETIME | |
| completed_at | DATETIME | |
| compensation_reason | VARCHAR(255) | |

**saga_steps:**
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| saga_id | VARCHAR(255) | FK → saga_instances |
| step_name | VARCHAR(255) | |
| status | VARCHAR(20) | (IN_PROGRESS/COMPLETED/FAILED/COMPENSATED) |
| compensation_action | VARCHAR(255) | |
| executed_at | DATETIME | |
| compensated_at | DATETIME | |
| error_message | VARCHAR(255) | |

---

## 19. KAFKA TOPICS & EVENT MAP

### Topics
| Topic Name | Publisher | Consumer(s) |
|-----------|-----------|------------|
| `ticket-created` | Ticket Service | Payment Service, Notification Service, Saga Orchestrator |
| `ticket-confirmed` | Ticket Service | (chưa có consumer) |
| `ticket-cancelled` | Ticket Service | (chưa có consumer) |
| `payment-initiated` | Payment Service | (chưa có consumer) |
| `payment-completed` | Payment Service | Ticket Service, Notification Service |
| `payment-failed` | Payment Service | Ticket Service |
| `payment-refunded` | Payment Service | (chưa có consumer) |
| `user-created` | User Service | Notification Service |
| `user-updated` | User Service | (chưa có consumer) |
| `user-deleted` | User Service | (chưa có consumer) |
| `event-store` | KafkaEventStore | (internal event sourcing) |
| `event-dlq` | Kafka (auto) | DeadLetterQueueHandler |

### Consumer Groups
| Group ID | Service | Subscribed Topics |
|----------|---------|-------------------|
| ticket-service-group | Ticket Service | payment-completed, payment-failed |
| payment-service-group | Payment Service | ticket-created |
| notification-service-group | Notification Service | user-created, ticket-created, payment-completed |
| saga-orchestrator-group | Saga Orchestrator | ticket-created |

### Event Flow Diagram
```
User Service ──user-created──────────────────────────────────> Notification Service
                                                               (Welcome email)

Ticket Service ──ticket-created──┬────────────────────────────> Payment Service
                                 │                              (Create payment record)
                                 ├────────────────────────────> Notification Service
                                 │                              (Booking confirmation)
                                 └────────────────────────────> Saga Orchestrator
                                                                (Start saga)

Payment Service ──payment-completed──┬────────────────────────> Ticket Service
                                     │                          (Confirm ticket)
                                     └────────────────────────> Notification Service
                                                                (Payment success)

Payment Service ──payment-failed─────────────────────────────> Ticket Service
                                                                (Handle failure)
```

### Kafka Consumer Type Mapping (QUAN TRỌNG khi code KafkaConfig)

Mỗi consumer service cần config `spring.json.type.mapping` để deserialize đúng event class:

| Service | Cần mapping cho events | Ví dụ trong KafkaConfig |
|---------|----------------------|------------------------|
| **Payment Service** | TicketCreatedEvent | `TicketCreatedEvent:com.heditra.events.ticket.TicketCreatedEvent` |
| **Ticket Service** | PaymentCompletedEvent, PaymentFailedEvent | `PaymentCompletedEvent:..., PaymentFailedEvent:...` |
| **Notification Service** | UserCreatedEvent, TicketCreatedEvent, PaymentCompletedEvent | Map 3 event types |
| **Saga Orchestrator** | TicketCreatedEvent | `TicketCreatedEvent:com.heditra.events.ticket.TicketCreatedEvent` |

> **Lưu ý:** DomainEvent dùng `@JsonTypeInfo(use = Id.CLASS)` nên JSON chứa `@class` field.
> Nếu vẫn gặp lỗi deserialization → thêm `spring.json.trusted.packages=*` trong consumer config.

---

## 20. THỨ TỰ XÂY DỰNG ĐỀ XUẤT

### Phase 1: Foundation (Nền tảng)
```
□ 1.1  Tạo Parent POM (multi-module Maven project)
□ 1.2  Tạo common-event-library
        ├── Core classes: DomainEvent, EventPublisher, EventHandler
        ├── Infrastructure: KafkaEventPublisher, EventKafkaConfig
        ├── Event Sourcing: AggregateRoot, EventStore, KafkaEventStore
        └── Domain Events: tất cả event classes
□ 1.3  Tạo docker-compose.yml với infrastructure services
        ├── MySQL + init-db.sql
        ├── Redis
        ├── Kafka + Zookeeper
        ├── Consul
        └── Elasticsearch
□ 1.4  Tạo .env.example, .gitignore
```

### Phase 2: Core Services (Services cơ bản)
```
□ 2.1  User Service (đơn giản nhất, bắt đầu từ đây)
        ├── Entity, Repository, Service, Controller
        ├── DTOs, Mapper, Exception handling
        ├── Redis caching
        ├── Kafka event publishing
        └── Swagger docs
□ 2.2  Inventory Service
        ├── Entity, Repository, Service, Controller
        ├── Redisson distributed locking
        └── Redis caching
□ 2.3  Ticket Service
        ├── Entity, Repository, Service, Controller
        ├── CQRS pattern (Commands + Queries + Handlers)
        ├── WebClient call đến Inventory Service
        ├── Circuit Breaker (Resilience4j)
        ├── Event publishing (ticket-created, confirmed, cancelled)
        └── Event consuming (payment-completed, payment-failed)
□ 2.4  Payment Service
        ├── Entity, Repository, Service, Controller
        ├── Event consuming (ticket-created → auto create payment)
        ├── Event publishing (payment-initiated, completed, failed, refunded)
        └── Payment processing logic (stub)
□ 2.5  Notification Service
        ├── Entity, Repository, Service, Controller
        ├── 3 Kafka listeners (user-created, ticket-created, payment-completed)
        └── Notification sending logic (stub)
```

### Phase 3: Orchestration (Điều phối)
```
□ 3.1  Saga Orchestrator Service
        ├── SagaInstance + SagaStep entities
        ├── TicketBookingSaga (Kafka listener + step execution)
        ├── CompensationHandler (reverse compensation)
        └── Saga status management
□ 3.2  API Gateway
        ├── Spring Cloud Gateway (WebFlux-based)
        ├── Route configuration
        ├── CORS config
        ├── Consul integration
        └── Error handling
```

### Phase 4: Infrastructure & DevOps ⏳ (LÀM SAU)
```
⏳ 4.1  Dockerfiles cho tất cả services
⏳ 4.2  NGINX configuration (Reverse proxy, SSL, Rate limiting, Security headers)
⏳ 4.3  Prometheus + Grafana (Monitoring)
⏳ 4.4  Kubernetes manifests (Namespace, Deployments, Services, Ingress, HPA)
⏳ 4.5  CI/CD Pipelines (GitHub Actions, CodeQL, SonarCloud, Dependabot)
```

### Phase 5: Testing & Polish ⏳ (LÀM SAU)
```
⏳ 5.1  Unit tests + Integration tests
⏳ 5.2  Test configuration (H2, mock Kafka)
⏳ 5.3  README.md + Architecture diagram
```

> **GHI CHÚ:** Phase 4 và 5 đã được ghi nhận đầy đủ ở Section 14-17 trong blueprint.
> Ưu tiên hoàn thành Phase 1-3 (core services) trước, sau đó quay lại làm Phase 4-5.

---

## GHI CHÚ QUAN TRỌNG KHI CODE LẠI

1. **Build order:** common-event-library PHẢI build trước (mvn install) rồi mới build các service khác.

2. **Consul dependency:** Tất cả services dùng `@EnableDiscoveryClient` + Consul. Khi chạy local không có Consul, cần disable trong test config.

3. **Kafka serialization:** Dùng `JsonSerializer` cho producer, `JsonDeserializer` cho consumer. Cần set `trusted.packages=*` để deserialize events.

4. **Event key = aggregateId:** Đảm bảo ordering cho cùng 1 entity (ví dụ: tất cả events của ticket ID=1 đi vào cùng partition).

5. **Password chưa mã hóa:** User service lưu plain text password. Trong thực tế cần dùng BCryptPasswordEncoder.

6. **Payment gateway là stub:** `executePaymentGateway()` luôn return true. Cần integrate gateway thật (Stripe, PayPal, ...).

7. **Notification sending là stub:** `executeNotificationSending()` luôn return true. Cần integrate email (SendGrid, SES), SMS (Twilio), push notification.

8. **Saga steps là stub:** 3 steps trong TicketBookingSaga đều return true. Cần implement logic thật (gọi API inventory, payment).

9. **Event sourcing chỉ là demo:** KafkaEventStore dùng in-memory cache. Production cần database thật.

10. **JPA ddl-auto: update** trong tất cả services → tự tạo/update tables. Production nên dùng Flyway hoặc Liquibase.

11. **Không có Authentication/Authorization:** Chưa có JWT filter, Spring Security. Đây là điểm cần bổ sung nếu muốn production-ready.

12. **Test configs** dùng H2 in-memory database và disable Consul, Redis để chạy được mà không cần infrastructure.

13. **Chạy local development:** Khi chạy services từ IDE (không phải Docker), cần đổi hostname trong application.yml:
    - `mysql:3306` → `localhost:3306`
    - `redis:6379` → `localhost:6379`
    - `kafka:9092` → `localhost:29092`
    - `consul:8500` → `localhost:8500`
    - `elasticsearch:9200` → `localhost:9200`
    Hoặc tạo `application-local.yml` riêng và chạy với `--spring.profiles.active=local`

---

## TEST APPLICATION.YML (Pattern dùng chung cho tất cả services)

> File: `src/test/resources/application.yml` (trong mỗi service)
> Mục đích: Chạy tests mà KHÔNG cần Docker infrastructure

```yaml
spring:
  application:
    name: {service-name}
  cloud:
    config:
      enabled: false
      import-check:
        enabled: false
    consul:
      enabled: false
      discovery:
        enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  data:
    redis:
      host: localhost
      port: 6379
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      auto-offset-reset: earliest
      group-id: {service-name}-test

eureka:
  client:
    enabled: false

server:
  port: 0

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

> Thay `{service-name}` bằng: user-service, ticket-service, payment-service, v.v.
> Dùng H2 in-memory thay MySQL, disable Consul, Redis, Eureka.
