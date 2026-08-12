package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.DoctorReviewRequest;
import com.thphatts.clinicportal.dto.response.DoctorReviewResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface DoctorReviewService {

    DoctorReviewResponse createReview(DoctorReviewRequest request, UserPrincipal currentUser);

     DoctorReviewResponse updateReview(DoctorReviewRequest request, UserPrincipal currentUser, Long reviewId);

    void deleteReview(Long reviewId, UserPrincipal currentUser);

    PagedResponse<DoctorReviewResponse> getReviewsByDoctor( Long doctorId, Pageable pageable);
}