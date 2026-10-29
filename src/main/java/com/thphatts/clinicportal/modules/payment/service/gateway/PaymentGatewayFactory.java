package com.thphatts.clinicportal.modules.payment.service.gateway;

import com.thphatts.clinicportal.modules.payment.entity.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class PaymentGatewayFactory {
    private final Map<String, PaymentGateway> gatewayMap;

    public PaymentGateway getGateway(PaymentMethod method) {
        PaymentGateway gateway = gatewayMap.get(method.name());
        if(gateway == null) {
            throw new IllegalArgumentException("Không hỗ trợ thanh toán: " + method);
        }
        return gateway;
    }
}
