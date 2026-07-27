package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.config.security.JwtTokenProvider;
import com.thphatts.clinicportal.dto.request.LoginRequest;
import com.thphatts.clinicportal.dto.request.RegisterRequest;
import com.thphatts.clinicportal.dto.response.AuthResponse;
import com.thphatts.clinicportal.entity.Role;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.exception.BaseException;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại trong hệ thống: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được đăng ký: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_PATIENT;

        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhone())
                .address(request.getAddress())
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .expiresInMs(tokenProvider.getJwtExpirationMs())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + request.getUsername())));

        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresInMs(tokenProvider.getJwtExpirationMs())
                .build();
    }
}
