package com.thphatts.clinicportal.modules.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotResponse(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String timeSlotLabel // format giống Appointment.timeSlot hiện tại: "09:00 - 09:30"
) {}