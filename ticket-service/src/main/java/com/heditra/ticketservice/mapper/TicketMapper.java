package com.heditra.ticketservice.mapper;

import com.heditra.ticketservice.document.TicketDocument;
import com.heditra.ticketservice.dto.request.CreateTicketRequest;
import com.heditra.ticketservice.dto.response.TicketResponse;
import com.heditra.ticketservice.model.Ticket;
import com.heditra.ticketservice.model.TicketStatus;

import java.math.BigDecimal;

public final class TicketMapper {

    private TicketMapper() {}

    public static Ticket toEntity(CreateTicketRequest request) {
        return Ticket.builder()
                .userId(request.getUserId())
                .eventName(request.getEventName())
                .quantity(request.getQuantity())
                .pricePerTicket(request.getPricePerTicket())
                .totalAmount(request.getPricePerTicket().multiply(
                        BigDecimal.valueOf(request.getQuantity())))
                .eventDate(request.getEventDate())
                .status(TicketStatus.PENDING)
                .build();
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .userId(ticket.getUserId())
                .eventName(ticket.getEventName())
                .quantity(ticket.getQuantity())
                .pricePerTicket(ticket.getPricePerTicket())
                .totalAmount(ticket.getTotalAmount())
                .status(ticket.getStatus())
                .eventDate(ticket.getEventDate())
                .bookingDate(ticket.getBookingDate())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public static TicketResponse toResponse(TicketDocument doc) {
        if (doc == null) {
            return null;
        }
        return TicketResponse.builder()
                .id(Long.parseLong(doc.getId()))
                .userId(doc.getUserId())
                .eventName(doc.getEventName())
                .quantity(doc.getQuantity())
                .pricePerTicket(doc.getPricePerTicket() != null ? BigDecimal.valueOf(doc.getPricePerTicket()) : null)
                .totalAmount(doc.getTotalAmount() != null ? BigDecimal.valueOf(doc.getTotalAmount()) : null)
                .status(doc.getStatus() != null ? TicketStatus.valueOf(doc.getStatus()) : null)
                .eventDate(doc.getEventDate())
                .bookingDate(doc.getBookingDate())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
