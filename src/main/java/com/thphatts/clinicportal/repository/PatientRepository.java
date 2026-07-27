package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Patient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient,Long> {
    @Query(value = "SELECT p FROM Patient p JOIN FETCH p.checkups")
    List<Patient> findAllWithCheckUp();
    @EntityGraph(attributePaths = {"checkups"})
    @Query(value = "SELECT p FROM Patient p")
    List<Patient> findAllWithCheckUpOptimized();
}
