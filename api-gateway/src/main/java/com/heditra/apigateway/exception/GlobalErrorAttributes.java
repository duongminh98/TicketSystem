package com.heditra.apigateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    private final ObjectMapper objectMapper;

    public GlobalErrorAttributes(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> defaultAttributes = super.getErrorAttributes(
                request,
                options.including(ErrorAttributeOptions.Include.MESSAGE)
        );

        ApiErrorResponse apiError = new ApiErrorResponse(
                false,
                (String) defaultAttributes.getOrDefault("error", "UNKNOWN_ERROR"),
                (String) defaultAttributes.getOrDefault("message", "Unexpected error"),
                Instant.now(),
                (String) defaultAttributes.getOrDefault("path", request.path()),
                (String) defaultAttributes.get("trace")
        );

        return objectMapper.convertValue(apiError, Map.class);
    }
}

