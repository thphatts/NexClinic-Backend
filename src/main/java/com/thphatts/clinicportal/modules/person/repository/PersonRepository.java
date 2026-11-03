package com.thphatts.clinicportal.modules.person.repository;

import com.thphatts.clinicportal.modules.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    @Query("select distinct p from Person p join p.attributes a where a.attributeType = 'PHONE' and a.value = :phone")
    Optional<Person> findByPhoneNumber(@Param("phone") String phone);

    @Query("select distinct p from Person p join p.attributes a where a.attributeType = 'CITIZEN_ID' and a.value = :citizenId")
    Optional<Person> findByCitizenId(@Param("citizenId") String citizenId);

    @Query("select count(p) > 0 from Person p join p.attributes a where a.attributeType = 'PHONE' and a.value = :phone")
    boolean existsByPhoneNumber(@Param("phone") String phone);

    @Query("select count(p) > 0 from Person p join p.attributes a where a.attributeType = 'CITIZEN_ID' and a.value = :citizenId")
    boolean existsByCitizenId(@Param("citizenId") String citizenId);
}
