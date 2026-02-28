package com.heditra.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnPayProperties {

    /**
     * Merchant code assigned by VNPay.
     */
    private String tmnCode;

    /**
     * Secret key for HMAC signature.
     */
    private String hashSecret;

    /**
     * VNPay payment URL (sandbox or production).
     */
    private String url;

    /**
     * Return URL that VNPay will redirect the user to after payment.
     */
    private String returnUrl;
}

