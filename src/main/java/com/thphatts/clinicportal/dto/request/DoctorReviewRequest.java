package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record DoctorReviewRequest(

        Long appointmentId,

        @NotNull(message = "Id bác sĩ không được trống!")
        Long doctorId,

        String comment,

        @NotNull(message = "Đánh giá không được bỏ trống!")
        @Max(value = 5)
        @Min(value = 1)
        Integer rating
) {
}
