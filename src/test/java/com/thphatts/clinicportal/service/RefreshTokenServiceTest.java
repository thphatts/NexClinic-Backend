package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.entity.RefreshToken;
import com.thphatts.clinicportal.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private IRefreshTokenService refreshTokenService;

    private final String userId = "user-uuid-12345";

    @Nested
    @DisplayName("1. Test case: Tạo Refresh Token")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Tạo mới refresh token thành công và lưu bản băm SHA-256 vào DB")
        void createRefreshToken_Success() {
            // Act
            String rawToken = refreshTokenService.createRefreshToken(userId);

            // Assert
            assertNotNull(rawToken);
            assertFalse(rawToken.isBlank());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(1)).save(captor.capture());

            RefreshToken savedEntity = captor.getValue();
            assertEquals(userId, savedEntity.getUserId());
            assertNotNull(savedEntity.getTokenHash());
            assertFalse(savedEntity.isRevoked());
            assertTrue(savedEntity.getExpiryDate().isAfter(Instant.now()));
        }
    }

    @Nested
    @DisplayName("2. Test case: Token hợp lệ (Valid Token)")
    class ValidTokenTests {

        @Test
        @DisplayName("Validate và xoay vòng (rotate) token hợp lệ thành công")
        void validateAndRotate_ValidToken_Success() {
            // Arrange: Tạo 1 raw token để dịch băm và mock trả về RefreshToken hợp lệ
            String rawToken = refreshTokenService.createRefreshToken(userId);
            reset(refreshTokenRepository); // Reset mock để kiểm tra thao tác validate sau đó

            RefreshToken existingToken = RefreshToken.builder()
                    .id("token-id-1")
                    .userId(userId)
                    .tokenHash("some-hash")
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existingToken));

            // Act
            String returnedUserId = refreshTokenService.validateAndRotate(rawToken);

            // Assert
            assertEquals(userId, returnedUserId);
            verify(refreshTokenRepository, times(1)).save(existingToken);
            assertTrue(existingToken.isRevoked(), "Token cũ phải chuyển sang trạng thái revoked = true sau khi rotate");
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }
    }

    @Nested
    @DisplayName("3. Test case: Token hết hạn (Expired Token)")
    class ExpiredTokenTests {

        @Test
        @DisplayName("Token hết hạn sẽ bị từ chối và tự động thu hồi toàn bộ token của User")
        void validateAndRotate_ExpiredToken_ThrowsException() {
            // Arrange
            String rawToken = "raw-expired-token";
            RefreshToken expiredToken = RefreshToken.builder()
                    .id("token-id-2")
                    .userId(userId)
                    .tokenHash("hash-expired")
                    .expiryDate(Instant.now().minus(1, ChronoUnit.DAYS)) // Đã hết hạn từ 1 ngày trước
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    refreshTokenService.validateAndRotate(rawToken)
            );

            assertEquals("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại", exception.getMessage());
            // Cần thu hồi toàn bộ token của User khi phát hiện dấu hiệu bất thường / hết hạn
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. Test case: Token bị dùng lại sau khi đã revoke (Reused / Revoked Token)")
    class RevokedTokenTests {

        @Test
        @DisplayName("Token đã bị revoke trước đó nếu bị dùng lại sẽ bị phát hiện (Ném lỗi & Revoke toàn bộ token)")
        void validateAndRotate_RevokedToken_ReuseAttackDetected() {
            // Arrange
            String rawToken = "raw-reused-token";
            RefreshToken reusedToken = RefreshToken.builder()
                    .id("token-id-3")
                    .userId(userId)
                    .tokenHash("hash-reused")
                    .expiryDate(Instant.now().plus(3, ChronoUnit.DAYS))
                    .revoked(true) // Đã bị revoke (đã từng dùng trước đó)
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(reusedToken));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    refreshTokenService.validateAndRotate(rawToken)
            );

            assertEquals("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại", exception.getMessage());
            // Bảo mật: Khi token đã revoked bị dùng lại -> Có nguy cơ tấn công Token Reuse -> Thu hồi toàn bộ session của User
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5. Test case: Token không tồn tại & Thu hồi chủ động (Revoke)")
    class OtherTokenTests {

        @Test
        @DisplayName("Truyền vào token không tồn tại trong hệ thống sẽ ném ngoại lệ")
        void validateAndRotate_NotFound_ThrowsException() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    refreshTokenService.validateAndRotate("invalid-token")
            );

            assertEquals("Refresh token không hợp lệ", exception.getMessage());
        }

        @Test
        @DisplayName("Thu hồi (logout) refresh token thành công")
        void revoke_Success() {
            RefreshToken activeToken = RefreshToken.builder()
                    .id("token-id-4")
                    .userId(userId)
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(activeToken));

            refreshTokenService.revoke("valid-raw-token");

            assertTrue(activeToken.isRevoked());
            verify(refreshTokenRepository, times(1)).save(activeToken);
        }
    }
}
