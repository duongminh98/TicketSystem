package com.heditra.events.infrastructure;

import com.heditra.events.core.DomainEvent;
import com.heditra.events.core.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public <T extends DomainEvent> CompletableFuture<Void> publish(T event) {
        String topic = deriveTopicFromEvent(event);
        return publish(topic, event);
    }

    @Override
    public <T extends DomainEvent> CompletableFuture<Void> publish(String topic, T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getAggregateId() == null) {
            throw new IllegalArgumentException("Event aggregateId cannot be null");
        }

        log.info("Publishing event {} to topic {}", event.getEventType(), topic);

        CompletableFuture<Void> future = new CompletableFuture<>();

        kafkaTemplate.send(topic, event.getAggregateId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Event {} published successfully to topic {}",
                                event.getEventType(), topic);
                        future.complete(null);
                    } else {
                        log.error("Failed to publish event {} to topic {}: {}",
                                event.getEventType(), topic, ex.getMessage());
                        future.completeExceptionally(ex);
                    }
                });

        return future;
    }

    /**
     * "TicketCreated" → "ticket-created" (camelCase → kebab-case)
     */
    private <T extends DomainEvent> String deriveTopicFromEvent(T event) {
        String eventType = event.getEventType();
        return eventType.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
