package com.thphatts.clinicportal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicalRecordRequest(
        @NotNull(message = "Appointment ID không được để trống")
        Long appointmentId,

        @NotBlank(message = "Chẩn đoán không được để trống")
        String diagnosis,

        String symptoms,

        String notes,

        LocalDate reexaminationDate,

        @Valid
        PrescriptionRequest prescription
) {
}
