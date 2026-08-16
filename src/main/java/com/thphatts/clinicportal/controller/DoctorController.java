package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.request.DoctorRequest;
import com.thphatts.clinicportal.dto.response.DoctorResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController extends BaseController {

    private final DoctorService doctorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        return createdSuccessResponse(doctorService.createDoctor(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DoctorResponse> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request
    ) {
        return ApiResponse.success(doctorService.updateDoctor(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ApiResponse.success(doctorService.getDoctorById(id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<DoctorResponse>> getAllDoctors(
            @RequestParam(name = "page_no", defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "asc") String sortDir,
            @RequestParam(name = "search", required = false) String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);

        return ApiResponse.success(doctorService.getAllDoctors(search, pageable));
    }

    @GetMapping("/specialization/{specialization}")
    public ApiResponse<List<DoctorResponse>> getDoctorsBySpecialization(@PathVariable String specialization) {
        return ApiResponse.success(doctorService.getDoctorsBySpecialization(specialization));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ApiResponse.success("Đã xóa bác sĩ có ID: " + id + " thành công.");
    }
}
