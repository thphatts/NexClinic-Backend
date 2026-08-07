package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.entity.MedicalRecord;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.MedicalRecordRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("medicalRecordSecurity")
@RequiredArgsConstructor
public class MedicalRecordSecurity {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public boolean canAccessRecord(Long recordId, UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        // 1. ADMIN có toàn quyền (sẽ thực hiện ghi Audit Log ở Lớp 6)
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        // 2. RECEPTIONIST không được xem nội dung bệnh án
        if (currentUser.getRole() == Role.ROLE_RECEPTIONIST) {
            return false;
        }

        Optional<MedicalRecord> recordOpt = medicalRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        MedicalRecord record = recordOpt.get();

        // 3. PATIENT: Chỉ xem khi record.patient.userId == currentUserId
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Patient patient = record.getPatient();
            return patient != null && patient.getUserId() != null && patient.getUserId().equals(currentUser.getUserId());
        }

        // 4. DOCTOR: Chỉ khi bác sĩ này đã từng có Appointment với đúng bệnh nhân của hồ sơ đó
        if (currentUser.getRole() == Role.ROLE_DOCTOR) {
            Patient patient = record.getPatient();
            if (patient == null || patient.getId() == null) {
                return false;
            }
            return appointmentRepository.existsByDoctorUserIdAndPatientId(currentUser.getUserId(), patient.getId());
        }

        return false;
    }

    public boolean canAccessPatientRecords(Long patientId, UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.ROLE_RECEPTIONIST) {
            return false;
        }

        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            return patientRepository.findById(patientId)
                    .map(patient -> currentUser.getUserId().equals(patient.getUserId()))
                    .orElse(false);
        }

        if (currentUser.getRole() == Role.ROLE_DOCTOR) {
            return appointmentRepository.existsByDoctorUserIdAndPatientId(currentUser.getUserId(), patientId);
        }

        return false;
    }
}

