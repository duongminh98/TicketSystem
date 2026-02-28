package com.heditra.ticketservice.repository;

import com.heditra.ticketservice.document.TicketDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface TicketSearchRepository extends ElasticsearchRepository<TicketDocument, String> {

    List<TicketDocument> findByEventNameContaining(String eventName);

    List<TicketDocument> findByEventName(String eventName);

    List<TicketDocument> findByUserId(Long userId);

    List<TicketDocument> findByStatus(String status);
}
