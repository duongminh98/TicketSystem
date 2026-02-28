package com.heditra.paymentservice.integration;

import com.heditra.paymentservice.config.VnPayProperties;
import com.heditra.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VnPayClient {

    private final VnPayProperties properties;

    /**
     * Build VNPay payment URL for redirecting the end user.
     */
    public String buildPaymentUrl(Payment payment) {
        if (properties.getUrl() == null || properties.getTmnCode() == null || properties.getHashSecret() == null) {
            log.warn("VNPay is not fully configured. Returning empty URL.");
            return "";
        }
        try {
            return createSignedUrl(payment);
        } catch (Exception ex) {
            log.error("Error while building VNPay payment URL for payment {}: {}", payment.getId(), ex.getMessage());
            return "";
        }
    }

    private String createSignedUrl(Payment payment) throws Exception {
        Map<String, String> params = new TreeMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", payment.getAmount().multiply(java.math.BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", "Payment for ticket " + payment.getTicketId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (hashData.length() > 0) {
                hashData.append('&');
                query.append('&');
            }
            hashData.append(entry.getKey()).append('=').append(entry.getValue());

            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
        }

        String secureHash = hmacSHA512(properties.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return properties.getUrl() + "?" + query;
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKey);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verify VNPay callback signature using vnp_SecureHash and all vnp_* params except vnp_SecureHash.
     */
    public boolean verifySignature(Map<String, String> vnpParams) {
        String receivedHash = vnpParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        try {
            Map<String, String> sorted = new TreeMap<>();
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("vnp_") && !"vnp_SecureHash".equals(key)) {
                    sorted.put(key, entry.getValue());
                }
            }

            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(entry.getKey()).append('=').append(entry.getValue());
            }

            String calculated = hmacSHA512(properties.getHashSecret(), hashData.toString());
            return receivedHash.equalsIgnoreCase(calculated);
        } catch (Exception ex) {
            log.error("Error while verifying VNPay signature: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Check whether VNPay response indicates success.
     * According to VNPay docs, both vnp_ResponseCode and vnp_TransactionStatus should be \"00\".
     */
    public boolean isSuccessResponse(Map<String, String> vnpParams) {
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }
}

