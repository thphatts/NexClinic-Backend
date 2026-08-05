package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.MedicalRecordRequest;
import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController extends BaseController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<MedicalRecordResponse> createMedicalRecord(@Valid @RequestBody MedicalRecordRequest request) {
        return ApiResponse.success(medicalRecordService.createMedicalRecord(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PagedResponse<MedicalRecordResponse>> getAllMedicalRecords(
            @RequestParam(value = "page_no", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "desc") String sortDir
    ) {
        int pageIndex = page > 0 ? page - 1 : 0;
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageIndex, size, sort);

        return ApiResponse.success(medicalRecordService.getAllMedicalRecords(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @medicalRecordSecurity.canAccessRecord(#id)")
    public ApiResponse<MedicalRecordResponse> getMedicalRecordById(@PathVariable("id") Long id) {
        return ApiResponse.success(medicalRecordService.getMedicalRecordById(id));
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<MedicalRecordResponse> getMedicalRecordByAppointmentId(@PathVariable("appointmentId") Long appointmentId) {
        return ApiResponse.success(medicalRecordService.getMedicalRecordByAppointmentId(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN') or @medicalRecordSecurity.canAccessPatientRecords(#patientId)")
    public ApiResponse<List<MedicalRecordResponse>> getMedicalRecordsByPatientId(@PathVariable("patientId") Long patientId) {
        return ApiResponse.success(medicalRecordService.getMedicalRecordsByPatientId(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ApiResponse<List<MedicalRecordResponse>> getMedicalRecordsByDoctorId(@PathVariable("doctorId") Long doctorId) {
        return ApiResponse.success(medicalRecordService.getMedicalRecordsByDoctorId(doctorId));
    }
}

