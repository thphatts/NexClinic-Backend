package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.AppointmentRequest;
import com.thphatts.clinicportal.dto.response.AppointmentResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
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
