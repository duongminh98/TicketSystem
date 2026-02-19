# KẾ HOẠCH 5 NGÀY - Code Lại Event-Driven Ticketing Platform

> Phạm vi: Chỉ tập trung vào CORE (Common Library + 6 Microservices + API Gateway).
> NGINX, K8s, CI/CD, Monitoring, Dockerfiles sẽ làm sau.
> Mỗi ngày ~5-7 giờ code, có thời gian nghỉ + review.
> Sau mỗi task có checkpoint TEST để chắc chắn chạy được rồi mới đi tiếp.

---

## CÁCH SỬ DỤNG FILE NÀY

```
File này dùng cùng với PROJECT_BLUEPRINT.md:

📅 File này (5_DAY_PLAN.md) = HƯỚNG DẪN THỰC HIỆN
   → Cho bạn biết: "Bước tiếp theo là gì?", "Hôm nay làm gì?"
   → Mỗi bước có tham chiếu đến section tương ứng trong BLUEPRINT

📋 PROJECT_BLUEPRINT.md = TÀI LIỆU THAM CHIẾU CHI TIẾT
   → Chứa mọi chi tiết kỹ thuật: file structure, logic, YAML config, dependencies
   → Tra cứu khi cần biết: "Field nào?", "Config gì?", "Logic ra sao?"

Workflow:
  1. Đọc bước hiện tại trong file này
  2. Mở BLUEPRINT → tìm section liên quan (mỗi bước ghi rõ tham chiếu)
  3. Code theo chi tiết trong BLUEPRINT
  4. Test theo checkpoint trong file này
  5. Tick ✅ → tiếp bước sau

Dùng với AI Assistant:
  → Đưa CẢ 2 FILE cho AI (Cursor, ChatGPT, Claude, ...)
  → Nói: "Giúp mình làm bước X trong kế hoạch ngày Y"
  → AI sẽ tra cứu BLUEPRINT và code giúp bạn từng bước
```

---

## TRƯỚC KHI BẮT ĐẦU - Chuẩn bị (30 phút)

```
□ Cài Java 21 (JDK Temurin)
□ Cài Maven 3.9+
□ Cài Docker Desktop
□ Cài IDE (IntelliJ IDEA recommended)
□ Cài Postman hoặc Bruno (test API)
□ Tạo repo Git mới, commit đầu tiên
```

---

## NGÀY 1: NỀN TẢNG + COMMON EVENT LIBRARY
> Mục tiêu cuối ngày: Parent POM + Common Library build thành công + Infrastructure chạy
> 📋 Tham chiếu BLUEPRINT: Section 5 (Root Files), Section 6 (Common Event Library)

---

### SÁNG (2-3h): Khung dự án + Docker Infrastructure

#### Bước 1.1 - Tạo Parent POM (20 phút)
```
□ Tạo thư mục project gốc
□ Tạo pom.xml (parent)
   - parent: spring-boot-starter-parent:3.2.0
   - groupId: com.heditra
   - artifactId: ticketing-system
   - packaging: pom
   - java.version: 21
   - spring-cloud.version: 2023.0.0
   - Khai báo 8 modules (common-event-library, api-gateway, 6 services)
   - pluginManagement: spring-boot-maven-plugin, checkstyle, jacoco

✅ TEST: chạy "mvn validate" → không lỗi (sẽ warning chưa có modules, OK)
```

#### Bước 1.2 - Docker Compose Infrastructure (30 phút)
```
□ Tạo docker-compose.yml - CHỈ infrastructure services:
   1. mysql (port 3306) + healthcheck + mount init-db.sql
   2. redis (port 6379) + healthcheck + appendonly
   3. zookeeper (port 2181) + healthcheck
   4. kafka (port 9092, 29092) + healthcheck + depends_on zookeeper
   5. consul (port 8500) + healthcheck
   - Network: ticketing-network
   - Volumes: mysql-data, redis-data

□ Tạo init-db.sql:
   CREATE DATABASE IF NOT EXISTS ticketdb;
   CREATE DATABASE IF NOT EXISTS userdb;
   CREATE DATABASE IF NOT EXISTS paymentdb;
   CREATE DATABASE IF NOT EXISTS notificationdb;
   CREATE DATABASE IF NOT EXISTS inventorydb;

□ Tạo .gitignore
□ Tạo .env.example

✅ TEST: "docker-compose up -d" → tất cả containers healthy
   - docker-compose ps → 5 containers running
   - MySQL: docker exec mysql mysql -uroot -prootpassword -e "SHOW DATABASES;" → thấy 5 DB
   - Consul: mở http://localhost:8500 → thấy UI
   - Redis: docker exec redis redis-cli ping → PONG
```

**GIT COMMIT: "feat: init project structure + docker infrastructure"**

---

### CHIỀU (2-3h): Common Event Library

#### Bước 1.3 - Core Package (1h)
```
Đây là thư viện dùng chung, build TRƯỚC tất cả services.

□ Tạo folder common-event-library/
□ Tạo pom.xml
   - Dependencies: spring-kafka, spring-boot-starter, jackson-databind,
     jackson-datatype-jsr310, lombok, slf4j-api, jakarta.validation-api
   - KHÔNG có spring-boot-maven-plugin (đây là library, không phải app)

□ Tạo package com.heditra.events.core/ (6 files):
   1. DomainEvent.java
      - abstract class, implements Serializable
      - @JsonTypeInfo(use = Id.CLASS) ← quan trọng cho deserialization
      - Fields: eventId (String), eventType (String), occurredAt (LocalDateTime),
        aggregateId (String), version (Integer)
      - Constructor: set occurredAt = LocalDateTime.now()

   2. EventPublisher.java
      - interface
      - publish(T event) → CompletableFuture<Void>
      - publish(String topic, T event) → CompletableFuture<Void>

   3. EventHandler.java
      - interface generic: <T extends DomainEvent>
      - handle(T event)
      - getEventType() → Class<T>

   4. EventEnvelope.java
      - Generic class <T extends DomainEvent>
      - Fields: EventMetadata metadata, T payload

   5. EventMetadata.java
      - Fields: eventId, eventType, source, correlationId, causationId, timestamp (Long)

   6. EventProcessingException.java
      - extends RuntimeException
      - 2 constructors: (message), (message, cause)
```

#### Bước 1.4 - Infrastructure Package (45 phút)
```
□ Tạo package com.heditra.events.infrastructure/ (4 files):

   1. EventKafkaConfig.java (@Configuration, @EnableKafka)
      - @Value bootstrap-servers (default: kafka:9092)
      - @Bean ObjectMapper: registerModule(JavaTimeModule), disable WRITE_DATES_AS_TIMESTAMPS
      - @Bean ProducerFactory: StringSerializer + JsonSerializer, acks=all, retries=3, idempotence=true
      - @Bean KafkaTemplate

   2. KafkaEventPublisher.java (@Component, implements EventPublisher)
      - Inject KafkaTemplate<String, Object>
      - publish(event): tự suy topic bằng deriveTopicFromEvent()
      - publish(topic, event): validate null → kafkaTemplate.send(topic, aggregateId, event)
      - deriveTopicFromEvent: "TicketCreated" → "ticket-created" (camelCase → kebab-case)

   3. KafkaEventListener.java (@Component)
      - Map<Class, EventHandler> handlers (ConcurrentHashMap)
      - Constructor nhận List<EventHandler> → build map
      - processEvent(event): tìm handler → gọi handle()

   4. DeadLetterQueueHandler.java (@Component)
      - @KafkaListener(topics = "event-dlq")
      - Log error + acknowledge
```

#### Bước 1.5 - Domain Events (45 phút)
```
□ 13 event classes, mỗi file rất ngắn (~20-30 dòng):
  Pattern: extends DomainEvent, @Data, @NoArgsConstructor, @Builder constructor

  com.heditra.events.user/:
   □ UserCreatedEvent     - fields: userId, username, email, role
   □ UserUpdatedEvent     - fields: userId, username, email, role
   □ UserDeletedEvent     - fields: userId

  com.heditra.events.ticket/:
   □ TicketCreatedEvent   - fields: ticketId, userId, eventName, quantity, pricePerTicket, totalAmount
   □ TicketConfirmedEvent - fields: ticketId, userId, eventName
   □ TicketCancelledEvent - fields: ticketId, userId, eventName, quantity, cancellationReason

  com.heditra.events.payment/:
   □ PaymentInitiatedEvent  - fields: paymentId, ticketId, userId, amount, paymentMethod, transactionId
   □ PaymentCompletedEvent  - fields: paymentId, ticketId, userId, amount, transactionId
   □ PaymentFailedEvent     - fields: paymentId, ticketId, userId, amount, transactionId, failureReason
   □ PaymentRefundedEvent   - fields: paymentId, ticketId, userId, amount, transactionId, refundReason

  com.heditra.events.inventory/:
   □ InventoryReservedEvent - fields: eventName, quantity, ticketId, reservationId
   □ InventoryReleasedEvent - fields: eventName, quantity, ticketId, reservationId, releaseReason

  com.heditra.events.notification/:
   □ NotificationSentEvent  - fields: notificationId, userId, notificationType, recipient, subject, message, successful

□ (Tùy chọn - nếu còn thời gian) Tạo package sourcing/:
   AggregateRoot, EventStore, KafkaEventStore, EventReplayer, EventSourcingRepository
   → Nếu không kịp thì bỏ qua, thêm sau được

✅ TEST: chạy "mvn clean install" trong common-event-library/ → BUILD SUCCESS
```

**GIT COMMIT: "feat: add common-event-library with core events + kafka infrastructure"**

---

## NGÀY 2: USER SERVICE + INVENTORY SERVICE
> Mục tiêu cuối ngày: 2 services chạy độc lập, CRUD + caching + event publishing
> 📋 Tham chiếu BLUEPRINT: Section 8 (User Service), Section 11 (Inventory Service)

---

### SÁNG (3h): User Service (service đầu tiên - làm kỹ nhất)

#### Bước 2.1 - Khung + Model + Repository (30 phút)
```
□ Tạo folder user-service/
□ Tạo pom.xml:
   - Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
     mysql-connector-j, spring-kafka, spring-cloud-starter-consul-discovery,
     spring-boot-starter-data-redis, springdoc-openapi-starter-webmvc-ui (2.3.0),
     micrometer-registry-prometheus, spring-boot-starter-actuator,
     lombok, spring-boot-starter-test, common-event-library (1.0.0)

□ UserServiceApplication.java: @SpringBootApplication + @EnableDiscoveryClient

□ application.yml:
   - server.port: 8082
   - datasource: jdbc:mysql://mysql:3306/userdb?createDatabaseIfNotExist=true
   - jpa: hibernate.ddl-auto=update, dialect=MySQLDialect
   - redis: host=redis, port=6379, timeout=2000ms
   - consul: host=consul, port=8500
   - kafka: bootstrap-servers=kafka:9092, group-id=user-service-group
   - management: expose health, info, prometheus

□ User.java (@Entity, table "users"):
   - id (Long, @Id, @GeneratedValue IDENTITY)
   - username (String, unique, not null, max 50)
   - email (String, unique, not null)
   - password (String, not null)
   - firstName, lastName (String, not null, max 50)
   - role (UserRole enum, @Enumerated STRING)
   - createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
   - Cần @EntityListeners(AuditingEntityListener.class)

□ UserRole.java (enum): ADMIN, USER, MANAGER

□ UserRepository.java (extends JpaRepository<User, Long>):
   - findByUsername(String) → Optional<User>
   - findByEmail(String) → Optional<User>
   - findByRole(UserRole) → List<User>
   - existsByUsername(String) → boolean
   - existsByEmail(String) → boolean
```

#### Bước 2.2 - DTOs + Mapper + Config (30 phút)
```
□ dto/request/CreateUserRequest.java:
   - username (@NotBlank, @Size 3-50)
   - email (@NotBlank, @Email)
   - password (@NotBlank, @Size min=6)
   - firstName (@NotBlank, @Size max=50)
   - lastName (@NotBlank, @Size max=50)
   - role (@NotNull UserRole)

□ dto/request/UpdateUserRequest.java:
   - email, firstName, lastName, role

□ dto/response/UserResponse.java:
   - id, username, email, firstName, lastName, role, createdAt, updatedAt

□ dto/response/ApiResponse<T>.java:
   - success, data, message, timestamp
   - static factory: success(data, message), success(data)

□ dto/common/ApiErrorResponse.java:
   - success, errorCode, message, timestamp, path, details
   - static factory: of(errorCode, message, path)

□ mapper/UserMapper.java (@Component):
   - toEntity(CreateUserRequest) → User
   - toResponse(User) → UserResponse
   - toResponseList(List<User>) → List<UserResponse>
   - updateEntityFromRequest(User, UpdateUserRequest)

□ config/JpaAuditingConfig.java: @Configuration + @EnableJpaAuditing
□ config/RedisConfig.java: @EnableCaching + RedisTemplate + CacheManager (TTL 10 phút)
□ config/KafkaConfig.java: ProducerFactory + KafkaTemplate
□ config/EventPublisherConfig.java: @Bean EventPublisher = new KafkaEventPublisher(kafkaTemplate)
□ config/OpenApiConfig.java: @Bean OpenAPI title="User Service API"
```

#### Bước 2.3 - Exceptions (15 phút)
```
□ BusinessException.java (base, có errorCode field)
□ TechnicalException.java (base)
□ ValidationException.java (base)
□ UserNotFoundException.java extends BusinessException (errorCode = "USER_NOT_FOUND")
□ UserAlreadyExistsException.java extends BusinessException
□ InvalidUserDataException.java extends ValidationException
□ handler/GlobalExceptionHandler.java (@RestControllerAdvice):
   - BusinessException → 400 BAD_REQUEST
   - ValidationException → 400 BAD_REQUEST
   - MethodArgumentNotValidException → 400 (collect field errors)
   - TechnicalException → 500 INTERNAL_SERVER_ERROR
   - Exception (catch-all) → 500

⭐ LƯU Ý: Pattern exception này sẽ COPY y nguyên cho các service sau
   Chỉ đổi tên exception cụ thể (UserNotFound → TicketNotFound, ...)
```

#### Bước 2.4 - Service + Controller (45 phút)
```
□ UserService.java (interface): 8 methods

□ UserServiceImpl.java (@Service, @Transactional(readOnly = true)):
   - createUser (@Transactional):
     1. validateUserUniqueness(request) → check username + email
     2. userMapper.toEntity() → save
     3. publishUserCreatedEvent(savedUser) ← EventPublisher.publish("user-created", event)
     4. return toResponse

   - getUserById: @Cacheable(value="users", key="#id")
   - getUserByUsername: @Cacheable(value="users", key="'username:'+#username")
   - getUserByEmail: @Cacheable(value="users", key="'email:'+#email")
   - getAllUsers, getUsersByRole

   - updateUser (@Transactional, @CachePut):
     1. findById → updateEntityFromRequest → save
     2. publishUserUpdatedEvent

   - deleteUser (@Transactional, @CacheEvict allEntries=true):
     1. findById → delete
     2. publishUserDeletedEvent

□ UserController.java (@RestController, @RequestMapping("/users")):
   - POST   /users              → createUser (201 CREATED)
   - GET    /users/{id}         → getUserById
   - GET    /users/username/{u} → getUserByUsername
   - GET    /users/email/{e}    → getUserByEmail
   - GET    /users              → getAllUsers
   - GET    /users/role/{role}  → getUsersByRole
   - PUT    /users/{id}         → updateUser
   - DELETE /users/{id}         → deleteUser (204 NO_CONTENT)
   - Thêm Swagger annotations: @Tag, @Operation, @ApiResponse
```

#### Bước 2.5 - Test User Service (30 phút)
```
✅ TEST:
   1. docker-compose up -d (infrastructure phải đang chạy)
   2. mvn clean package (trong user-service/) → BUILD SUCCESS
   3. Chạy: mvn spring-boot:run hoặc run trong IDE
   4. Swagger: http://localhost:8082/swagger-ui.html → thấy API docs
   5. Test bằng Postman:
      POST /users → body: { username, email, password, firstName, lastName, role: "USER" } → 201
      GET /users/1 → 200 + user data
      PUT /users/1 → update email → 200
      GET /users → list all → 200
      DELETE /users/1 → 204
   6. Consul: http://localhost:8500 → thấy "user-service" registered
   7. Xem log: thấy "Publishing UserCreatedEvent" → Kafka working
```

**GIT COMMIT: "feat: add user-service with CRUD, caching, event publishing"**

---

### CHIỀU (3h): Inventory Service

#### Bước 2.6 - Khung + Model (30 phút)
```
□ Tạo folder inventory-service/
□ pom.xml: giống user-service + thêm redisson-spring-boot-starter
□ InventoryServiceApplication.java
□ application.yml (port 8085, inventorydb)

□ Inventory.java (@Entity, table "inventory"):
   - id (Long, IDENTITY)
   - eventName (String, not null, max 200, @NotBlank)
   - eventDate (LocalDateTime, not null, @Future)
   - totalSeats (Integer, not null, @Min 1)
   - availableSeats (Integer, not null, @Min 0)
   - price (BigDecimal, not null, precision 10 scale 2, @DecimalMin > 0)
   - location (String, not null, max 300, @NotBlank)
   - createdAt (@CreationTimestamp), updatedAt (@UpdateTimestamp)

□ InventoryRepository.java:
   - findByEventName(String) → Optional<Inventory>
   - existsByEventName(String) → boolean
   - @Query findAvailableEvents(LocalDateTime now) → List<Inventory>
     (WHERE availableSeats > 0 AND eventDate > :now)
   - findByEventDateBetween(start, end) → List<Inventory>
```

#### Bước 2.7 - Config + Exceptions (20 phút)
```
□ config/RedisConfig.java: @EnableCaching + CacheManager
□ config/RedissonConfig.java: ← MỚI, không có ở user-service
   @Bean RedissonClient:
     Config config = new Config()
     config.useSingleServer().setAddress("redis://redis:6379")
           .setConnectionPoolSize(10).setConnectionMinimumIdleSize(5)
     return Redisson.create(config)

□ exception/BusinessException.java (copy từ user-service)
□ exception/InventoryNotFoundException.java extends BusinessException
□ exception/InsufficientInventoryException.java extends BusinessException
□ exception/handler/GlobalExceptionHandler.java (copy + sửa import)
□ dto/common/ApiErrorResponse.java (copy)
```

#### Bước 2.8 - Service Logic với Distributed Locking (1h) ← PHẦN HAY NHẤT
```
□ InventoryService.java (interface): 10 methods

□ InventoryServiceImpl.java (@Service):
   - CONSTANTS: LOCK_KEY = "inventory:lock:", WAIT_TIME = 10s, LEASE_TIME = 5s

   - createInventory: validate → check duplicate eventName → save
   - getInventoryById: @Cacheable(value="inventory", key="#id")
   - getInventoryByEventName: @Cacheable(value="inventory", key="'event:'+#eventName")
   - getAllInventory
   - getAvailableEvents: @Cacheable(value="availableEvents")
   - getEventsByDateRange

   ★ reserveSeats(inventoryId, quantity): ← QUAN TRỌNG
     @Transactional(isolation = SERIALIZABLE)
     @CacheEvict(value = {"inventory", "availableEvents"}, allEntries = true)
     1. lockKey = "inventory:lock:" + inventoryId
     2. RLock lock = redissonClient.getLock(lockKey)
     3. try {
          if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
            try {
              inventory = getInventoryById(inventoryId)
              if (availableSeats < quantity) return false
              availableSeats -= quantity
              save(inventory)
              return true
            } finally { lock.unlock() }
          } else { return false }
        } catch (InterruptedException) { Thread.interrupt(); return false }

   ★ releaseSeats(inventoryId, quantity): ← Tương tự nhưng CỘNG seats
     - Check: newAvailable <= totalSeats

   - updateInventory: @CachePut + @CacheEvict("availableEvents")
   - deleteInventory: @CacheEvict all
```

#### Bước 2.9 - Controller + Test (30 phút)
```
□ InventoryController.java (@RestController, @RequestMapping("/inventory")):
   - POST   /inventory                        → createInventory (201)
   - GET    /inventory/{id}                    → getInventoryById
   - GET    /inventory/event/{eventName}       → getInventoryByEventName
   - GET    /inventory                         → getAllInventory
   - GET    /inventory/available               → getAvailableEvents
   - GET    /inventory/date-range?start&end    → getEventsByDateRange
   - PUT    /inventory/{id}                    → updateInventory
   - POST   /inventory/{id}/reserve?quantity=X → reserveSeats
   - POST   /inventory/{id}/release?quantity=X → releaseSeats
   - DELETE /inventory/{id}                    → deleteInventory (204)

✅ TEST:
   1. Chạy inventory-service (port 8085)
   2. POST /inventory → tạo "Concert A", totalSeats=100, availableSeats=100, price=50.00
   3. POST /inventory/1/reserve?quantity=5 → true, check: availableSeats = 95
   4. POST /inventory/1/reserve?quantity=200 → false (không đủ)
   5. POST /inventory/1/release?quantity=5 → true, check: availableSeats = 100
   6. Consul: http://localhost:8500 → thấy inventory-service registered
```

**GIT COMMIT: "feat: add inventory-service with distributed locking (Redisson)"**

---

## NGÀY 3: TICKET SERVICE (phức tạp nhất)
> Mục tiêu cuối ngày: Ticket Service hoạt động với CQRS + gọi Inventory qua HTTP + events
> 📋 Tham chiếu BLUEPRINT: Section 9 (Ticket Service) - đọc kỹ 9.3, 9.5, 9.6

---

### SÁNG (3h): Ticket Service Core

#### Bước 3.1 - Khung + Model (30 phút)
```
□ Tạo folder ticket-service/
□ pom.xml: giống user-service + THÊM:
   - spring-boot-starter-webflux (cho WebClient - HTTP client)
   - spring-cloud-starter-circuitbreaker-reactor-resilience4j (circuit breaker)
□ TicketServiceApplication.java
□ application.yml (port 8081, ticketdb)

□ Ticket.java (@Entity, table "tickets"):
   - id (Long, IDENTITY)
   - userId (Long, not null, @NotNull)
   - eventName (String, not null, max 200, @NotBlank)
   - quantity (Integer, not null, @Min 1)
   - totalPrice (BigDecimal, not null, precision 10 scale 2)
   - pricePerTicket (BigDecimal, not null, precision 10 scale 2)
   - totalAmount (BigDecimal, not null, precision 10 scale 2)
   - status (TicketStatus enum, @Enumerated STRING)
   - bookingDate (LocalDateTime, @CreationTimestamp)
   - createdAt (LocalDateTime, @CreationTimestamp)
   - updatedAt (LocalDateTime, @UpdateTimestamp)
   - eventDate (LocalDateTime, not null, @Future)
   - version (Long, @Version) ← optimistic locking

□ TicketStatus.java (enum): PENDING, CONFIRMED, CANCELLED, COMPLETED

□ TicketRepository.java:
   - findByUserId(Long) → List<Ticket>
   - findByStatus(TicketStatus) → List<Ticket>
   - findByEventName(String) → List<Ticket>
```

#### Bước 3.2 - Config + Exceptions (20 phút)
```
□ config/KafkaConfig.java: Producer + Consumer config
□ config/RedisConfig.java: @EnableCaching + CacheManager
□ config/EventPublisherConfig.java
□ config/OpenApiConfig.java
□ config/RestClientConfig.java: ← QUAN TRỌNG
   @Bean WebClient webClient() {
     return WebClient.builder().build();
   }

□ Exception classes (copy pattern từ user-service):
   - BusinessException, TechnicalException, ValidationException
   - TicketNotFoundException extends BusinessException
   - TicketAlreadyCancelledException extends BusinessException
   - InvalidTicketStatusException extends BusinessException
   - handler/GlobalExceptionHandler.java
□ dto/common/ApiErrorResponse.java
```

#### Bước 3.3 - Service Logic (1.5h) ← PHẦN CORE, LÀM KỸ
```
□ TicketService.java (interface): 8 methods

□ TicketServiceImpl.java (@Service):
   Inject: TicketRepository, EventPublisher, WebClient

   ★ createTicket(ticket):
     1. Validate: ticket != null
     2. Validate: userId, eventName, quantity, pricePerTicket != null
     3. Validate: quantity > 0, pricePerTicket > 0
     4. Set status = PENDING
     5. Tính totalAmount = pricePerTicket × quantity

     6. GỌI INVENTORY SERVICE (HTTP sync call):
        @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveSeatsFallback")
        @Retry(name = "inventory-service")
        reserveSeatsInInventory(eventName, quantity):
          webClient.post()
            .uri("http://inventory-service/inventory/event/{name}/reserve?quantity={qty}")
            .retrieve().bodyToMono(Boolean.class).block()

     7. Nếu reserve = false → throw BusinessException("Insufficient availability")
     8. Save ticket
     9. publishTicketCreatedEvent(savedTicket) → topic "ticket-created"

     Fallback: reserveSeatsFallback() → return false

   - getTicketById: @Cacheable(value="tickets", key="#id")
   - getAllTickets, getTicketsByUserId, getTicketsByStatus, getTicketsByEventName

   ★ updateTicketStatus(id, status):
     - Không cho thay đổi ticket đã CANCELLED (trừ khi set CANCELLED)
     - Save
     - Nếu status = CONFIRMED → publishTicketConfirmedEvent()

   ★ cancelTicket(id):
     - Check: chưa bị CANCELLED
     - releaseSeatsInInventory(eventName, quantity) ← HTTP call
     - Set status = CANCELLED
     - publishTicketCancelledEvent() → topic "ticket-cancelled"

   - deleteTicket: @CacheEvict

   Private helpers:
     - publishTicketCreatedEvent(ticket) → build TicketCreatedEvent → publish
     - publishTicketConfirmedEvent(ticket) → build TicketConfirmedEvent → publish
     - publishTicketCancelledEvent(ticket) → build TicketCancelledEvent → publish
     - reserveSeatsInInventory(eventName, qty) → WebClient POST
     - releaseSeatsInInventory(eventName, qty) → WebClient POST
```

#### Bước 3.4 - Controller + Quick Test (30 phút)
```
□ TicketController.java (@RestController, @RequestMapping("/tickets")):
   - POST   /tickets                → createTicket (201)
   - GET    /tickets/{id}           → getTicketById
   - GET    /tickets                → getAllTickets
   - GET    /tickets/user/{userId}  → getTicketsByUserId
   - GET    /tickets/status/{s}     → getTicketsByStatus
   - GET    /tickets/event/{name}   → getTicketsByEventName
   - PATCH  /tickets/{id}/status?status=X → updateTicketStatus
   - PATCH  /tickets/{id}/cancel    → cancelTicket
   - DELETE /tickets/{id}           → deleteTicket (204)

✅ TEST (cần inventory-service đang chạy):
   1. Chạy inventory-service + ticket-service
   2. Tạo inventory: POST localhost:8085/inventory → "Concert A", 100 seats
   3. POST localhost:8081/tickets → tạo ticket cho "Concert A", qty=2
      → Ticket Service GỌI Inventory Service → reserve 2 seats
      → Ticket saved (PENDING)
      → Event "ticket-created" publish lên Kafka
   4. GET localhost:8081/tickets/1 → thấy PENDING
   5. PATCH localhost:8081/tickets/1/cancel → seats released, CANCELLED
   6. GET localhost:8085/inventory/1 → availableSeats lại = 100
```

**GIT COMMIT: "feat: add ticket-service core with inventory integration + circuit breaker"**

---

### CHIỀU (2h): CQRS Pattern + Event Handlers

#### Bước 3.5 - CQRS Pattern (1h)
```
Tạo package cqrs/ - 14 files nhỏ:

□ cqrs/common/:
   - CommandResult<T>.java { boolean success, T data, String errorMessage }
   - QueryResult<T>.java { boolean success, T data, String errorMessage }

□ cqrs/commands/:
   - CreateTicketCommand.java { Long userId, String eventName, Integer quantity,
     BigDecimal pricePerTicket, LocalDateTime eventDate }
   - CancelTicketCommand.java { Long ticketId, String cancellationReason }
   - UpdateTicketStatusCommand.java { Long ticketId, TicketStatus newStatus }

□ cqrs/commands/handlers/:
   - CreateTicketCommandHandler.java (@Component):
     inject TicketService
     handle(command) → build Ticket → ticketService.createTicket() → return CommandResult<Long>

   - CancelTicketCommandHandler.java (@Component):
     handle(command) → ticketService.cancelTicket() → return CommandResult<Void>

   - UpdateTicketStatusCommandHandler.java (@Component):
     handle(command) → ticketService.updateTicketStatus() → return CommandResult<Void>

□ cqrs/queries/:
   - GetTicketQuery.java { Long ticketId }
   - GetTicketsByUserQuery.java { Long userId }
   - GetTicketsByStatusQuery.java { TicketStatus status }

□ cqrs/queries/handlers/:
   - GetTicketQueryHandler.java (@Component):
     handle(query) → ticketService.getTicketById() → return QueryResult<Ticket>

   - GetTicketsByUserQueryHandler.java (@Component):
     handle(query) → ticketService.getTicketsByUserId() → return QueryResult<List<Ticket>>

   - GetTicketsByStatusQueryHandler.java (@Component):
     handle(query) → ticketService.getTicketsByStatus() → return QueryResult<List<Ticket>>

Mẹo: Mỗi file siêu ngắn (~20-40 dòng). Command/Query = DTO có @Data @Builder.
Handler = @Component inject TicketService, có 1 method handle().
```

#### Bước 3.6 - Event Handlers (30 phút)
```
□ events/handlers/PaymentCompletedEventHandler.java:
   - implements EventHandler<PaymentCompletedEvent>
   - @KafkaListener(topics = "payment-completed", groupId = "ticket-service-group")
   - @Transactional
   - handle(event):
     1. Validate: event != null, ticketId != null
     2. ticketService.updateTicketStatus(event.getTicketId(), TicketStatus.CONFIRMED)
     3. Log: "Ticket {id} confirmed after successful payment"

□ events/handlers/PaymentFailedEventHandler.java:
   - implements EventHandler<PaymentFailedEvent>
   - @KafkaListener(topics = "payment-failed", groupId = "ticket-service-group")
   - handle(event): log warning, có thể cancel ticket

✅ TEST: Build mvn clean package → SUCCESS
   (Full flow test sẽ làm ngày 4 khi có Payment Service)
```

**GIT COMMIT: "feat: add CQRS pattern + payment event handlers to ticket-service"**

---

### TỐI (1h): Review ngày 3
```
□ Đọc lại code Ticket Service, hiểu kỹ:
   - Tại sao dùng WebClient thay vì RestTemplate? (reactive, non-blocking)
   - Circuit Breaker hoạt động thế nào? (nếu Inventory down → fallback)
   - @Version trong Ticket.java để làm gì? (optimistic locking)
   - CQRS tách Command/Query ra sao? (Command = write, Query = read)
□ Fix bug nếu có
```

---

## NGÀY 4: PAYMENT SERVICE + NOTIFICATION SERVICE
> Mục tiêu cuối ngày: FULL EVENT FLOW hoạt động end-to-end
> 📋 Tham chiếu BLUEPRINT: Section 10 (Payment), Section 12 (Notification), Section 19 (Kafka Topics)

---

### SÁNG (3h): Payment Service

#### Bước 4.1 - Khung + Model (30 phút)
```
□ Tạo folder payment-service/
□ pom.xml (web, jpa, mysql, kafka, consul, redis, actuator, openapi, lombok, common-event-library)
□ PaymentServiceApplication.java
□ application.yml (port 8083, paymentdb)

□ Payment.java (@Entity, table "payments", @EntityListeners AuditingEntityListener):
   - id (Long, IDENTITY)
   - ticketId (Long, not null)
   - userId (Long, not null)
   - amount (BigDecimal, not null, precision 10 scale 2)
   - paymentMethod (PaymentMethod enum, not null, max 20)
   - status (PaymentStatus enum, not null, max 20)
   - transactionId (String, unique, max 100)
   - createdAt (LocalDateTime, @CreatedDate)

□ PaymentStatus.java (enum): PENDING, SUCCESS, FAILED, REFUNDED
□ PaymentMethod.java (enum): CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, DIGITAL_WALLET

□ PaymentRepository.java:
   - findByTicketId(Long) → Optional<Payment>
   - findByTransactionId(String) → Optional<Payment>
   - findByUserId(Long) → List<Payment>
   - findByStatus(PaymentStatus) → List<Payment>
```

#### Bước 4.2 - DTOs + Mapper + Config (20 phút)
```
□ dto/request/CreatePaymentRequest.java: ticketId, userId, amount, paymentMethod
□ dto/response/PaymentResponse.java: all fields
□ dto/response/ApiResponse<T>.java (copy từ user-service)
□ dto/common/ApiErrorResponse.java (copy)
□ mapper/PaymentMapper.java: toEntity(), toResponse(), toResponseList()

□ config/KafkaConfig.java: Producer + Consumer (CẢ HAI!) ← khác user-service
   Consumer config cần: JsonDeserializer, trusted.packages="*",
   type.mapping: TicketCreatedEvent
□ config/EventPublisherConfig.java
□ config/JpaAuditingConfig.java
□ config/OpenApiConfig.java
□ Exception classes: BusinessException, PaymentNotFoundException, InsufficientFundsException,
   PaymentProcessingException, ValidationException, TechnicalException, GlobalExceptionHandler
```

#### Bước 4.3 - Service Logic + Event Handler (1h)
```
□ PaymentService.java (interface)

□ PaymentServiceImpl.java (@Service, @Transactional(readOnly=true)):
   Inject: PaymentRepository, PaymentMapper, EventPublisher

   - createPayment (@Transactional):
     1. Validate request + amount > 0
     2. paymentMapper.toEntity() → set PENDING → generate transactionId (UUID)
     3. Save → publishPaymentInitiatedEvent → return toResponse

   - getPaymentById, getPaymentByTicketId, getPaymentByTransactionId
   - getAllPayments, getPaymentsByUserId, getPaymentsByStatus

   ★ processPayment(paymentId) (@Transactional):
     1. Find payment, check status == PENDING
     2. executePaymentGateway(payment) → STUB: return true
     3. If success: status → SUCCESS → publishPaymentCompletedEvent
     4. If fail: status → FAILED → publishPaymentFailedEvent
     5. Save + return

   ★ refundPayment(paymentId) (@Transactional):
     1. Find payment, check status == SUCCESS
     2. status → REFUNDED → publishPaymentRefundedEvent
     3. Save + return

   - deletePayment (@Transactional)

□ events/handlers/TicketCreatedEventHandler.java: ← AUTO CREATE PAYMENT
   - implements EventHandler<TicketCreatedEvent>
   - @KafkaListener(topics = "ticket-created", groupId = "payment-service-group")
   - @Transactional
   - handle(event):
     1. Validate: ticketId, userId, totalAmount != null
     2. Tạo Payment: ticketId, userId, amount=totalAmount, CREDIT_CARD, PENDING
     3. Save vào DB
     4. Log: "Payment record created for ticket: {id}"
```

#### Bước 4.4 - Controller (15 phút)
```
□ PaymentController.java (@RestController, @RequestMapping("/payments")):
   - POST   /payments                         → createPayment (201)
   - GET    /payments/{id}                     → getPaymentById
   - GET    /payments/ticket/{ticketId}        → getPaymentByTicketId
   - GET    /payments/transaction/{txnId}      → getPaymentByTransactionId
   - GET    /payments                          → getAllPayments
   - GET    /payments/user/{userId}            → getPaymentsByUserId
   - GET    /payments/status/{status}          → getPaymentsByStatus
   - POST   /payments/{id}/process             → processPayment
   - POST   /payments/{id}/refund              → refundPayment
   - DELETE /payments/{id}                     → deletePayment (204)
```

#### Bước 4.5 - Test Payment + Ticket Flow (30 phút)
```
✅ TEST FLOW QUAN TRỌNG:
   1. Start: inventory-service (8085) + ticket-service (8081) + payment-service (8083)
   2. Tạo inventory: POST :8085/inventory → "Concert A", 100 seats
   3. Tạo ticket: POST :8081/tickets → userId=1, eventName="Concert A", qty=2
      → Kafka event "ticket-created" phát ra
      → Payment Service (consumer) TỰ ĐỘNG tạo payment record!
   4. GET :8083/payments/ticket/1 → thấy payment PENDING (auto-created!)
   5. POST :8083/payments/1/process → payment SUCCESS
      → Kafka event "payment-completed" phát ra
      → Ticket Service (consumer) update ticket → CONFIRMED
   6. GET :8081/tickets/1 → status = CONFIRMED

   🎉 EVENT-DRIVEN FLOW HOẠT ĐỘNG!
   Ticket Created → Auto Payment → Process Payment → Auto Confirm Ticket
```

**GIT COMMIT: "feat: add payment-service with auto payment creation + event flow"**

---

### CHIỀU (2.5h): Notification Service

#### Bước 4.6 - Notification Service (1.5h)
```
□ Tạo folder notification-service/
□ pom.xml (web, jpa, mysql, kafka, consul, redis, actuator, lombok, common-event-library)
□ NotificationServiceApplication.java
□ application.yml (port 8084, notificationdb)

□ Notification.java (@Entity, table "notifications"):
   - id (Long, IDENTITY)
   - userId (Long, not null, @NotNull)
   - message (String, not null, max 1000, @NotBlank)
   - type (NotificationType enum, @Enumerated STRING)
   - status (NotificationStatus enum, @Enumerated STRING)
   - sentAt (LocalDateTime, @CreationTimestamp)

□ NotificationType.java (enum): EMAIL, SMS, PUSH
□ NotificationStatus.java (enum): PENDING, SENT, FAILED

□ NotificationRepository.java:
   - findByUserId(Long), findByStatus(NotificationStatus), findByType(NotificationType)

□ NotificationService.java (interface)

□ NotificationServiceImpl.java (@Service):
   - createNotification, getNotificationById, getAllNotifications
   - getNotificationsByUserId, getNotificationsByStatus, getNotificationsByType
   - sendNotification: executeNotificationSending() → STUB return true → status = SENT
   - deleteNotification

   ★ 3 KAFKA LISTENERS:

   @KafkaListener(topics = "user-created", groupId = "notification-service-group")
   handleUserCreated(UserCreatedEvent event):
     → Tạo Notification(userId, "Welcome! Your account has been created", EMAIL, PENDING)
     → createNotification() → sendNotification()

   @KafkaListener(topics = "ticket-created", groupId = "notification-service-group")
   handleTicketCreated(TicketCreatedEvent event):
     → Tạo Notification(userId, "Your ticket has been booked successfully", EMAIL, PENDING)
     → createNotification() → sendNotification()

   @KafkaListener(topics = "payment-completed", groupId = "notification-service-group")
   handlePaymentCompleted(PaymentCompletedEvent event):
     → Tạo Notification(userId, "Payment processed successfully. Thank you!", EMAIL, PENDING)
     → createNotification() → sendNotification()

□ config/KafkaConfig.java (consumer config, trusted.packages="*")
□ NotificationController.java, exception classes (copy pattern)
```

#### Bước 4.7 - TEST FULL END-TO-END FLOW (30 phút)
```
✅ FULL SYSTEM TEST - Đây là khoảnh khắc quan trọng nhất!

Start 4 services: inventory (8085), ticket (8081), payment (8083), notification (8084)
+ user-service (8082)

FLOW 1 - Đăng ký user:
   POST :8082/users → tạo user → event "user-created"
   → Notification Service: tạo "Welcome!" notification
   CHECK: GET :8084/notifications/user/1 → 1 notification (Welcome)

FLOW 2 - Đặt vé:
   POST :8085/inventory → tạo "Concert A" 100 seats
   POST :8081/tickets → đặt 2 vé → event "ticket-created"
   → Payment Service: auto tạo payment (PENDING)
   → Notification Service: tạo "Ticket booked" notification
   CHECK: GET :8083/payments/ticket/1 → payment PENDING
   CHECK: GET :8084/notifications/user/1 → 2 notifications

FLOW 3 - Thanh toán:
   POST :8083/payments/1/process → SUCCESS → event "payment-completed"
   → Ticket Service: ticket PENDING → CONFIRMED
   → Notification Service: tạo "Payment success" notification
   CHECK: GET :8081/tickets/1 → status = CONFIRMED
   CHECK: GET :8084/notifications/user/1 → 3 notifications

FLOW 4 - Huỷ vé:
   PATCH :8081/tickets/1/cancel → CANCELLED, seats released
   CHECK: GET :8085/inventory/1 → availableSeats lại = 100

🎉🎉🎉 TOÀN BỘ EVENT-DRIVEN ARCHITECTURE HOẠT ĐỘNG! 🎉🎉🎉
```

**GIT COMMIT: "feat: add notification-service with 3 kafka listeners - full event flow working"**

---

## NGÀY 5: SAGA ORCHESTRATOR + API GATEWAY
> Mục tiêu cuối ngày: Distributed transaction + Single entry point cho toàn hệ thống
> 📋 Tham chiếu BLUEPRINT: Section 13 (Saga Orchestrator), Section 7 (API Gateway)

---

### SÁNG (3h): Saga Orchestrator Service

#### Bước 5.1 - Models + Repository (30 phút)
```
□ Tạo folder saga-orchestrator-service/
□ pom.xml (web, jpa, mysql, kafka, consul, actuator, lombok, common-event-library)
□ SagaOrchestratorApplication.java: @SpringBootApplication + @EnableKafka
□ application.yml (port 8086, sagadb - JPA tự tạo DB nhờ createDatabaseIfNotExist=true)

□ SagaStatus.java (enum): STARTED, IN_PROGRESS, COMPLETED, COMPENSATING, COMPENSATED, FAILED
□ StepStatus.java (enum): IN_PROGRESS, COMPLETED, FAILED, COMPENSATED

□ SagaInstance.java (@Entity, table "saga_instances"):
   - sagaId (String, @Id) ← dùng UUID, KHÔNG auto-generate
   - ticketId (Long)
   - status (SagaStatus, @Enumerated STRING)
   - startedAt, completedAt (LocalDateTime)
   - compensationReason (String)
   - steps: @OneToMany(mappedBy="saga", cascade=ALL, orphanRemoval=true) @Builder.Default
   - addStep(SagaStep step): steps.add(step); step.setSaga(this);

□ SagaStep.java (@Entity, table "saga_steps"):
   - id (Long, @GeneratedValue IDENTITY)
   - saga: @ManyToOne(LAZY) @JoinColumn("saga_id") → SagaInstance
   - stepName (String)
   - status (StepStatus, @Enumerated STRING)
   - compensationAction (String)
   - executedAt, compensatedAt (LocalDateTime)
   - errorMessage (String)

□ SagaInstanceRepository.java (extends JpaRepository<SagaInstance, String>)

□ config/KafkaConfig.java, config/EventPublisherConfig.java
□ exception/BusinessException.java, SagaNotFoundException.java, GlobalExceptionHandler.java
□ dto/common/ApiErrorResponse.java
```

#### Bước 5.2 - TicketBookingSaga (1h) ← SAGA PATTERN
```
□ orchestrator/TicketBookingSaga.java (@Component):
   Inject: SagaInstanceRepository, EventPublisher, CompensationHandler

   @KafkaListener(topics = "ticket-created", groupId = "saga-orchestrator-group")
   @Transactional
   onTicketCreated(TicketCreatedEvent event):
     1. Validate: event != null, ticketId != null
     2. Tạo SagaInstance:
        sagaId = UUID.randomUUID().toString()
        ticketId = event.getTicketId()
        status = STARTED
        startedAt = LocalDateTime.now()
     3. sagaRepository.save(saga)
     4. try {
          executeStep(saga, "inventory-reservation", () -> true)   // stub
          executeStep(saga, "payment-initiation", () -> true)      // stub
          executeStep(saga, "notification-sending", () -> true)    // stub
          saga.setStatus(IN_PROGRESS)
          sagaRepository.save(saga)
        } catch (Exception e) {
          compensate(saga, e.getMessage())
        }

   private executeStep(SagaInstance saga, String stepName, StepExecutor executor):
     1. SagaStep step = SagaStep.builder()
          .stepName(stepName).status(IN_PROGRESS).executedAt(now()).build()
     2. saga.addStep(step)
     3. boolean success = executor.execute()
     4. if success → step.status = COMPLETED
     5. else → step.status = FAILED → throw BusinessException

   private compensate(SagaInstance saga, String reason):
     1. saga.status = COMPENSATING, saga.compensationReason = reason
     2. sagaRepository.save(saga)
     3. try { compensationHandler.compensate(saga) → saga.status = COMPENSATED }
        catch → saga.status = FAILED
     4. saga.completedAt = now()
     5. sagaRepository.save(saga)

   @FunctionalInterface
   private interface StepExecutor { boolean execute(); }
```

#### Bước 5.3 - CompensationHandler (30 phút)
```
□ compensation/CompensationHandler.java (@Component):
   Inject: EventPublisher

   compensate(SagaInstance saga):
     1. saga.getSteps().stream()
          .filter(step -> status == COMPLETED)
          .sorted(Comparator.comparing(SagaStep::getExecutedAt).reversed())  ← THỨ TỰ NGƯỢC
          .forEach(this::executeCompensation)

   private executeCompensation(SagaStep step):
     try {
       switch (step.getStepName()):
         "inventory-reservation" → log "Releasing inventory for ticket: {id}" (stub)
         "payment-initiation"   → log "Cancelling payment for ticket: {id}" (stub)
         "notification-sending" → log "No compensation needed" (skip)
       step.status = COMPENSATED
       step.compensatedAt = now()
     } catch → step.errorMessage = "Compensation failed: " + e.getMessage()
```

#### Bước 5.4 - Test Saga (30 phút)
```
✅ TEST:
   1. Start saga-orchestrator-service (8086) + các services khác
   2. Tạo ticket: POST :8081/tickets
      → Event "ticket-created" → Saga Orchestrator lắng nghe
   3. Xem log Saga Orchestrator:
      "Starting ticket booking saga for ticket: 1"
      "Step inventory-reservation: COMPLETED"
      "Step payment-initiation: COMPLETED"
      "Step notification-sending: COMPLETED"
      "Saga in progress for ticket: 1"
   4. Kiểm tra DB sagadb:
      - saga_instances: 1 record, status = IN_PROGRESS
      - saga_steps: 3 records, all COMPLETED
```

**GIT COMMIT: "feat: add saga-orchestrator with ticket booking saga + compensation"**

---

### CHIỀU (2.5h): API Gateway

#### Bước 5.5 - API Gateway (1.5h)
```
□ Tạo folder api-gateway/
□ pom.xml - ⚠️ DEPENDENCIES KHÁC BIỆT (đây là WebFlux app, KHÔNG phải MVC):
   - spring-cloud-starter-gateway (thay vì spring-boot-starter-web!)
   - spring-cloud-starter-consul-discovery
   - spring-boot-starter-data-redis-reactive (reactive!)
   - spring-cloud-starter-circuitbreaker-reactor-resilience4j
   - springdoc-openapi-starter-webflux-ui (2.3.0, WebFlux version!)
   - micrometer-registry-prometheus
   - spring-boot-starter-actuator, lombok, spring-boot-starter-test
   - KHÔNG CẦN common-event-library

□ ApiGatewayApplication.java: @SpringBootApplication + @EnableDiscoveryClient

□ application.yml:
   server.port: 8080
   spring.data.redis: host=redis, port=6379
   spring.cloud.consul: host=consul, port=8500
   spring.cloud.gateway:
     discovery.locator.enabled: true
     routes:
       - id: ticket-service,   uri: lb://ticket-service,       Path=/tickets/**
       - id: user-service,     uri: lb://user-service,         Path=/users/**
       - id: payment-service,  uri: lb://payment-service,      Path=/payments/**
       - id: notification-service, uri: lb://notification-service, Path=/notifications/**
       - id: inventory-service, uri: lb://inventory-service,    Path=/inventory/**
   management.endpoints: health, info

□ config/GatewayConfig.java (@Configuration):
   @Bean RouteLocator customRouteLocator(RouteLocatorBuilder builder):
     5 routes: users, tickets, payments, notifications, inventory
     Mỗi route: .path("/{service}/**")
       .filters(f -> f.stripPrefix(0).addRequestHeader("X-Gateway-Request", "API-Gateway"))
       .uri("lb://{service}")

   @Bean CorsWebFilter corsWebFilter():
     - AllowedOrigins: *
     - AllowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
     - AllowedHeaders: *
     - MaxAge: 3600

□ exception/GlobalErrorAttributes.java (extends DefaultErrorAttributes):
   - Override getErrorAttributes: thêm success=false, errorCode, timestamp

□ exception/ApiErrorResponse.java
```

#### Bước 5.6 - Test API Gateway + FINAL TEST (1h)
```
✅ TEST API GATEWAY:
   1. Start API Gateway (8080) + tất cả services
   2. Consul: http://localhost:8500 → thấy 7 services registered:
      api-gateway, user-service, ticket-service, payment-service,
      notification-service, inventory-service, saga-orchestrator-service
   3. Test routing qua Gateway (port 8080):
      GET  http://localhost:8080/users         → user-service:8082
      POST http://localhost:8080/tickets       → ticket-service:8081
      GET  http://localhost:8080/payments      → payment-service:8083
      GET  http://localhost:8080/notifications → notification-service:8084
      GET  http://localhost:8080/inventory     → inventory-service:8085
   4. Health: http://localhost:8080/actuator/health

✅ FINAL END-TO-END TEST (qua API Gateway):
   Bây giờ tất cả requests đi qua 1 URL duy nhất: http://localhost:8080

   1. POST :8080/users → tạo user
      → Kafka "user-created" → Notification: "Welcome!"

   2. POST :8080/inventory → tạo "Concert A", 100 seats

   3. POST :8080/tickets → đặt 2 vé
      → Inventory: reserve 2 seats (98 còn)
      → Kafka "ticket-created":
        → Payment Service: auto tạo payment (PENDING)
        → Notification Service: "Ticket booked"
        → Saga Orchestrator: start saga, 3 steps COMPLETED

   4. POST :8080/payments/1/process → thanh toán
      → Kafka "payment-completed":
        → Ticket Service: PENDING → CONFIRMED
        → Notification Service: "Payment success"

   5. GET :8080/tickets/1        → CONFIRMED
      GET :8080/payments/1       → SUCCESS
      GET :8080/notifications/user/1 → 3 notifications
      GET :8080/inventory/1      → 98 available seats

   6. PATCH :8080/tickets/1/cancel → huỷ vé
      → Inventory: release seats → 100

   ✨ HOÀN THÀNH! Toàn bộ hệ thống microservices hoạt động qua API Gateway!
```

**GIT COMMIT: "feat: add api-gateway - complete microservices platform"**

---

## TỔNG KẾT

```
╔══════════╦══════════════════════════════════════════════════╦════════════════════╗
║  NGÀY    ║  NỘI DUNG                                       ║  SẢN PHẨM          ║
╠══════════╬══════════════════════════════════════════════════╬════════════════════╣
║  Ngày 1  ║  Parent POM + Docker Infra                      ║  Library build OK  ║
║          ║  + Common Event Library (23 files)               ║  Infra chạy        ║
╠══════════╬══════════════════════════════════════════════════╬════════════════════╣
║  Ngày 2  ║  User Service (CRUD, cache, events)             ║  2 services chạy   ║
║          ║  + Inventory Service (distributed lock)          ║  độc lập           ║
╠══════════╬══════════════════════════════════════════════════╬════════════════════╣
║  Ngày 3  ║  Ticket Service                                 ║  3 services        ║
║          ║  (CQRS + Circuit Breaker + HTTP call Inventory)  ║  giao tiếp HTTP    ║
╠══════════╬══════════════════════════════════════════════════╬════════════════════╣
║  Ngày 4  ║  Payment Service + Notification Service         ║  FULL EVENT FLOW   ║
║          ║  (Kafka consumers, auto-create, 3 listeners)    ║  hoạt động!        ║
╠══════════╬══════════════════════════════════════════════════╬════════════════════╣
║  Ngày 5  ║  Saga Orchestrator + API Gateway                ║  HOÀN THÀNH CORE   ║
║          ║  (Saga pattern + Single entry point)            ║  7 services!       ║
╚══════════╩══════════════════════════════════════════════════╩════════════════════╝
```

### Sau 5 ngày, bạn đã có:
```
✅ 1 Common Event Library (23+ files)
✅ 6 Microservices (User, Ticket, Payment, Inventory, Notification, Saga)
✅ 1 API Gateway
✅ Event-Driven Architecture qua Kafka
✅ CQRS Pattern (Ticket Service)
✅ Saga Pattern (Saga Orchestrator)
✅ Distributed Locking (Inventory - Redisson)
✅ Circuit Breaker (Ticket → Inventory)
✅ Redis Caching
✅ Service Discovery (Consul)
✅ API Documentation (Swagger)
```

### Sẽ làm SAU (Phase 2):
```
⬜ Dockerfiles cho tất cả services
⬜ NGINX reverse proxy + SSL + rate limiting
⬜ Prometheus + Grafana monitoring
⬜ Kubernetes manifests
⬜ CI/CD pipelines (GitHub Actions)
⬜ Test configs + Unit tests
⬜ README.md documentation
```

---

### Tips:

1. **User Service là template.** Làm kỹ ngày 2, sau đó copy pattern cho service khác.
2. **Test ngay sau mỗi bước.** Đừng đợi cuối ngày.
3. **Exception handling:** Copy nguyên bộ từ user-service, chỉ đổi tên class.
4. **Nếu stuck 30 phút** → skip, ghi note, hỏi AI, tiếp tục bước sau.
5. **Commit thường xuyên.** Mỗi feature xong = 1 commit.
6. **Ngày 4 là ngày WOW** - lần đầu thấy full event flow tự động chạy.
