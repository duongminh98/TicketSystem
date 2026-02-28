package com.heditra.saga.exception.handler;

import com.heditra.saga.exception.ApiErrorResponse;
import com.heditra.saga.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        ApiErrorResponse response = ApiErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getDescription(false),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getDescription(false),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

