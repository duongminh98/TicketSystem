package com.heditra.ticketservice.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private String details;

    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return ApiErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
