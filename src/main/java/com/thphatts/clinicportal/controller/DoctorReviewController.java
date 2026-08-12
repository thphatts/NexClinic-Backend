package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.DoctorReviewRequest;
import com.thphatts.clinicportal.dto.response.DoctorReviewResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.service.DoctorReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctor-reviews")
@RequiredArgsConstructor
public class DoctorReviewController extends BaseController {

    private final DoctorReviewService doctorReviewService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<DoctorReviewResponse> createReview(
            @Valid @RequestBody DoctorReviewRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ApiResponse.success(doctorReviewService.createReview(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<DoctorReviewResponse> updateReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody DoctorReviewRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ApiResponse.success(doctorReviewService.updateReview(request, currentUser, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ApiResponse<String> deleteReview(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        doctorReviewService.deleteReview(id, currentUser);
        return ApiResponse.success("Xóa đánh giá thành công!");
    }

    @GetMapping("/doctor/{doctorId}")
    public ApiResponse<PagedResponse<DoctorReviewResponse>> getReviewsByDoctor(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam(value = "page_no", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int size,
            @RequestParam(value = "sort_by", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "desc") String sortDir
    ) {
        int pageIndex = page > 0 ? page - 1 : 0;
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageIndex, size, sort);

        return ApiResponse.success(doctorReviewService.getReviewsByDoctor(doctorId, pageable));
    }
}
