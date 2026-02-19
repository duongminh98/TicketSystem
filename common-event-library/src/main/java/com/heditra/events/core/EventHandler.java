package com.heditra.events.core;

/**
 * Interface xử lý event. Mỗi handler đăng ký xử lý 1 loại event cụ thể.
 * KafkaEventListener dùng getEventType() để dispatch event đến đúng handler.
 */
public interface EventHandler<T extends DomainEvent> {

    void handle(T event);

    Class<T> getEventType();
}
