package com.thphatts.clinicportal.modules.user.service.impl;

import com.thphatts.clinicportal.config.security.JwtTokenProvider;
import com.thphatts.clinicportal.modules.user.dto.LoginRequest;
import com.thphatts.clinicportal.modules.user.dto.RegisterRequest;
import com.thphatts.clinicportal.modules.user.dto.AuthResponse;
import com.thphatts.clinicportal.modules.user.dto.AuthResult;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.patient.repository.PatientRepository;
import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import com.thphatts.clinicportal.modules.user.service.AuthService;
import com.thphatts.clinicportal.modules.user.service.RefreshTokenService;
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
    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        String citizenId = normalizeOptional(request.getCitizenId());
        if (citizenId == null || !citizenId.matches("\\d{12}")) {
            throw new IllegalArgumentException("CCCD không hợp lệ (phải đủ 12 chữ số)");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username đã tồn tại trong hệ thống: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được đăng ký: " + email);
        }

        String phone = normalizePhone(request.getPhone());
        String address = (request.getAddress() != null && !request.getAddress().isBlank()) ? request.getAddress().trim() : null;
        if (citizenId != null && userRepository.existsByCitizenId(citizenId)) {
            throw new RuntimeException("Số CCCD đã được đăng ký: " + citizenId);
        }

        Person personByPhone = personRepository.findByPhoneNumber(phone).orElse(null);
        Person personByCitizenId = personRepository.findByCitizenId(citizenId).orElse(null);
        if (personByPhone != null && personByCitizenId != null
                && !personByPhone.getId().equals(personByCitizenId.getId())) {
            throw new RuntimeException("Số điện thoại và CCCD thuộc về hai hồ sơ khác nhau.");
        }
        Person person = personByPhone != null ? personByPhone : personByCitizenId;
        if (person != null && userRepository.existsByPerson_Id(person.getId())) {
            throw new RuntimeException("Hồ sơ này đã được liên kết với một tài khoản.");
        }
        if (person != null && citizenId != null && person.getCitizenId() != null
                && !person.getCitizenId().equals(citizenId)) {
            throw new RuntimeException("Số CCCD không khớp với hồ sơ đã có.");
        }
        if (person != null && person.getPhoneNumber() != null && !person.getPhoneNumber().equals(phone)) {
            throw new RuntimeException("Số điện thoại không khớp với hồ sơ đã có.");
        }
        if (person != null) {
            if (person.getPhoneNumber() == null) person.setPhoneNumber(phone);
            if (person.getCitizenId() == null) person.setCitizenId(citizenId);
            if (person.getEmail() == null) person.setEmail(email);
        }
        if (person == null) {
            person = new Person();
            person.setFullName(request.getName().trim());
            person.setBirthdate(request.getDob());
            person.setGender(normalizeGender(request.getGender()));
            person.setPhoneNumber(phone);
            person.setEmail(email);
            person.setAddress(address);
            person.setCitizenId(citizenId);
        }

        Role role = Role.ROLE_PATIENT;

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .person(person)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Keep the clinical record ID stable when a returning patient claims their verified phone.
        if (patientRepository.findByPerson_Id(person.getId()).isEmpty()) {
            patientRepository.save(Patient.builder()
                    .person(person)
                    .patientCode("PT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build());
        }

        String token = tokenProvider.generateToken(savedUser);
        String rawRefreshToken = refreshTokenService.createRefreshToken(savedUser.getId());
        AuthResponse response =  AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .expiresInMs(tokenProvider.getJwtExpirationMs())
                .build();
        return new AuthResult(response, rawRefreshToken);
    }

    @Override
    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + request.getUsername())));

        String token = tokenProvider.generateToken(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user.getId());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresInMs(tokenProvider.getJwtExpirationMs())
                .build();
        return new AuthResult(response, rawRefreshToken);
    }

    @Override
    public AuthResult refresh(String rawRefreshToken) {
        String userId = refreshTokenService.validateAndRotate(rawRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        String newAccessToken = tokenProvider.generateToken(user);
        String newRawRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponse response = AuthResponse.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresInMs(tokenProvider.getJwtExpirationMs())
                .build();

        return new AuthResult(response, newRawRefreshToken);
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Số điện thoại không được để trống.");
        return phone.replaceAll("[\\s()-]", "").trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) return null;
        return switch (gender.trim().toUpperCase()) {
            case "M", "MALE", "NAM" -> "M";
            case "F", "FEMALE", "NU", "NỮ" -> "F";
            case "O", "OTHER", "KHAC", "KHÁC" -> "O";
            default -> "U";
        };
    }
}
