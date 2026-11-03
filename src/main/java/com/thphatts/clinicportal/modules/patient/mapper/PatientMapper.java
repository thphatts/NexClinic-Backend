package com.thphatts.clinicportal.modules.patient.mapper;

import com.thphatts.clinicportal.modules.patient.dto.PatientRequest;
import com.thphatts.clinicportal.modules.patient.dto.PatientResponse;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientMapper {
    public PatientResponse toResponse(Patient patient) {
        List<String> diagnoses = patient.getCheckups() == null ? List.of() : patient.getCheckups().stream()
                .map(c -> c.getDiagnoses()).filter(d -> d != null && !d.isBlank()).toList();
        return new PatientResponse(patient.getId(), patient.getFullName(), patient.getCitizenId(), patient.getPhone(),
                patient.getEmail(), patient.getDob(), patient.getGender(), patient.getAddress(), patient.getUserId(),
                patient.getCreatedAt(), diagnoses);
    }

    public Patient toEntity(PatientRequest request) {
        Patient patient = new Patient();
        updateEntityFromRequest(request, patient);
        return patient;
    }

    public void updateEntityFromRequest(PatientRequest request, Patient patient) {
        if (patient.getPerson() == null) patient.setPerson(new com.thphatts.clinicportal.modules.person.entity.Person());
        var person = patient.getPerson();
        person.setFullName(request.getFullName());
        person.setCitizenId(request.getCitizenId());
        person.setPhoneNumber(request.getPhone());
        person.setEmail(request.getEmail());
        person.setBirthdate(request.getDob());
        person.setGender(normalizeGender(request.getGender()));
        person.setAddress(request.getAddress());
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) return "U";
        return switch (gender.trim().toUpperCase()) {
            case "M", "MALE", "NAM" -> "M";
            case "F", "FEMALE", "NU", "NỮ" -> "F";
            case "O", "OTHER", "KHAC", "KHÁC" -> "O";
            default -> "U";
        };
    }
}
