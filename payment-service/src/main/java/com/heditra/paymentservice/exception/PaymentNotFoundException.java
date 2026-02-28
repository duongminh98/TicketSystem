package com.heditra.paymentservice.exception;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException(Long id) {
        super("PAYMENT_NOT_FOUND", "Payment not found with id: " + id);
    }

    public PaymentNotFoundException(String field, String value) {
        super("PAYMENT_NOT_FOUND", "Payment not found with " + field + ": " + value);
    }
}
