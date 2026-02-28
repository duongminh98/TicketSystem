package com.heditra.paymentservice.controller;

import com.heditra.paymentservice.dto.request.CreatePaymentRequest;
import com.heditra.paymentservice.dto.response.ApiResponse;
import com.heditra.paymentservice.dto.response.PaymentResponse;
import com.heditra.paymentservice.model.PaymentStatus;
import com.heditra.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment processing APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a new payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @GetMapping("/ticket/{ticketId}")
    @Operation(summary = "Get payment by ticket ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTicketId(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByTicketId(ticketId)));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment by transaction ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByTransactionId(transactionId)));
    }

    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get payments by user ID")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByUserId(userId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByStatus(
            @PathVariable PaymentStatus status) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByStatus(status)));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process a pending payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.processPayment(id), "Payment URL generated successfully"));
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay return URL (callback after payment)")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleVnPayReturn(
            @RequestParam Map<String, String> params) {
        PaymentResponse payment = paymentService.handleVnPayReturn(params);
        return ResponseEntity.ok(ApiResponse.success(payment, "VNPay callback processed"));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a successful payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.refundPayment(id), "Payment refunded successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
