package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("doctorScheduleSecurity")
@RequiredArgsConstructor
public class DoctorScheduleSecurity {
    private final DoctorRepository doctorRepository;

    public boolean isOwner(Long doctorId, UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }
        if (currentUser.getRole() != Role.ROLE_DOCTOR) {
            return false;
        }

        return doctorRepository.findUserIdByDoctorId(doctorId)
                .map(userId -> userId.equals(currentUser.getUserId()))
                .orElse(false);
    }
}
