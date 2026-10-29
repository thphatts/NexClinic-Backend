package com.thphatts.clinicportal.modules.patient.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordRequest;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordResponse;
import com.thphatts.clinicportal.common.dto.PagedResponse;
import com.thphatts.clinicportal.modules.patient.entity.MedicalRecord;
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

