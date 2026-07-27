package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Prescription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @EntityGraph(attributePaths = {"medicalRecord", "items", "items.product"})
    Optional<Prescription> findByMedicalRecordId(Long medicalRecordId);
}
