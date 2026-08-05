package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.MedicalRecord;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.MedicalRecordRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("medicalRecordSecurity")
@RequiredArgsConstructor
public class MedicalRecordSecurity {

    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public boolean canAccessRecord(Long recordId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username));

        if (userOpt.isEmpty()) {
            return false;
        }

        User currentUser = userOpt.get();

        // 1. ADMIN có quyền truy cập mọi hồ sơ
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        Optional<MedicalRecord> recordOpt = medicalRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        MedicalRecord record = recordOpt.get();

        // 2. Kiểm tra nếu là BÁC SĨ (ROLE_DOCTOR): Phải là bác sĩ phụ trách hồ sơ này
        if (currentUser.getRole() == Role.ROLE_DOCTOR) {
            Doctor doctor = record.getDoctor();
            if (doctor != null && doctor.getUser() != null) {
                return doctor.getUser().getId().equals(currentUser.getId());
            }
            return false;
        }

        // 3. Kiểm tra nếu là BỆNH NHÂN (ROLE_PATIENT): Phải là chính bệnh nhân sở hữu hồ sơ này
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Patient patient = record.getPatient();
            if (patient != null && patient.getUserId() != null) {
                return patient.getUserId().equals(currentUser.getId());
            }
            return false;
        }

        return false;
    }

    public boolean canAccessPatientRecords(Long patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username));

        if (userOpt.isEmpty()) {
            return false;
        }

        User currentUser = userOpt.get();
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_DOCTOR) {
            return true;
        }

        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            return patientOpt.isPresent() && currentUser.getId().equals(patientOpt.get().getUserId());
        }

        return false;
    }
}
