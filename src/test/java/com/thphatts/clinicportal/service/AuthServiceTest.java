package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.config.security.JwtTokenProvider;
import com.thphatts.clinicportal.dto.request.LoginRequest;
import com.thphatts.clinicportal.dto.request.RegisterRequest;
import com.thphatts.clinicportal.dto.response.AuthResult;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("user-uuid-001")
                .name("Nguyễn Văn Test")
                .username("testuser")
                .email("test@clinic.com")
                .password("encoded_password")
                .role(Role.ROLE_PATIENT)
                .build();

        // RegisterRequest dùng @Data → có setter
        registerRequest = RegisterRequest.builder()
                .name("Nguyễn Văn Test")
                .username("testuser")
                .email("test@clinic.com")
                .password("password123")
                .phone("0901234567")
                .citizenId("079123456789")
                .build();

        // LoginRequest dùng @Data → có setter
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }

    // =========================================================
    // REGISTER TESTS
    // =========================================================
    @Nested
    @DisplayName("Đăng ký tài khoản (Register)")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công - trả về token và tạo hồ sơ bệnh nhân mới")
        void register_Success_WithNewPatientProfile() {
            // Arrange
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@clinic.com")).thenReturn(false);
            when(userRepository.existsByCitizenId("079123456789")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(patientRepository.findByCitizenId("079123456789")).thenReturn(Optional.empty());
            when(tokenProvider.generateToken(mockUser)).thenReturn("jwt_access_token");
            when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
            when(refreshTokenService.createRefreshToken("user-uuid-001")).thenReturn("raw_refresh_token");

            // Act
            AuthResult result = authService.register(registerRequest);

            // Assert
            assertNotNull(result);
            assertEquals("jwt_access_token", result.response().getToken());
            assertEquals("raw_refresh_token", result.rawRefreshToken());
            assertEquals("testuser", result.response().getUsername());
            assertEquals(Role.ROLE_PATIENT, result.response().getRole());

            verify(userRepository).save(any(User.class));
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - liên kết với hồ sơ bệnh nhân có sẵn (CitizenId match)")
        void register_Success_LinkingExistingPatient() {
            // Arrange
            Patient existingPatient = Patient.builder()
                    .id(1L)
                    .fullName("Nguyễn Văn Test")
                    .citizenId("079123456789")
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@clinic.com")).thenReturn(false);
            when(userRepository.existsByCitizenId("079123456789")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);
            when(patientRepository.findByCitizenId("079123456789")).thenReturn(Optional.of(existingPatient));
            when(tokenProvider.generateToken(mockUser)).thenReturn("jwt_access_token");
            when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
            when(refreshTokenService.createRefreshToken("user-uuid-001")).thenReturn("raw_refresh_token");

            // Act
            AuthResult result = authService.register(registerRequest);

            // Assert
            assertNotNull(result);
            // Phải update patient cũ (save existingPatient), không tạo patient mới
            verify(patientRepository, times(1)).save(existingPatient);
            assertEquals("user-uuid-001", existingPatient.getUserId());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Username đã tồn tại")
        void register_Fail_UsernameAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertTrue(ex.getMessage().contains("Username đã tồn tại"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email đã được đăng ký")
        void register_Fail_EmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@clinic.com")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertTrue(ex.getMessage().contains("Email đã được đăng ký"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - CCCD đã được đăng ký")
        void register_Fail_CitizenIdAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@clinic.com")).thenReturn(false);
            when(userRepository.existsByCitizenId("079123456789")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertTrue(ex.getMessage().contains("Số CCCD đã được đăng ký"));
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================
    // LOGIN TESTS
    // =========================================================
    @Nested
    @DisplayName("Đăng nhập (Login)")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công bằng Username")
        void login_Success_WithUsername() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null); // auth thành công không ném exception
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
            when(tokenProvider.generateToken(mockUser)).thenReturn("jwt_token");
            when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);
            when(refreshTokenService.createRefreshToken("user-uuid-001")).thenReturn("refresh_token");

            // Act
            AuthResult result = authService.login(loginRequest);

            // Assert
            assertNotNull(result);
            assertEquals("jwt_token", result.response().getToken());
            assertEquals("Bearer", result.response().getTokenType());
            assertEquals("testuser", result.response().getUsername());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Sai mật khẩu")
        void login_Fail_BadCredentials() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class,
                    () -> authService.login(loginRequest));

            verify(userRepository, never()).findByUsername(any());
            verify(tokenProvider, never()).generateToken(any());
        }
    }

    // =========================================================
    // LOGOUT TESTS
    // =========================================================
    @Nested
    @DisplayName("Đăng xuất (Logout)")
    class LogoutTests {

        @Test
        @DisplayName("Logout thành công - revoke refresh token")
        void logout_Success() {
            // Arrange
            doNothing().when(refreshTokenService).revoke("raw_refresh_token");

            // Act
            authService.logout("raw_refresh_token");

            // Assert
            verify(refreshTokenService, times(1)).revoke("raw_refresh_token");
        }
    }
}
