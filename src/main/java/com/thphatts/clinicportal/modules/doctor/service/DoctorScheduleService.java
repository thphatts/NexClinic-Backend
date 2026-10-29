package com.thphatts.clinicportal.modules.doctor.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleRequest;
import com.thphatts.clinicportal.modules.appointment.dto.AvailableSlotResponse;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface DoctorScheduleService {

    DoctorScheduleResponse createSchedule(Long doctorId, DoctorScheduleRequest request, UserPrincipal currentUser);

    List<DoctorScheduleResponse> getSchedulesByDoctor(Long doctorId);

    void deleteSchedule(Long doctorId, Long scheduleId, UserPrincipal currentUser);

    void addLeave(Long doctorId, LocalDate date, String reason, UserPrincipal currentUser);

    List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);
}