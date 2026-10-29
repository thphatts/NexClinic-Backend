package com.thphatts.clinicportal.modules.payment.dto;

import com.thphatts.clinicportal.modules.payment.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotNull
    private Long appointmentId;

    @NotNull
    private PaymentMethod paymentMethod;
}
