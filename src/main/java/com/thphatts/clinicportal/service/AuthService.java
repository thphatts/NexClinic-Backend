package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.LoginRequest;
import com.thphatts.clinicportal.dto.request.RegisterRequest;
import com.thphatts.clinicportal.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
