package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {


    // @EntityGrab giúp ra lệnh cho spring sử dụng join fetch để lấy các thông tin được khai báo trong attribuiePathsa
    // khắc phục lỗi hiệu năng N+1 Query


    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "prescription", "prescription.items", "prescription.items.product"})
    Optional<MedicalRecord> findById(Long id);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "prescription", "prescription.items", "prescription.items.product"})
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "prescription"})
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "prescription"})
    List<MedicalRecord> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "prescription"})
    Page<MedicalRecord> findAll(Pageable pageable);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByPatientIdAndDoctorUserId(Long patientId, String doctorUserId);
}
