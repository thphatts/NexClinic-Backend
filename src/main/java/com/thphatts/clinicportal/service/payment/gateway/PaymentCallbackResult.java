package com.thphatts.clinicportal.service.payment.gateway;

public record PaymentCallbackResult(
        String orderRef,
        boolean success,
        String gatewayTransactionId,
        String rawResponseCode
) {
}
