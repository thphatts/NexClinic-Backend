package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.PatientRequest;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.PatientResponse;
import com.thphatts.clinicportal.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
    @RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController extends BaseController {

    private final PatientService patientService;

    @GetMapping("/me")
    public ApiResponse<PatientResponse> getMyPatientProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(patientService.getMyPatientProfile(currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    public ApiResponse<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        return ApiResponse.success(patientService.createPatient(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    public ApiResponse<PagedResponse<PatientResponse>> getAllPatients(
            @RequestParam(value = "page_no", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "asc") String sortDir,
            @RequestParam(value = "search", required = false) String search
    ) {
        int pageIndex = page > 0 ? page - 1 : 0;
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageIndex, size, sort);

        return ApiResponse.success(patientService.getAllPatients(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    public ApiResponse<PatientResponse> getPatientById(@PathVariable("id") Long id) {
        return ApiResponse.success(patientService.getPatientById(id));
    }

    @GetMapping("/citizen-id/{citizenId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    public ApiResponse<PatientResponse> getPatientByCitizenId(@PathVariable("citizenId") String citizenId) {
        return ApiResponse.success(patientService.getPatientByCitizenId(citizenId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'STAFF')")
    public ApiResponse<PatientResponse> updatePatient(
            @PathVariable("id") Long id,
            @Valid @RequestBody PatientRequest request
    ) {
        return ApiResponse.success(patientService.updatePatient(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deletePatient(@PathVariable("id") Long id) {
        patientService.deletePatient(id);
        return ApiResponse.success("Xóa thông tin bệnh nhân thành công (ID: " + id + ")");
    }
}
