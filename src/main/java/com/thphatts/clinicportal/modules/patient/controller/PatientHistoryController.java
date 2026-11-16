package com.thphatts.clinicportal.modules.patient.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.modules.patient.dto.*;
import com.thphatts.clinicportal.modules.patient.entity.Obs;
import com.thphatts.clinicportal.modules.patient.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
@RequiredArgsConstructor
public class PatientHistoryController {
    private final PatientRepository patients;
    private final VisitRepository visits;
    private final EncounterRepository encounters;
    private final ObsRepository observations;

    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ApiResponse<List<VisitHistoryResponse>> visits(@PathVariable Long patientId) {
        requirePatient(patientId);
        return ApiResponse.success(visits.findByPatient_IdOrderByStartTimeDesc(patientId).stream().map(visit ->
                new VisitHistoryResponse(visit.getId(), visit.getAppointmentId(), visit.getVisitType(), visit.getStatus(),
                        visit.getStartTime(), visit.getEndTime(), encounters.findByVisit_Id(visit.getId()).stream().map(encounter ->
                        new EncounterDetailResponse(encounter.getId(), encounter.getEncounterType(), encounter.getEncounterDatetime(),
                                encounter.getDoctor() == null ? null : encounter.getDoctor().getFullName(),
                                observations.findByEncounter_Visit_IdOrderByEncounter_EncounterDatetimeAsc(visit.getId()).stream()
                                        .filter(obs -> obs.getEncounter().getId().equals(encounter.getId())).map(this::toDetail).toList())).toList())).toList());
    }

    @GetMapping("/observations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ApiResponse<List<ObservationTrendResponse>> trend(@PathVariable Long patientId, @RequestParam Long conceptId,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        requirePatient(patientId);
        LocalDate start = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate finish = to == null ? LocalDate.now() : to;
        if (start.isAfter(finish)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ.");
        return ApiResponse.success(observations.findNumericTrend(patientId, conceptId, start.atStartOfDay(), finish.plusDays(1).atStartOfDay())
                .stream().map(obs -> new ObservationTrendResponse(obs.getConcept().getId(), obs.getConcept().getName(),
                        obs.getConcept().getUnit(), obs.getValueNumeric(), obs.getEncounter().getEncounterDatetime())).toList());
    }

    private void requirePatient(Long patientId) {
        if (!patients.existsById(patientId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân.");
    }

    private ObservationDetailResponse toDetail(Obs obs) {
        return new ObservationDetailResponse(obs.getId(), obs.getConcept().getId(), obs.getConcept().getName(),
                obs.getConcept().getUnit(), obs.getConcept().getDatatype(), obs.getValueNumeric(), obs.getValueText(),
                obs.getValueCoded() == null ? null : obs.getValueCoded().getId(),
                obs.getValueCoded() == null ? null : obs.getValueCoded().getName(), obs.getValueDatetime(),
                obs.getValueBoolean(), obs.getEncounter().getEncounterDatetime());
    }
}
