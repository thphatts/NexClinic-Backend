package com.thphatts.clinicportal.modules.doctor.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorReviewRequest;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorReviewResponse;
import com.thphatts.clinicportal.common.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface DoctorReviewService {

    DoctorReviewResponse createReview(DoctorReviewRequest request, UserPrincipal currentUser);

     DoctorReviewResponse updateReview(DoctorReviewRequest request, UserPrincipal currentUser, Long reviewId);

    void deleteReview(Long reviewId, UserPrincipal currentUser);

    PagedResponse<DoctorReviewResponse> getReviewsByDoctor( Long doctorId, Pageable pageable);
}