package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPerson_Id(Long personId);

    @Query("select p from Patient p join p.person.attributes a where a.attributeType = 'CITIZEN_ID' and a.value = :value")
    Optional<Patient> findByCitizenId(@Param("value") String citizenId);

    @Query("select p from Patient p join p.person.attributes a where a.attributeType = 'PHONE' and a.value = :value")
    Optional<Patient> findByPhone(@Param("value") String phone);

    @Query("select p from Patient p join p.person.attributes a where a.attributeType = 'EMAIL' and a.value = :value")
    Optional<Patient> findByEmail(@Param("value") String email);

    @Query("select p from Patient p join User u on u.person = p.person where u.id = :userId")
    Optional<Patient> findByUserId(@Param("userId") String userId);

    @Query("select count(p) > 0 from Patient p join User u on u.person = p.person where p.id = :patientId and u.id = :userId")
    boolean belongsToUser(@Param("patientId") Long patientId, @Param("userId") String userId);

    @Query("select count(p) > 0 from Patient p join p.person.attributes a where a.attributeType = 'CITIZEN_ID' and a.value = :value")
    boolean existsByCitizenId(@Param("value") String citizenId);

    @Query("select count(p) > 0 from Patient p join p.person.attributes a where a.attributeType = 'PHONE' and a.value = :value")
    boolean existsByPhone(@Param("value") String phone);

    @Query("select count(p) > 0 from Patient p join p.person.attributes a where p.id <> :id and a.attributeType = 'CITIZEN_ID' and a.value = :value")
    boolean existsByCitizenIdAndIdNot(@Param("value") String citizenId, @Param("id") Long id);

    @Query("select count(p) > 0 from Patient p join p.person.attributes a where p.id <> :id and a.attributeType = 'PHONE' and a.value = :value")
    boolean existsByPhoneAndIdNot(@Param("value") String phone, @Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Patient p LEFT JOIN p.person.names n LEFT JOIN p.person.attributes a WHERE " +
           "LOWER(CONCAT(COALESCE(n.givenName, ''), ' ', COALESCE(n.middleName, ''), ' ', COALESCE(n.familyName, ''))) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(a.attributeType = 'CITIZEN_ID' AND a.value LIKE CONCAT('%', :keyword, '%')) OR " +
           "(a.attributeType = 'PHONE' AND a.value LIKE CONCAT('%', :keyword, '%'))")
    Page<Patient> searchPatients(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p FROM Patient p JOIN FETCH p.checkups")
    List<Patient> findAllWithCheckUp();

    @EntityGraph(attributePaths = {"checkups"})
    @Query(value = "SELECT p FROM Patient p")
    List<Patient> findAllWithCheckUpOptimized();
}
