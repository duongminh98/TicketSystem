package com.heditra.ticketservice.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final String errorCode;

    public TechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
