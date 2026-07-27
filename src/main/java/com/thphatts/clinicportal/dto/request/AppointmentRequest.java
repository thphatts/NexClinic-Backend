package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AppointmentRequest(
        @NotNull(message = "Bệnh nhân không được để trống")
        Long patientId,

        @NotNull(message = "Bác sĩ không được để trống")
        Long doctorId,

        @NotNull(message = "Ngày khám không được để trống")
        @FutureOrPresent(message = "Ngày khám phải là ngày hôm nay hoặc trong tương lai")
        LocalDate appointmentDate,

        @NotBlank(message = "Ca khám/khung giờ không được để trống")
        String timeSlot,

        String reason,

        String notes
) {}
