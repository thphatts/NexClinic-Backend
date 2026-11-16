package com.thphatts.clinicportal.modules.patient.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.modules.appointment.entity.Appointment;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.modules.appointment.repository.AppointmentRepository;
import com.thphatts.clinicportal.modules.concept.entity.Concept;
import com.thphatts.clinicportal.modules.concept.repository.ConceptRepository;
import com.thphatts.clinicportal.modules.concept.repository.ConceptAnswerRepository;
import com.thphatts.clinicportal.modules.concept.enums.DataType;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorRepository;
import com.thphatts.clinicportal.modules.patient.dto.*;
import com.thphatts.clinicportal.modules.patient.entity.*;
import com.thphatts.clinicportal.modules.patient.entity.enums.*;
import com.thphatts.clinicportal.modules.patient.repository.*;
import com.thphatts.clinicportal.modules.person.entity.Person;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {
    private final VisitRepository visits;
    private final PatientRepository patients;
    private final EncounterRepository encounters;
    private final ObsRepository observations;
    private final ConceptRepository concepts;
    private final ConceptAnswerRepository conceptAnswers;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Transactional
    public ApiResponse<ActiveVisitResponse> checkIn(@Valid @RequestBody VisitRequest request) {
        Patient patient = patients.findById(request.patientId()).orElseThrow(() -> notFound("Không tìm thấy bệnh nhân."));
        Appointment appointment = appointments.findById(request.appointmentId())
                .orElseThrow(() -> notFound("Không tìm thấy lịch hẹn."));
        if (!appointment.getPatient().getId().equals(patient.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn không thuộc bệnh nhân này.");
        if (!appointment.getAppointmentDate().equals(java.time.LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể check-in lịch hẹn trong ngày hôm nay.");
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lịch hẹn không thể check-in ở trạng thái hiện tại.");
        if (visits.existsByAppointmentId(appointment.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lịch hẹn này đã được check-in.");
        if (visits.existsByPatient_IdAndStatus(patient.getId(), VisitStatus.IN_PROGRESS)
                || visits.existsByPatient_IdAndStatus(patient.getId(), VisitStatus.ACTIVE))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bệnh nhân đã có lượt khám đang mở.");
        Visit visit = Visit.builder().patient(patient).appointmentId(request.appointmentId())
                .visitType(request.visitType() == null || request.visitType().isBlank() ? "OUTPATIENT" : request.visitType())
                .status(VisitStatus.IN_PROGRESS).startTime(LocalDateTime.now()).build();
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        return ApiResponse.success(toActiveVisit(visits.save(visit)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Transactional(readOnly = true)
    public ApiResponse<List<ActiveVisitResponse>> activeVisits() {
        return ApiResponse.success(visits.findByStatusOrderByStartTimeDesc(VisitStatus.IN_PROGRESS).stream()
                .map(this::toActiveVisit).toList());
    }

    @PostMapping("/{visitId}/encounters")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Transactional
    public ApiResponse<EncounterCreatedResponse> createEncounter(@PathVariable Long visitId, @Valid @RequestBody EncounterRequest request,
                                                   Authentication authentication) {
        Visit visit = visits.findById(visitId).orElseThrow(() -> notFound("Không tìm thấy lượt khám."));
        if (visit.getStatus() != VisitStatus.IN_PROGRESS && visit.getStatus() != VisitStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lượt khám đã đóng.");
        boolean isDoctor = hasRole(authentication, "ROLE_DOCTOR");
        boolean isReceptionist = hasRole(authentication, "ROLE_RECEPTIONIST") || hasRole(authentication, "ROLE_NURSE");
        if (request.encounterType() == EncounterType.CONSULTATION && !isDoctor)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ bác sĩ được tạo phiên khám lâm sàng.");
        if (request.encounterType() == EncounterType.VITALS && !(isDoctor || isReceptionist))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền ghi nhận sinh hiệu.");
        Doctor doctor = request.doctorId() == null ? null : doctors.findById(request.doctorId())
                .orElseThrow(() -> notFound("Không tìm thấy bác sĩ."));
        if (doctor == null && visit.getAppointmentId() != null)
            doctor = appointments.findById(visit.getAppointmentId()).map(Appointment::getDoctor).orElse(null);
        if (doctor == null && authentication != null && isDoctor)
            doctor = doctors.findByUserId(authentication.getName()).orElse(null);
        Encounter encounter = Encounter.builder().visit(visit).patient(visit.getPatient()).doctor(doctor)
                .encounterType(request.encounterType()).encounterDatetime(LocalDateTime.now()).build();
        encounter = encounters.save(encounter);
        Person person = visit.getPatient().getPerson();
        if (request.observations() != null) for (ObsRequest value : request.observations()) {
            Concept concept = concepts.findById(value.conceptId()).orElseThrow(() -> notFound("Không tìm thấy Concept."));
            Concept coded = value.valueCodedId() == null ? null : concepts.findById(value.valueCodedId())
                    .orElseThrow(() -> notFound("Không tìm thấy Concept trả lời."));
            validateObservation(concept, value);
            if (coded != null && !conceptAnswers.existsByQuestion_IdAndAnswer_Id(concept.getId(), coded.getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị CODED không thuộc danh sách lựa chọn của Concept.");
            observations.save(Obs.builder().encounter(encounter).person(person).concept(concept)
                    .valueNumeric(value.valueNumeric()).valueText(value.valueText()).valueCoded(coded)
                    .valueDatetime(value.valueDatetime()).valueBoolean(value.valueBoolean()).build());
        }
        return ApiResponse.success(new EncounterCreatedResponse(encounter.getId(), visitId, encounter.getEncounterType(),
                encounter.getEncounterDatetime(), request.observations() == null ? 0 : request.observations().size()));
    }

    @PatchMapping("/{visitId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Transactional
    public ApiResponse<VisitResultResponse> close(@PathVariable Long visitId) {
        Visit visit = visits.findById(visitId).orElseThrow(() -> notFound("Không tìm thấy lượt khám."));
        if (!encounters.existsByVisit_IdAndEncounterType(visitId, EncounterType.CONSULTATION))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần hoàn tất phiên khám lâm sàng trước khi đóng lượt.");
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setEndTime(LocalDateTime.now());
        visit = visits.save(visit);
        return ApiResponse.success(new VisitResultResponse(visit.getId(), visit.getPatient().getId(),
                visit.getPatient().getFullName(), visit.getAppointmentId(), visit.getVisitType(), visit.getStatus(),
                visit.getStartTime(), visit.getEndTime()));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private ActiveVisitResponse toActiveVisit(Visit visit) {
        return new ActiveVisitResponse(visit.getId(), visit.getPatient().getId(), visit.getPatient().getFullName(),
                visit.getAppointmentId(), visit.getVisitType(), visit.getStartTime(), encounters.findByVisit_Id(visit.getId())
                        .stream().map(Encounter::getEncounterType).toList());
    }

    private void validateObservation(Concept concept, ObsRequest value) {
        int supplied = (value.valueNumeric() != null ? 1 : 0) + (value.valueText() != null ? 1 : 0)
                + (value.valueCodedId() != null ? 1 : 0) + (value.valueDatetime() != null ? 1 : 0)
                + (value.valueBoolean() != null ? 1 : 0);
        boolean valid = switch (concept.getDatatype()) {
            case NUMERIC -> value.valueNumeric() != null && value.valueText() == null && value.valueCodedId() == null
                    && value.valueDatetime() == null && value.valueBoolean() == null
                    && (concept.getMinValue() == null || value.valueNumeric() >= concept.getMinValue())
                    && (concept.getMaxValue() == null || value.valueNumeric() <= concept.getMaxValue());
            case TEXT -> value.valueText() != null && !value.valueText().isBlank() && value.valueNumeric() == null
                    && value.valueCodedId() == null && value.valueDatetime() == null && value.valueBoolean() == null;
            case CODED -> value.valueCodedId() != null && value.valueNumeric() == null && value.valueText() == null
                    && value.valueDatetime() == null && value.valueBoolean() == null;
            case BOOLEAN -> value.valueBoolean() != null && value.valueNumeric() == null && value.valueText() == null
                    && value.valueCodedId() == null && value.valueDatetime() == null;
            case DATETIME -> value.valueDatetime() != null && value.valueNumeric() == null && value.valueText() == null
                    && value.valueCodedId() == null && value.valueBoolean() == null;
        };
        if (!valid || supplied != 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị Obs không khớp với kiểu dữ liệu Concept " + concept.getName() + ".");
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
