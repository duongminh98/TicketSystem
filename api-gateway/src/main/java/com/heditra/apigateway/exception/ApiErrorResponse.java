package com.heditra.apigateway.exception;

import java.time.Instant;

public class ApiErrorResponse {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final Instant timestamp;
    private final String path;
    private final String details;

    public ApiErrorResponse(boolean success,
                            String errorCode,
                            String message,
                            Instant timestamp,
                            String path,
                            String details) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getDetails() {
        return details;
    }
}

