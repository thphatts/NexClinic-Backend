package com.thphatts.clinicportal.dto.response;

import com.thphatts.clinicportal.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private String email;
    private Role role;
    private long expiresInMs;
}
