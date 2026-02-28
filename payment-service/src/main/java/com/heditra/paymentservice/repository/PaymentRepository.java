package com.heditra.paymentservice.repository;

import com.heditra.paymentservice.model.Payment;
import com.heditra.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTicketId(Long ticketId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(PaymentStatus status);
}
