package com.thphatts.clinicportal.modules.doctor.dto;

import java.time.LocalDateTime;

public record DoctorReviewResponse(
        Long id,
        Long doctorId,
        Long patientId,
        String patientName,
        String doctorName,
        Boolean verified,
        Long appointmentId,
        Integer rating,
        String comment,
        Integer visitCountSnapshot,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
