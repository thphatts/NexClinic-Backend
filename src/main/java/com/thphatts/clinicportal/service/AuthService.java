package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.LoginRequest;
import com.thphatts.clinicportal.dto.request.RegisterRequest;
import com.thphatts.clinicportal.dto.response.AuthResponse;
import com.thphatts.clinicportal.dto.response.AuthResult;

public interface AuthService {
    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
    AuthResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
