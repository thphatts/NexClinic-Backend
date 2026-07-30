package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.dto.request.LoginRequest;
import com.thphatts.clinicportal.dto.request.RegisterRequest;
import com.thphatts.clinicportal.dto.response.AuthResponse;
import com.thphatts.clinicportal.dto.response.AuthResult;
import com.thphatts.clinicportal.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpResponse) {
        AuthResult result = authService.register(request);
        setRefreshCookie(httpResponse, result.rawRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Đăng ký tài khoản thành công", result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        AuthResult result = authService.login(request);
        setRefreshCookie(httpResponse, result.rawRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(200, "Đăng nhập thành công", result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse httpResponse) {
        AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(httpResponse, result.rawRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(200, "Làm mới token thành công", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true).secure(true).path("/api/v1/auth").maxAge(0).build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok(new ApiResponse<>(200, "Đăng xuất thành công", null));
    }

    private void setRefreshCookie(HttpServletResponse httpResponse, String rawRefreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}