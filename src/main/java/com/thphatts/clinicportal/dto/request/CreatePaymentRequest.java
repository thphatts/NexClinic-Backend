package com.thphatts.clinicportal.dto.request;

import com.thphatts.clinicportal.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotNull
    private Long appointmentId;

    @NotNull
    private PaymentMethod paymentMethod;
}
