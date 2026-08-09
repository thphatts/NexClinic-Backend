package com.thphatts.clinicportal.config.security;

import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("doctorScheduleSecurity")
@RequiredArgsConstructor
public class DoctorSchduleSecurity {
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public boolean isOwn(Long doctorId, UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null ) {
            return false;
        }
        if (currentUser.getRole() == Role.ROLE_ADMIN){
            return true;
        }
        if(currentUser.getRole() != Role.ROLE_DOCTOR) {
            return false;
        }

        return doctorRepository.findUserIdByDoctorId(doctorId).map(userId -> userId.equals(currentUser.getUserId()))
                .orElse(false);
    }
}
