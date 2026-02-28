package com.heditra.ticketservice.dto.request;

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
public class UpdateTicketRequest {

    @Size(max = 200, message = "Event name must be at most 200 characters")
    private String eventName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal pricePerTicket;

    private LocalDateTime eventDate;
}
