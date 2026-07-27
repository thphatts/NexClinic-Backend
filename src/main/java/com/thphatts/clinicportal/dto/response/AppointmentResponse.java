package com.thphatts.clinicportal.dto.response;

import com.thphatts.clinicportal.entity.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        String patientPhone,
        Long doctorId,
        String doctorName,
        String doctorSpecialization,
        BigDecimal consultationFee,
        LocalDate appointmentDate,
        String timeSlot,
        AppointmentStatus status,
        String reason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
