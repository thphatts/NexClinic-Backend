package com.thphatts.clinicportal.dto.response;

import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import com.thphatts.clinicportal.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String orderRef;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentUrl;
}
