package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType;
import java.time.LocalDateTime;

public record EncounterCreatedResponse(Long encounterId, Long visitId, EncounterType encounterType,
                                       LocalDateTime encounterDatetime, int observationCount) {}
