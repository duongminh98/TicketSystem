package com.heditra.events.infrastructure;

import com.heditra.events.core.DomainEvent;
import com.heditra.events.core.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatch event đến đúng EventHandler dựa trên event class type.
 * Mỗi service đăng ký các handler cụ thể, listener sẽ tự route.
 */
@Slf4j
@Component
public class KafkaEventListener {

    private final Map<Class<?>, EventHandler<?>> handlers = new ConcurrentHashMap<>();

    public KafkaEventListener(List<EventHandler<?>> eventHandlers) {
        for (EventHandler<?> handler : eventHandlers) {
            handlers.put(handler.getEventType(), handler);
            log.info("Registered event handler for: {}", handler.getEventType().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void processEvent(T event) {
        EventHandler<T> handler = (EventHandler<T>) handlers.get(event.getClass());
        if (handler != null) {
            log.info("Processing event: {} with handler: {}",
                    event.getEventType(), handler.getClass().getSimpleName());
            handler.handle(event);
        } else {
            log.warn("No handler found for event type: {}", event.getClass().getSimpleName());
        }
    }
}
