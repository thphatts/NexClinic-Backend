package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.PatientRequest;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.PatientResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.thphatts.clinicportal.config.security.UserPrincipal;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    PatientResponse updatePatient(Long id, PatientRequest request);

    PatientResponse getPatientById(Long id);

    PatientResponse getPatientByCitizenId(String citizenId);

    PatientResponse getMyPatientProfile(UserPrincipal currentUser);

    PagedResponse<PatientResponse> getAllPatients(String search, Pageable pageable);

    void deletePatient(Long id);

    List<PatientResponse> getPatientsWithNPlusOne();
}
