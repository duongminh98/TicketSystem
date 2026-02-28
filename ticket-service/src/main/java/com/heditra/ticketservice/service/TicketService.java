package com.heditra.ticketservice.service;

import com.heditra.ticketservice.dto.request.CreateTicketRequest;
import com.heditra.ticketservice.dto.request.UpdateTicketRequest;
import com.heditra.ticketservice.dto.response.TicketResponse;
import com.heditra.ticketservice.model.TicketStatus;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse getTicketById(Long id);

    List<TicketResponse> getAllTickets();

    List<TicketResponse> getTicketsByUserId(Long userId);

    List<TicketResponse> getTicketsByStatus(TicketStatus status);

    List<TicketResponse> getTicketsByEventName(String eventName);

    TicketResponse updateTicket(Long id, UpdateTicketRequest request);

    TicketResponse cancelTicket(Long id, String reason);

    TicketResponse confirmTicket(Long id);

    void deleteTicket(Long id);

    List<TicketResponse> searchTickets(String keyword);
}
