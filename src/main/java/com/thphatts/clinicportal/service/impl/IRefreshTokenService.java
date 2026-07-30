package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.entity.RefreshToken;
import com.thphatts.clinicportal.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IRefreshTokenService implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long REFRESH_TOKEN_DAYS = 7;

    public String createRefreshToken(String userId) {
        byte[] randomBytes = new byte[64];
        RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiryDate(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(entity);

        return rawToken; // chỉ tồn tại 1 lần duy nhất ở đây, DB chỉ lưu bản băm
    }

    // Kiểm tra hợp lệ, xoay vòng (revoke cái cũ), trả về userId để service gọi tiếp
    public String validateAndRotate(String rawToken) {
        RefreshToken saved = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (saved.isRevoked() || saved.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.revokeAllByUserId(saved.getUserId());
            throw new RuntimeException("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại");
        }

        saved.setRevoked(true);
        refreshTokenRepository.save(saved);
        return saved.getUserId();
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
        }
    }
}