package com.thphatts.clinicportal.modules.patient.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ObsRequest(
        @NotNull(message = "Concept ID không được để trống")
        Long conceptId,
        Double valueNumeric,
        String valueText,
        Long valueCodedId,
        LocalDateTime valueDatetime,
        Boolean valueBoolean
) {
}
