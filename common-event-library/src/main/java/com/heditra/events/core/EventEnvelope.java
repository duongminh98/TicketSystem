package com.heditra.events.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper bọc event kèm metadata (correlation ID, source, ...).
 * Hữu ích khi cần trace event qua nhiều service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T extends DomainEvent> {

    private EventMetadata metadata;
    private T payload;
}
