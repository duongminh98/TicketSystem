package com.heditra.paymentservice.exception;

public class PaymentProcessingException extends BusinessException {

    public PaymentProcessingException(String message) {
        super("PAYMENT_PROCESSING_ERROR", message);
    }
}
