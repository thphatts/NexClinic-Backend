package com.thphatts.clinicportal.modules.person.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorRepository;
import com.thphatts.clinicportal.modules.patient.dto.PatientProfileRequest;
import com.thphatts.clinicportal.modules.patient.dto.PatientResponse;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import com.thphatts.clinicportal.modules.patient.mapper.PatientMapper;
import com.thphatts.clinicportal.modules.patient.repository.PatientRepository;
import com.thphatts.clinicportal.modules.person.dto.PersonLookupResponse;
import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PatientMapper patientMapper;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ApiResponse<PersonLookupResponse> search(@RequestParam String keyword) {
        String value = keyword.trim();
        Person person = personRepository.findByCitizenId(value)
                .or(() -> personRepository.findByPhoneNumber(value))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người này."));
        Patient patient = patientRepository.findByPerson_Id(person.getId()).orElse(null);
        Long doctorId = doctorRepository.findByPerson_Id(person.getId()).map(Doctor::getId).orElse(null);
        return ApiResponse.success(new PersonLookupResponse(person.getId(), person.getFullName(), person.getBirthdate(),
                person.getGender(), person.getPhoneNumber(), person.getCitizenId(), person.getAddress(),
                new PersonLookupResponse.Profiles(patient == null ? null : patient.getId(),
                        patient == null ? null : patient.getPatientCode(), doctorId, person.getUser() != null)));
    }
    @PostMapping("/{personId}/patient-profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Transactional
    public ApiResponse<PatientResponse> addPatientProfile(@PathVariable Long personId,
            @Valid @RequestBody PatientProfileRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người này."));
        if (patientRepository.findByPerson_Id(personId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Người này đã có hồ sơ bệnh nhân.");
        Patient patient = Patient.builder().person(person)
                .patientCode("PT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .bloodType(request.bloodType()).medicalHistorySummary(request.medicalHistorySummary()).build();
        return ApiResponse.success(patientMapper.toResponse(patientRepository.save(patient)));
    }
}
