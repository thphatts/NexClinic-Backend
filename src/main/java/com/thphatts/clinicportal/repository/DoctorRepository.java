package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(String userId);

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Doctor> findBySpecialization(String specialization);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "d.phone LIKE CONCAT('%', :search, '%')")
    Page<Doctor> searchDoctors(@Param("search") String search, Pageable pageable);

    @Query("SELECT d.user.id FROM Doctor d WHERE d.id = :doctorId")
    Optional<String> findUserIdByDoctorId(@Param("doctorId") Long doctorId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE doctors
        SET average_rating = COALESCE((SELECT AVG(rating) FROM doctor_reviews WHERE doctor_id = :doctorId AND deleted = false), 0),
            total_reviews = (SELECT COUNT(*) FROM doctor_reviews WHERE doctor_id = :doctorId AND deleted = false)
        WHERE id = :doctorId
        """, nativeQuery = true)
    void recalculateRatingStats(@Param("doctorId") Long doctorId);
}
