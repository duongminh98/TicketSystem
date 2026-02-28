package com.heditra.ticketservice.repository;

import com.heditra.ticketservice.model.Ticket;
import com.heditra.ticketservice.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByEventName(String eventName);
}
