package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // Optional<T> dùng để bọc một giá trị mà nó có thể tồn tại hoặc không, tránh
    // lỗi nullpointer rõ r
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query(value = "UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(String userId);
}
