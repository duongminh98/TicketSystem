package com.heditra.events.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata đi kèm event: dùng để trace, correlate events xuyên suốt nhiều services.
 * correlationId: ID chung cho 1 business flow (vd: 1 lần đặt vé).
 * causationId: ID của event gây ra event hiện tại (event chain).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMetadata {

    private String eventId;
    private String eventType;
    private String source;
    private String correlationId;
    private String causationId;
    private Long timestamp;
}
