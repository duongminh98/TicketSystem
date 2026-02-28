package com.heditra.saga.exception;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ApiErrorResponse {

    boolean success;
    String errorCode;
    String message;
    Instant timestamp;
    String path;
    String details;

    public static ApiErrorResponse of(String errorCode, String message, String path, String details) {
        return ApiErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .path(path)
                .details(details)
                .build();
    }
}

