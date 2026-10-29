package com.thphatts.clinicportal.modules.appointment.service;

import com.thphatts.clinicportal.modules.appointment.dto.AppointmentRequest;
import com.thphatts.clinicportal.modules.appointment.dto.AppointmentResponse;
import com.thphatts.clinicportal.common.dto.PagedResponse;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentRequest request);

    AppointmentResponse updateAppointmentStatus(Long id, AppointmentStatus status, String notes);

    AppointmentResponse getAppointmentById(Long id);

    PagedResponse<AppointmentResponse> filterAppointments(
            Long doctorId, Long patientId, AppointmentStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable
    );

    void cancelAppointment(Long id);
}
