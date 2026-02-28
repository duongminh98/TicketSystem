package com.heditra.ticketservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckResponse {

    private Long id;
    private String eventName;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal price;
    private LocalDateTime eventDate;
    private boolean available;
}
