package com.thphatts.clinicportal.modules.doctor.repository;

import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
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

    Optional<Doctor> findByPerson_Id(Long personId);

    @Query("select d from Doctor d join d.person.attributes a where a.attributeType = 'EMAIL' and a.value = :email")
    Optional<Doctor> findByEmail(@Param("email") String email);

    @Query("select d from Doctor d join d.person.attributes a where a.attributeType = 'PHONE' and a.value = :phone")
    Optional<Doctor> findByPhone(@Param("phone") String phone);

    @Query("select count(d) > 0 from Doctor d join d.person.attributes a where a.attributeType = 'PHONE' and a.value = :phone")
    boolean existsByPhone(@Param("phone") String phone);

    @Query("select count(d) > 0 from Doctor d join d.person.attributes a where a.attributeType = 'EMAIL' and a.value = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("select count(d) > 0 from Doctor d join d.person.attributes a where d.id <> :id and a.attributeType = 'PHONE' and a.value = :phone")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("id") Long id);

    @Query("select count(d) > 0 from Doctor d join d.person.attributes a where d.id <> :id and a.attributeType = 'EMAIL' and a.value = :email")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);

    List<Doctor> findBySpecialization(String specialization);

    @Query("SELECT DISTINCT d FROM Doctor d LEFT JOIN d.person.names n WHERE " +
           "LOWER(CONCAT(COALESCE(n.givenName, ''), ' ', COALESCE(n.middleName, ''), ' ', COALESCE(n.familyName, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT a.id FROM PersonAttribute a WHERE a.person = d.person AND a.attributeType = 'PHONE' AND a.value LIKE CONCAT('%', :search, '%'))")
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
