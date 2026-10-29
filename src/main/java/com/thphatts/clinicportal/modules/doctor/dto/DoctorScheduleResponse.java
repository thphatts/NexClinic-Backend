package com.thphatts.clinicportal.modules.doctor.dto;

import java.time.LocalTime;

public record DoctorScheduleResponse(
        Long id,
        Long doctorId,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Boolean active
) {}