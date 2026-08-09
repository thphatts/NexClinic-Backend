
package com.thphatts.clinicportal.dto.response;

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