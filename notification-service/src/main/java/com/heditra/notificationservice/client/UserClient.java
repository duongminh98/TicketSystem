package com.heditra.notificationservice.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    public String getUserEmail(Long userId) {
        try {
            String url = userServiceBaseUrl + "/users/" + userId;
            ResponseEntity<ApiResponse<UserSummary>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserSummary>>() {
                    }
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Failed to fetch user {} from user-service. Status={}", userId, response.getStatusCode());
                return null;
            }

            ApiResponse<UserSummary> body = response.getBody();
            if (!body.isSuccess() || body.getData() == null) {
                log.warn("User-service returned unsuccessful response for user {}: {}", userId, body.getMessage());
                return null;
            }

            return body.getData().getEmail();
        } catch (Exception ex) {
            log.error("Error while calling user-service for user {}: {}", userId, ex.getMessage());
            return null;
        }
    }

    @Data
    private static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
    }

    @Data
    private static class UserSummary {
        private Long id;
        private String username;
        private String email;
    }
}

