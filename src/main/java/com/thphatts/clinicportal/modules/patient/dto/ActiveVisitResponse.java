package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType;
import java.time.LocalDateTime;
import java.util.List;

public record ActiveVisitResponse(Long visitId, Long patientId, String patientName, Long appointmentId,
                                  String visitType, LocalDateTime startTime, List<EncounterType> encounters) {}
