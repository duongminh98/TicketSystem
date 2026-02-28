package com.heditra.paymentservice.service.impl;

import com.heditra.events.core.EventPublisher;
import com.heditra.events.payment.PaymentCompletedEvent;
import com.heditra.events.payment.PaymentFailedEvent;
import com.heditra.events.payment.PaymentInitiatedEvent;
import com.heditra.events.payment.PaymentRefundedEvent;
import com.heditra.paymentservice.dto.request.CreatePaymentRequest;
import com.heditra.paymentservice.dto.response.PaymentResponse;
import com.heditra.paymentservice.exception.PaymentNotFoundException;
import com.heditra.paymentservice.exception.PaymentProcessingException;
import com.heditra.paymentservice.integration.VnPayClient;
import com.heditra.paymentservice.mapper.PaymentMapper;
import com.heditra.paymentservice.model.Payment;
import com.heditra.paymentservice.model.PaymentStatus;
import com.heditra.paymentservice.repository.PaymentRepository;
import com.heditra.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final VnPayClient vnPayClient;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for ticket {} user {}", request.getTicketId(), request.getUserId());

        Payment payment = PaymentMapper.toEntity(request);
        payment = paymentRepository.save(payment);

        publishPaymentInitiatedEvent(payment);

        log.info("Payment created: id={}, transactionId={}", payment.getId(), payment.getTransactionId());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(PaymentMapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Override
    public PaymentResponse getPaymentByTicketId(Long ticketId) {
        return paymentRepository.findByTicketId(ticketId)
                .map(PaymentMapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException("ticketId", String.valueOf(ticketId)));
    }

    @Override
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(PaymentMapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException("transactionId", transactionId));
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return PaymentMapper.toResponseList(paymentRepository.findAll());
    }

    @Override
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return PaymentMapper.toResponseList(paymentRepository.findByUserId(userId));
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        return PaymentMapper.toResponseList(paymentRepository.findByStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse processPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException(
                    "Payment must be PENDING to process. Current status: " + payment.getStatus());
        }

        String paymentUrl = vnPayClient.buildPaymentUrl(payment);
        if (paymentUrl.isEmpty()) {
            log.warn("VNPay URL is empty, cannot start payment for {}", paymentId);
            throw new PaymentProcessingException("Unable to build VNPay payment URL");
        }

        PaymentResponse response = PaymentMapper.toResponse(payment);
        response.setPaymentUrl(paymentUrl);
        log.info("Generated VNPay payment URL for payment {}: {}", paymentId, paymentUrl);
        return response;
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException(
                    "Payment must be SUCCESS to refund. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);

        publishPaymentRefundedEvent(payment, "Refund requested");

        log.info("Payment {} refunded", paymentId);
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        paymentRepository.delete(payment);
        log.info("Payment deleted: {}", id);
    }

    @Override
    @Transactional
    public PaymentResponse handleVnPayReturn(java.util.Map<String, String> vnpParams) {
        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new PaymentProcessingException("Missing vnp_TxnRef in VNPay response");
        }

        if (!vnPayClient.verifySignature(vnpParams)) {
            throw new PaymentProcessingException("Invalid VNPay signature");
        }

        Payment payment = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new PaymentNotFoundException("transactionId", txnRef));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Ignoring VNPay callback for payment {} with non-PENDING status: {}", payment.getId(),
                    payment.getStatus());
            return PaymentMapper.toResponse(payment);
        }

        if (vnPayClient.isSuccessResponse(vnpParams)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment = paymentRepository.save(payment);
            publishPaymentCompletedEvent(payment);
            log.info("VNPay payment completed for transaction {}", txnRef);
        } else {
            String responseCode = vnpParams.get("vnp_ResponseCode");
            String reason = "VNPay payment failed, responseCode=" + responseCode;
            payment.setStatus(PaymentStatus.FAILED);
            payment = paymentRepository.save(payment);
            publishPaymentFailedEvent(payment, reason);
            log.warn("VNPay payment failed for transaction {} with responseCode={}", txnRef, responseCode);
        }

        return PaymentMapper.toResponse(payment);
    }

    private void publishPaymentInitiatedEvent(Payment payment) {
        try {
            PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                    payment.getId(), payment.getTicketId(), payment.getUserId(),
                    payment.getAmount(), payment.getPaymentMethod().name(), payment.getTransactionId());
            eventPublisher.publish("payment-events", event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentInitiatedEvent for payment {}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payment.getId(), payment.getTicketId(), payment.getUserId(),
                    payment.getAmount(), payment.getTransactionId());
            eventPublisher.publish("payment-events", event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent for payment {}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private void publishPaymentFailedEvent(Payment payment, String reason) {
        try {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    payment.getId(), payment.getTicketId(), payment.getUserId(),
                    payment.getAmount(), payment.getTransactionId(), reason);
            eventPublisher.publish("payment-events", event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent for payment {}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private void publishPaymentRefundedEvent(Payment payment, String reason) {
        try {
            PaymentRefundedEvent event = new PaymentRefundedEvent(
                    payment.getId(), payment.getTicketId(), payment.getUserId(),
                    payment.getAmount(), payment.getTransactionId(), reason);
            eventPublisher.publish("payment-events", event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentRefundedEvent for payment {}: {}",
                    payment.getId(), e.getMessage());
        }
    }
}
