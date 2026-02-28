package com.heditra.paymentservice.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final String errorCode;

    public TechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
