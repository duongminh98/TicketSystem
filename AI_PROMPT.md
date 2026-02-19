# PROMPT - Hướng Dẫn AI Code Lại Dự Án Từ Đầu

> Copy toàn bộ nội dung bên dưới và paste vào conversation mới với AI.
> Đảm bảo đã có 2 file PROJECT_BLUEPRINT.md và 5_DAY_PLAN.md trong folder.

---

## PROMPT BẮT ĐẦU (Paste cái này vào AI)

```
Mình có 2 file trong folder này:

1. PROJECT_BLUEPRINT.md - Tài liệu tham chiếu kỹ thuật đầy đủ cho một dự án 
   Event-Driven Ticketing Platform (Java 21, Spring Boot 3.2, Microservices, Kafka, 
   Redis, MySQL, Consul, CQRS, Saga Pattern).

2. 5_DAY_PLAN.md - Kế hoạch thực hiện chia theo 5 ngày, từng bước cụ thể có 
   checkpoint test.

Mục tiêu: Mình muốn CODE LẠI toàn bộ dự án này TỪ ĐẦU để học và cho vào CV.

Yêu cầu khi làm việc cùng mình:

1. ĐỌC KỸ cả 2 file trước khi bắt đầu.
2. Làm theo THỨ TỰ trong 5_DAY_PLAN.md (đừng nhảy bước).
3. Khi code, tra cứu chi tiết kỹ thuật từ PROJECT_BLUEPRINT.md 
   (application.yml, dependencies, logic, entity fields, API endpoints, v.v.).
4. Mỗi bước xong → chạy TEST theo checkpoint trong plan.
5. Mỗi feature xong → nhắc mình GIT COMMIT.
6. Giải thích ngắn gọn MỤC ĐÍCH của code khi viết (để mình hiểu, không chỉ copy).
7. Nếu có lỗi → debug và giải thích nguyên nhân.

Bắt đầu: Đọc 2 file, sau đó giúp mình làm NGÀY 1, BƯỚC 1.1 - Tạo Parent POM.
```

---

## PROMPT CHO CÁC NGÀY TIẾP THEO

### Ngày 1 (nếu chưa xong hết):
```
Tiếp tục Ngày 1. Bước tiếp theo là [số bước]. Đọc lại 5_DAY_PLAN.md để xem chi tiết.
```

### Ngày 2:
```
Bắt đầu Ngày 2 - User Service + Inventory Service.
Tham chiếu BLUEPRINT Section 8 (User Service) và Section 11 (Inventory Service).
Bắt đầu từ Bước 2.1.
```

### Ngày 3:
```
Bắt đầu Ngày 3 - Ticket Service (phức tạp nhất, có CQRS + Circuit Breaker).
Tham chiếu BLUEPRINT Section 9. Đọc kỹ 9.3, 9.5, 9.6 trước khi code.
Bắt đầu từ Bước 3.1.
```

### Ngày 4:
```
Bắt đầu Ngày 4 - Payment Service + Notification Service.
Hôm nay là ngày kết nối event flow end-to-end.
Tham chiếu BLUEPRINT Section 10, 12, 19.
Bắt đầu từ Bước 4.1.
```

### Ngày 5:
```
Bắt đầu Ngày 5 - Saga Orchestrator + API Gateway.
Hôm nay hoàn thành toàn bộ hệ thống.
Tham chiếu BLUEPRINT Section 13, 7.
Bắt đầu từ Bước 5.1.
```

---

## PROMPT KHI GẶP VẤN ĐỀ

### Bị lỗi build:
```
Mình bị lỗi khi [build/run/test] [tên service]. Đây là error:
[paste error log]
Kiểm tra lại theo BLUEPRINT xem mình thiếu gì.
```

### Không hiểu logic:
```
Giải thích cho mình tại sao [phần code] lại viết như vậy. 
Nó hoạt động thế nào trong kiến trúc tổng thể?
```

### Muốn skip bước:
```
Mình muốn skip [bước X] và quay lại sau. Đánh dấu TODO và tiếp tục bước tiếp theo.
```

### Muốn review:
```
Review lại toàn bộ code mình đã viết hôm nay. 
So sánh với BLUEPRINT xem có thiếu gì không.
```

---

## PROMPT KHI XONG 5 NGÀY

```
Mình đã hoàn thành 5 ngày core development. Bây giờ:
1. Review toàn bộ project, kiểm tra xem có service nào chưa kết nối đúng không.
2. Chạy full integration test (tạo user → book ticket → payment → notification).
3. Liệt kê những gì cần làm tiếp (Phase 4-5 trong BLUEPRINT: Docker, NGINX, K8s, CI/CD).
```
