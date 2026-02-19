package com.heditra.events.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class cho mọi domain event trong hệ thống.
 * @JsonTypeInfo giúp Kafka deserialize đúng subclass khi consumer nhận event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class DomainEvent implements Serializable {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private String aggregateId;
    private Integer version;

    protected DomainEvent(String eventType, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.version = 1;
    }
}
