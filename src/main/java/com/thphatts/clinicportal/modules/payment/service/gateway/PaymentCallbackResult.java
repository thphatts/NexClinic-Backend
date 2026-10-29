package com.thphatts.clinicportal.modules.payment.service.gateway;

public record PaymentCallbackResult(
        String orderRef,
        boolean success,
        String gatewayTransactionId,
        String rawResponseCode
) {
}
