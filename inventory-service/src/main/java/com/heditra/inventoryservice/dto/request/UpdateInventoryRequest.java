package com.heditra.inventoryservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateInventoryRequest {

    @Size(max = 200, message = "Event name must not exceed 200 characters")
    private String eventName;

    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(max = 300, message = "Location must not exceed 300 characters")
    private String location;
}
