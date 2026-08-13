package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByCitizenId(String citizenId);

    Optional<Patient> findByUserId(String userId);

    Optional<Patient> findByPhone(String phone);

    Optional<Patient> findByEmail(String email);

    boolean existsByCitizenId(String citizenId);

    boolean existsByPhone(String phone);

    boolean existsByCitizenIdAndIdNot(String citizenId, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "p.citizenId LIKE CONCAT('%', :keyword, '%') OR " +
           "p.phone LIKE CONCAT('%', :keyword, '%')")
    Page<Patient> searchPatients(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p FROM Patient p JOIN FETCH p.checkups")
    List<Patient> findAllWithCheckUp();

    @EntityGraph(attributePaths = {"checkups"})
    @Query(value = "SELECT p FROM Patient p")
    List<Patient> findAllWithCheckUpOptimized();
}
