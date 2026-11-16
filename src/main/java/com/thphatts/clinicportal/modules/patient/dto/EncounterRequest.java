package com.thphatts.clinicportal.modules.patient.dto;

import com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EncounterRequest(@NotNull EncounterType encounterType, Long doctorId,
                               @Valid List<ObsRequest> observations) {}
