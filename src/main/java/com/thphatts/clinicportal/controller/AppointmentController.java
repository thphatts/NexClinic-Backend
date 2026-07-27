package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.AppointmentRequest;
import com.thphatts.clinicportal.dto.response.AppointmentResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.AppointmentStatus;
import com.thphatts.clinicportal.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController extends BaseController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        return createdSuccessResponse(appointmentService.createAppointment(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        return ApiResponse.success(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<AppointmentResponse>> filterAppointments(
            @RequestParam(name = "doctor_id", required = false) Long doctorId,
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "status", required = false) AppointmentStatus status,
            @RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page_no", defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "appointmentDate") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);

        return ApiResponse.success(appointmentService.filterAppointments(
                doctorId, patientId, status, fromDate, toDate, pageable
        ));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AppointmentResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestParam(name = "status") AppointmentStatus status,
            @RequestParam(name = "notes", required = false) String notes
    ) {
        return ApiResponse.success(appointmentService.updateAppointmentStatus(id, status, notes));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ApiResponse.success("Đã hủy lịch hẹn có ID: " + id + " thành công.");
    }
}
