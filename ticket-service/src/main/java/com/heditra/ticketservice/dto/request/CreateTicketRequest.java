package com.heditra.ticketservice.dto.request;

import jakarta.validation.constraints.*;
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
public class CreateTicketRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Event name is required")
    @Size(min = 1, max = 200, message = "Event name must be between 1 and 200 characters")
    private String eventName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price per ticket is required")
    @DecimalMin(value = "0.01", message = "Price per ticket must be positive")
    private BigDecimal pricePerTicket;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;
}
