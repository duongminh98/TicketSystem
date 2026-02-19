package com.heditra.events.core;

import java.util.concurrent.CompletableFuture;

/**
 * Interface publish event lên message broker (Kafka).
 * Có 2 method: tự suy topic từ event type, hoặc chỉ định topic cụ thể.
 */
public interface EventPublisher {

    <T extends DomainEvent> CompletableFuture<Void> publish(T event);

    <T extends DomainEvent> CompletableFuture<Void> publish(String topic, T event);
}
