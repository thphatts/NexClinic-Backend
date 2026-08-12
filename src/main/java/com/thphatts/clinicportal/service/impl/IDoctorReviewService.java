package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.DoctorReviewRequest;
import com.thphatts.clinicportal.dto.response.DoctorReviewResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.DoctorReview;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.mapper.DoctorReviewMapper;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.DoctorReviewRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.DoctorReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class IDoctorReviewService implements DoctorReviewService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorReviewRepository doctorReviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorReviewMapper doctorReviewMapper;

    @Override
    @Transactional
    public DoctorReviewResponse createReview(DoctorReviewRequest request, UserPrincipal currentUser) {
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + request.doctorId()));

        Patient patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân tương ứng với tài khoản!"));

        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + request.appointmentId()));

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("Bạn không có quyền đánh giá lịch hẹn của người khác!");
            }

            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new RuntimeException("Lịch hẹn này không thuộc về bác sĩ được đánh giá!");
            }

            if (doctorReviewRepository.existsByAppointmentIdAndDeletedFalse(request.appointmentId())) {
                throw new RuntimeException("Lịch hẹn này đã được tạo đánh giá trước đó!");
            }
        }

        DoctorReview review = doctorReviewMapper.toEntity(request);
        review.setPatient(patient);
        review.setDoctor(doctor);
        review.setAppointment(appointment);

        DoctorReview savedReview = doctorReviewRepository.save(review);

        updateDoctorRatingSummary(doctor.getId());

        return doctorReviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional
    public DoctorReviewResponse updateReview(DoctorReviewRequest request, UserPrincipal currentUser, Long reviewId) {
        DoctorReview review = doctorReviewRepository.findById(reviewId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId));

        Patient patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân tương ứng với tài khoản!"));

        if (review.getPatient() != null && !review.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đánh giá này!");
        }

        doctorReviewMapper.updateEntityFromRequest(request, review);

        DoctorReview updatedReview = doctorReviewRepository.save(review);

        updateDoctorRatingSummary(review.getDoctor().getId());

        return doctorReviewMapper.toResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, UserPrincipal currentUser) {
        DoctorReview review = doctorReviewRepository.findById(reviewId)
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId));

        Patient patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân tương ứng với tài khoản!"));

        if (review.getPatient() != null && !review.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này!");
        }

        review.setDeleted(true);
        doctorReviewRepository.save(review);

        updateDoctorRatingSummary(review.getDoctor().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DoctorReviewResponse> getReviewsByDoctor(Long doctorId, Pageable pageable) {
        Page<DoctorReview> reviewPage = doctorReviewRepository.findByDoctorIdAndDeletedFalse(doctorId, pageable);

        List<DoctorReviewResponse> items = reviewPage.getContent().stream()
                .map(doctorReviewMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                reviewPage.getNumber() + 1,
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.isLast()
        );
    }

    private void updateDoctorRatingSummary(Long doctorId) {
        Double avg = doctorReviewRepository.getAverageRatingByDoctorId(doctorId);
        Long count = doctorReviewRepository.countByDoctorIdAndDeletedFalse(doctorId);

        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor != null) {
            doctor.setAverageRating(avg != null ? BigDecimal.valueOf(avg) : BigDecimal.ZERO);
            doctor.setTotalReviews(count != null ? count.intValue() : 0);
            doctorRepository.save(doctor);
        }
    }
}

