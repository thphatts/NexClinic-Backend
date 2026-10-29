package com.thphatts.clinicportal.modules.user.service;

import com.thphatts.clinicportal.modules.user.dto.LoginRequest;
import com.thphatts.clinicportal.modules.user.dto.RegisterRequest;
import com.thphatts.clinicportal.modules.user.dto.AuthResponse;
import com.thphatts.clinicportal.modules.user.dto.AuthResult;

public interface AuthService {
    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
    AuthResult refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
