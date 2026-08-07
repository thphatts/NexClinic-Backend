package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.MedicalRecordRequest;
import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.MedicalRecord;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request);

    MedicalRecordResponse getMedicalRecordById(Long id, UserPrincipal currentUser);

    MedicalRecordResponse getMedicalRecordByAppointmentId(Long appointmentId, UserPrincipal currentUser);

    List<MedicalRecordResponse> getMedicalRecordsByPatientId(Long patientId, UserPrincipal currentUser);

    List<MedicalRecordResponse> getMedicalRecordsByDoctorId(Long doctorId, UserPrincipal currentUser);

    PagedResponse<MedicalRecordResponse> getAllMedicalRecords(Pageable pageable, UserPrincipal currentUser);

    void checkReadAccess(MedicalRecord record, UserPrincipal currentUser);
}

