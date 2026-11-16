package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.VisitStatus;
import java.time.LocalDateTime;
import java.util.List;

public record VisitHistoryResponse(Long visitId, Long appointmentId, String visitType, VisitStatus status,
        LocalDateTime startTime, LocalDateTime endTime, List<EncounterDetailResponse> encounters) {}
