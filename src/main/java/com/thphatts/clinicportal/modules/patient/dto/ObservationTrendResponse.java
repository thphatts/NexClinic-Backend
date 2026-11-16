package com.thphatts.clinicportal.modules.patient.dto;

import java.time.LocalDateTime;

public record ObservationTrendResponse(Long conceptId, String conceptName, String unit,
        Double value, LocalDateTime observedAt) {}
