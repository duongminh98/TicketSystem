package com.heditra.events.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Xử lý event thất bại (Dead Letter Queue).
 * Kafka tự gửi event vào DLQ khi consumer xử lý thất bại quá số lần retry.
 * Production nên lưu vào DB hoặc gửi alert thay vì chỉ log.
 */
@Slf4j
@Component
public class DeadLetterQueueHandler {

    @KafkaListener(topics = "${event.dlq.topic:event-dlq}", groupId = "dlq-handler-group")
    public void handleDeadLetterEvent(String message, Acknowledgment acknowledgment) {
        log.error("Received dead letter event: {}", message);
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }
}
