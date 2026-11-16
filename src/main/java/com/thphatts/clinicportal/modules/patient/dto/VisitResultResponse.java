package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.VisitStatus;
import java.time.LocalDateTime;

public record VisitResultResponse(Long visitId, Long patientId, String patientName, Long appointmentId,
                                  String visitType, VisitStatus status, LocalDateTime startTime, LocalDateTime endTime) {}
