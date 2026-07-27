package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.PatientRequest;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.PatientResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    PatientResponse updatePatient(Long id, PatientRequest request);

    PatientResponse getPatientById(Long id);

    PagedResponse<PatientResponse> getAllPatients(String search, Pageable pageable);

    void deletePatient(Long id);

    List<PatientResponse> getPatientsWithNPlusOne();
}

