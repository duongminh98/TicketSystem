package com.heditra.paymentservice.mapper;

import com.heditra.paymentservice.dto.request.CreatePaymentRequest;
import com.heditra.paymentservice.dto.response.PaymentResponse;
import com.heditra.paymentservice.model.Payment;
import com.heditra.paymentservice.model.PaymentMethod;
import com.heditra.paymentservice.model.PaymentStatus;

import java.util.List;
import java.util.UUID;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static Payment toEntity(CreatePaymentRequest request) {
        PaymentMethod method = PaymentMethod.CREDIT_CARD;
        if (request.getPaymentMethod() != null) {
            try {
                method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Payment.builder()
                .ticketId(request.getTicketId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .ticketId(payment.getTicketId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public static List<PaymentResponse> toResponseList(List<Payment> payments) {
        return payments.stream().map(PaymentMapper::toResponse).toList();
    }
}
