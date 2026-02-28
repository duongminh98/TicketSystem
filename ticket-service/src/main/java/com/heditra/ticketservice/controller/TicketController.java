package com.heditra.ticketservice.controller;

import com.heditra.ticketservice.dto.request.CreateTicketRequest;
import com.heditra.ticketservice.dto.request.UpdateTicketRequest;
import com.heditra.ticketservice.dto.response.ApiResponse;
import com.heditra.ticketservice.dto.response.TicketResponse;
import com.heditra.ticketservice.model.TicketStatus;
import com.heditra.ticketservice.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket booking management APIs")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Book a new ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        TicketResponse ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket, "Ticket booked successfully"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get tickets by user ID")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketsByUserId(userId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get tickets by status")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByStatus(
            @PathVariable TicketStatus status) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketsByStatus(status)));
    }

    @GetMapping("/event/{eventName}")
    @Operation(summary = "Get tickets by event name")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByEventName(
            @PathVariable String eventName) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketsByEventName(eventName)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search tickets by event name (Elasticsearch)")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> searchTickets(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.searchTickets(keyword)));
    }

    @GetMapping
    @Operation(summary = "Get all tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getAllTickets()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ticket (only PENDING tickets)")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.updateTicket(id, request), "Ticket updated successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
            @PathVariable Long id,
            @RequestParam(defaultValue = "User requested cancellation") String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.cancelTicket(id, reason), "Ticket cancelled successfully"));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a ticket (triggered after payment)")
    public ResponseEntity<ApiResponse<TicketResponse>> confirmTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.confirmTicket(id), "Ticket confirmed successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ticket")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
