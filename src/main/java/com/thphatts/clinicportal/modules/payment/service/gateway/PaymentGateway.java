package com.thphatts.clinicportal.modules.payment.service.gateway;

import com.thphatts.clinicportal.modules.payment.entity.Payment;

import java.util.Map;

public interface PaymentGateway {
    String createPaymentUrl(Payment payment, String ipAddress);

    boolean verifySignature(Map<String, String> callbackParams);

    PaymentCallbackResult parseCallback(Map<String, String> callbackParams);
}
