package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record DoctorScheduleRequest(
        @NotNull(message = "Thứ trong tuần không được để trống")
        @Min(value = 1, message = "day_of_week phải từ 1 (Thứ 2) đến 7 (Chủ Nhật)")
        @Max(value = 7, message = "day_of_week phải từ 1 (Thứ 2) đến 7 (Chủ Nhật)")
        Integer dayOfWeek,

        @NotNull(message = "Giờ bắt đầu không được để trống")
        LocalTime startTime,

        @NotNull(message = "Giờ kết thúc không được để trống")
        LocalTime endTime,

        @Min(value = 5, message = "Thời lượng mỗi slot tối thiểu 5 phút")
        Integer slotDurationMinutes
) {}