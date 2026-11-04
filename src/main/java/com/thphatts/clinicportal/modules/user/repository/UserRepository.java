package com.thphatts.clinicportal.modules.user.repository;

import com.thphatts.clinicportal.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByPerson_Id(Long personId);
    Optional<User> findByPerson_Id(Long personId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.person p JOIN p.attributes a " +
           "WHERE a.attributeType = 'CITIZEN_ID' AND a.value = :citizenId")
    boolean existsByCitizenId(@Param("citizenId") String citizenId);
}
