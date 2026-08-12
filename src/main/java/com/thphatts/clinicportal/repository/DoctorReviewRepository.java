package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.DoctorReview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
@Repository
public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {

    Page<DoctorReview> findByDoctorIdAndDeletedFalse(Long doctorId, Pageable pageable);

    boolean existsByAppointmentIdAndDeletedFalse(Long appointmentId);

    Optional<DoctorReview> findByPatientIdAndAppointmentIdAndDeletedFalse(Long patientId, Long appointmentId);

    @Query("SELECT AVG(r.rating) FROM DoctorReview r WHERE r.doctor.id = :doctorId AND r.deleted = false")
Double getAverageRatingByDoctorId(@Param("doctorId") Long doctorId);
Long countByDoctorIdAndDeletedFalse(Long doctorId);
}
