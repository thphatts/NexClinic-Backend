package com.thphatts.clinicportal.modules.user.repository;

import com.thphatts.clinicportal.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByCitizenId(String citizenId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
}
