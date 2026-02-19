package com.heditra.inventoryservice.dto.response;

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
public class InventoryResponse {

    private Long id;
    private String eventName;
    private LocalDateTime eventDate;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal price;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
