# TIẾN ĐỘ DỰ ÁN - Event-Driven Ticketing Platform

> **MỤC ĐÍCH FILE NÀY:** Khi AI hết token và phải bắt đầu session mới, đọc file này
> để biết chính xác đã làm gì, đang ở đâu, cần làm gì tiếp.
>
> **CÁCH DÙNG:** AI mới đọc file này + PROJECT_BLUEPRINT.md + 5_DAY_PLAN.md rồi tiếp tục
> từ bước tiếp theo (bước đầu tiên có trạng thái CHUA LAM).

> Cập nhật lần cuối: 2026-02-19

---

## THÔNG TIN MÔI TRƯỜNG

| Thông tin | Giá trị |
|-----------|---------|
| OS | Windows 11 (win32 10.0.26200) |
| Shell | PowerShell |
| Workspace | `d:\TicketSystem` |
| Java | 21.0.9 (Oracle, `C:\Program Files\Java\jdk-21`) |
| Maven | 3.9.12 (`D:\tools\apache-maven-3.9.12`) |
| Maven PATH | **CHUA co trong System PATH** - moi session can chay: `$env:Path = "D:\tools\apache-maven-3.9.12\bin;" + $env:Path` |
| Docker | 28.5.1 (Docker Desktop) |
| Git repo | CHUA khoi tao (chua co .git) |

---

## CẤU TRÚC HIỆN TẠI CỦA PROJECT

```
d:\TicketSystem\
├── pom.xml                          ← Parent POM (DA TAO - day du)
├── docker-compose.yml                ← DA TAO - 5 infra containers
├── init-db.sql                      ← DA TAO - tao 5 databases
├── .gitignore                       ← DA TAO
├── .env.example                     ← DA TAO
├── AI_PROMPT.md                     ← Huong dan cho AI
├── PROJECT_BLUEPRINT.md             ← Tai lieu tham chieu ky thuat
├── 5_DAY_PLAN.md                    ← Ke hoach 5 ngay
├── PROGRESS.md                      ← File nay
│
├── common-event-library\
│   └── pom.xml                      ← PLACEHOLDER (chua co code)
├── api-gateway\
│   └── pom.xml                      ← PLACEHOLDER
├── ticket-service\
│   └── pom.xml                      ← PLACEHOLDER
├── user-service\
│   └── pom.xml                      ← PLACEHOLDER
├── payment-service\
│   └── pom.xml                      ← PLACEHOLDER
├── notification-service\
│   └── pom.xml                      ← PLACEHOLDER
├── inventory-service\
│   └── pom.xml                      ← PLACEHOLDER
└── saga-orchestrator-service\
    └── pom.xml                      ← PLACEHOLDER
```

**Lưu ý:** Tất cả module pom.xml hiện tại là PLACEHOLDER tối thiểu (chỉ có groupId, artifactId, version, java 21). Sẽ được thay thế bằng pom.xml đầy đủ khi làm đến bước tương ứng.

---

## BƯỚC TIẾP THEO CẦN LÀM

> **=> NGAY 2, BUOC 2.1 - User Service: Khung + Model + Repository**
> Tham chiếu: PROJECT_BLUEPRINT.md Section 8
> Xem 5_DAY_PLAN.md dòng 222-261
> Theo plan: GIT COMMIT truoc khi bat dau Ngay 2:
>   Commit 1: "feat: init project structure + docker infrastructure"
>   Commit 2: "feat: add common-event-library with core events + kafka infrastructure"

---

## CHI TIẾT TỪNG BƯỚC ĐÃ LÀM

### NGÀY 1: NỀN TẢNG + COMMON EVENT LIBRARY

---

#### Bước 1.1 - Tạo Parent POM ✅ DONE
- **Thời gian:** 2026-02-19
- **File đã tạo/sửa:**
  - `d:\TicketSystem\pom.xml` - Parent POM đầy đủ
  - 8 folder modules + placeholder pom.xml
- **Nội dung Parent POM:**
  - parent: `spring-boot-starter-parent:3.2.0`
  - groupId: `com.heditra`, artifactId: `ticketing-system`, version: `1.0.0`, packaging: `pom`
  - Properties: java 21, spring-cloud `2023.0.0`, checkstyle `3.3.1`, jacoco `0.8.11`, sonar `3.10.0.2594`
  - 8 modules khai báo
  - pluginManagement: spring-boot-maven-plugin (exclude lombok), maven-checkstyle-plugin, jacoco-maven-plugin, sonar-maven-plugin
- **Việc khác đã làm:**
  - Cài Maven 3.9.12 vào `D:\tools\apache-maven-3.9.12`
  - Xoá folder `TicketSystem/TicketSystem` (project IntelliJ mặc định, không liên quan)
- **Test:** `mvn validate` → BUILD SUCCESS (9/9 modules)

---

#### Bước 1.2 - Docker Compose Infrastructure ✅ DONE
- **Thời gian:** 2026-02-19
- **File đã tạo:**
  - `docker-compose.yml` - 5 infrastructure containers (consul, zookeeper, kafka, mysql, redis)
  - `init-db.sql` - Tạo 5 databases: ticketdb, userdb, paymentdb, notificationdb, inventorydb
  - `.gitignore` - Ignore rules cho Maven, IDE, OS, Docker, logs, env
  - `.env.example` - Template biến môi trường (MySQL, Redis, Kafka, Consul, ports, JWT, etc.)
- **Việc khác đã làm:**
  - Tắt MySQL local (service mysql80) đang chiếm port 3306 bằng `net stop mysql80` (cần Run as Admin)
  - Docker Desktop version 28.5.1
- **Test:**
  - `docker-compose up -d` → 5/5 containers healthy
  - MySQL: SHOW DATABASES → thấy 5 databases (ticketdb, userdb, paymentdb, notificationdb, inventorydb)
  - Redis: `redis-cli ping` → PONG
  - Consul: healthy (UI http://localhost:8500)
  - Kafka + Zookeeper: healthy
- **LƯU Ý:** Mỗi lần mở máy cần:
  1. Mở Docker Desktop
  2. Tắt MySQL local nếu đang chạy: `Start-Process -FilePath "net" -ArgumentList "stop mysql80" -Verb RunAs -Wait`
  3. `docker-compose up -d` trong `d:\TicketSystem`

#### Bước 1.3 + 1.4 + 1.5 - Common Event Library ✅ DONE
- **Thời gian:** 2026-02-19
- **pom.xml đầy đủ:** spring-kafka, spring-boot-starter, jackson-databind, jackson-datatype-jsr310, lombok, slf4j-api, jakarta.validation-api. KHÔNG có spring-boot-maven-plugin (library, không phải app). Build plugin: maven-compiler-plugin (java 21 + lombok annotation processor)
- **23 Java files tạo trong** `common-event-library/src/main/java/com/heditra/events/`:
  - `core/` (6 files): DomainEvent (abstract, @JsonTypeInfo(Id.CLASS)), EventPublisher (interface), EventHandler (interface), EventEnvelope, EventMetadata, EventProcessingException
  - `infrastructure/` (4 files): EventKafkaConfig (@Configuration, producer: acks=all, retries=3, idempotence=true), KafkaEventPublisher (deriveTopicFromEvent: camelCase→kebab-case), KafkaEventListener (dispatch theo event class), DeadLetterQueueHandler (topic "event-dlq")
  - `ticket/` (3 files): TicketCreatedEvent, TicketConfirmedEvent, TicketCancelledEvent
  - `payment/` (4 files): PaymentInitiatedEvent, PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent
  - `user/` (3 files): UserCreatedEvent, UserUpdatedEvent, UserDeletedEvent
  - `inventory/` (2 files): InventoryReservedEvent, InventoryReleasedEvent
  - `notification/` (1 file): NotificationSentEvent
- **Bug đã fix:** KafkaEventPublisher - CompletableFuture<Object> không cast được sang CompletableFuture<Void>. Fix: dùng whenComplete() + manual future.complete()
- **Test:** `mvn clean install` → BUILD SUCCESS, JAR installed vào `~/.m2/repository/com/heditra/common-event-library/1.0.0/`
- **Còn thiếu:** Package `sourcing/` (AggregateRoot, EventStore, KafkaEventStore, EventReplayer, EventSourcingRepository) - chưa cần vì không service nào dùng Event Sourcing thực sự. Sẽ làm ở Phase 5 (Polish)

---

### NGÀY 2: USER SERVICE + INVENTORY SERVICE

#### Bước 2.1 ~ 2.5 - User Service ⏳ CHUA LAM
#### Bước 2.6 ~ 2.9 - Inventory Service ⏳ CHUA LAM

### NGÀY 3: TICKET SERVICE
#### Bước 3.1 ~ 3.6 ⏳ CHUA LAM

### NGÀY 4: PAYMENT SERVICE + NOTIFICATION SERVICE
#### Bước 4.1 ~ 4.7 ⏳ CHUA LAM

### NGÀY 5: SAGA ORCHESTRATOR + API GATEWAY
#### Bước 5.1 ~ 5.6 ⏳ CHUA LAM

### PHASE 5 (SAU KHI XONG CORE): POLISH
#### Event Sourcing Package ⏳ CHUA LAM
> Tạo `common-event-library/src/main/java/com/heditra/events/sourcing/`:
> - AggregateRoot.java (base class: aggregateId, version, uncommittedEvents, applyEvent(), loadFromHistory())
> - EventStore.java (interface: save, saveAll, getEvents, getEventsByType)
> - KafkaEventStore.java (implement bằng Kafka + in-memory cache)
> - EventReplayer.java (replay events to rebuild state)
> - EventSourcingRepository.java (save/load aggregate qua reflection)
> Sau khi tạo xong: rebuild common-event-library (`mvn clean install`)

---

## GIT COMMITS

| # | Commit Message | Bước | Ngày |
|---|---------------|------|------|
| - | (chua co commit nao - chua init git repo) | - | - |

---

## VẤN ĐỀ ĐÃ GẶP & CÁCH GIẢI QUYẾT

| # | Vấn đề | Cách giải quyết |
|---|--------|----------------|
| 1 | Maven chua cai | Tai Maven 3.9.12, giai nen vao `D:\tools\apache-maven-3.9.12`. Moi session PowerShell can set PATH: `$env:Path = "D:\tools\apache-maven-3.9.12\bin;" + $env:Path` |
| 2 | `mvn validate` bao loi module not exist | Tao folder + placeholder pom.xml cho 8 modules |
| 3 | Folder `TicketSystem/TicketSystem` thua | Project IntelliJ mac dinh, da xoa |
| 4 | MySQL local (service mysql80) chiem port 3306 | Tat bang `Start-Process -FilePath "net" -ArgumentList "stop mysql80" -Verb RunAs -Wait`. Can lam moi lan khoi dong may neu MySQL local tu bat |
