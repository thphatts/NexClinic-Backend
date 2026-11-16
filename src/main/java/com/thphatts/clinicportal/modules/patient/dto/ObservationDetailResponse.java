package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.concept.enums.DataType;
import java.time.LocalDateTime;

public record ObservationDetailResponse(Long obsId, Long conceptId, String conceptName, String unit,
        DataType dataType, Double valueNumeric, String valueText, Long valueCodedId, String valueCodedName,
        LocalDateTime valueDatetime, Boolean valueBoolean, LocalDateTime observedAt) {}
