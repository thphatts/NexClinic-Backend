package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.MedicalRecordRequest;
import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request);

    MedicalRecordResponse getMedicalRecordById(Long id);

    MedicalRecordResponse getMedicalRecordByAppointmentId(Long appointmentId);

    List<MedicalRecordResponse> getMedicalRecordsByPatientId(Long patientId);

    List<MedicalRecordResponse> getMedicalRecordsByDoctorId(Long doctorId);

    PagedResponse<MedicalRecordResponse> getAllMedicalRecords(Pageable pageable);
}
