package com.thphatts.clinicportal.modules.patient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MedicalRecordRequest(
        @NotNull(message = "Appointment ID không được để trống")
        Long appointmentId,

        @NotBlank(message = "Chẩn đoán không được để trống")
        String diagnosis,

        String symptoms,

        String notes,

        LocalDate reexaminationDate,

        @Valid
        PrescriptionRequest prescription,

        @Valid
        List<ObsRequest> observations
) {
    public MedicalRecordRequest(
            Long appointmentId,
            String diagnosis,
            String symptoms,
            String notes,
            LocalDate reexaminationDate,
            PrescriptionRequest prescription
    ) {
        this(appointmentId, diagnosis, symptoms, notes, reexaminationDate, prescription, null);
    }
}
