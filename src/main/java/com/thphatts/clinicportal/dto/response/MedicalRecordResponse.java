package com.thphatts.clinicportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalRecordResponse(
        Long id,
        Long appointmentId,
        Long patientId,
        String patientName,
        String patientPhone,
        String citizenId,
        Long doctorId,
        String doctorName,
        String doctorSpecialization,
        String diagnosis,
        String symptoms,
        String notes,
        LocalDate reexaminationDate,
        PrescriptionResponse prescription,
        LocalDateTime createdAt
) {
}
