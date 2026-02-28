package com.heditra.ticketservice.dto.response;

import com.heditra.ticketservice.model.TicketStatus;
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
public class TicketResponse {

    private Long id;
    private Long userId;
    private String eventName;
    private Integer quantity;
    private BigDecimal pricePerTicket;
    private BigDecimal totalAmount;
    private TicketStatus status;
    private LocalDateTime eventDate;
    private LocalDateTime bookingDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
