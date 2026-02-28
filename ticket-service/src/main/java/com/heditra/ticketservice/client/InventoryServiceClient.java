package com.heditra.ticketservice.client;

import com.heditra.ticketservice.dto.response.InventoryCheckResponse;
import com.heditra.ticketservice.exception.TechnicalException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient webClient;

    @Value("${services.inventory.url:http://localhost:8082}")
    private String inventoryBaseUrl;

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "checkAvailabilityFallback")
    @Retry(name = "inventory-service")
    public InventoryCheckResponse checkAvailability(String eventName) {
        log.info("Checking inventory availability for event: {}", eventName);
        Map<String, Object> wrapper = webClient.get()
                .uri(inventoryBaseUrl + "/inventory/event/{eventName}", eventName)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (wrapper == null || wrapper.get("data") == null) {
            throw new TechnicalException("INVENTORY_UNAVAILABLE",
                    "Unable to get inventory for event: " + eventName);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) wrapper.get("data");
        return mapToInventoryCheckResponse(data);
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveSeatsFallback")
    @Retry(name = "inventory-service")
    public boolean reserveSeats(Long inventoryId, int quantity) {
        log.info("Reserving {} seats for inventory ID: {}", quantity, inventoryId);
        Map<String, Object> wrapper = webClient.post()
                .uri(inventoryBaseUrl + "/inventory/{id}/reserve?quantity={qty}",
                        inventoryId, quantity)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (wrapper == null || wrapper.get("data") == null) {
            return false;
        }
        return Boolean.TRUE.equals(wrapper.get("data"));
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseSeatsFallback")
    @Retry(name = "inventory-service")
    public boolean releaseSeats(Long inventoryId, int quantity) {
        log.info("Releasing {} seats for inventory ID: {}", quantity, inventoryId);
        Map<String, Object> wrapper = webClient.post()
                .uri(inventoryBaseUrl + "/inventory/{id}/release?quantity={qty}",
                        inventoryId, quantity)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (wrapper == null || wrapper.get("data") == null) {
            return false;
        }
        return Boolean.TRUE.equals(wrapper.get("data"));
    }

    @SuppressWarnings("unused")
    private InventoryCheckResponse checkAvailabilityFallback(String eventName, Throwable t) {
        log.warn("Circuit breaker fallback for checkAvailability: event={}, error={}",
                eventName, t.getMessage());
        return InventoryCheckResponse.builder()
                .eventName(eventName)
                .available(false)
                .build();
    }

    @SuppressWarnings("unused")
    private boolean reserveSeatsFallback(Long inventoryId, int quantity, Throwable t) {
        log.warn("Circuit breaker fallback for reserveSeats: inventoryId={}, error={}",
                inventoryId, t.getMessage());
        return false;
    }

    @SuppressWarnings("unused")
    private boolean releaseSeatsFallback(Long inventoryId, int quantity, Throwable t) {
        log.warn("Circuit breaker fallback for releaseSeats: inventoryId={}, error={}",
                inventoryId, t.getMessage());
        return false;
    }

    private InventoryCheckResponse mapToInventoryCheckResponse(Map<String, Object> data) {
        return InventoryCheckResponse.builder()
                .id(data.get("id") != null ? Long.valueOf(data.get("id").toString()) : null)
                .eventName((String) data.get("eventName"))
                .totalSeats(data.get("totalSeats") != null
                        ? Integer.valueOf(data.get("totalSeats").toString()) : null)
                .availableSeats(data.get("availableSeats") != null
                        ? Integer.valueOf(data.get("availableSeats").toString()) : null)
                .price(data.get("price") != null
                        ? new java.math.BigDecimal(data.get("price").toString()) : null)
                .available(data.get("availableSeats") != null
                        && Integer.parseInt(data.get("availableSeats").toString()) > 0)
                .build();
    }
}
