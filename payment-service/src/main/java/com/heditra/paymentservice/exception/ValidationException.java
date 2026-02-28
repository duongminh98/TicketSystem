package com.heditra.paymentservice.exception;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
