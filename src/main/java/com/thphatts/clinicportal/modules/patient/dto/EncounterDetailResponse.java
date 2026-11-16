package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType;
import java.time.LocalDateTime;
import java.util.List;

public record EncounterDetailResponse(Long encounterId, EncounterType encounterType,
        LocalDateTime encounterDatetime, String doctorName, List<ObservationDetailResponse> observations) {}
