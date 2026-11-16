package com.thphatts.clinicportal.modules.patient.dto;

import jakarta.validation.constraints.NotNull;

public record VisitRequest(@NotNull Long patientId, @NotNull Long appointmentId, String visitType) {}
