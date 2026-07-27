package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    java.util.Optional<User> findByUsername(String username);
    java.util.Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
