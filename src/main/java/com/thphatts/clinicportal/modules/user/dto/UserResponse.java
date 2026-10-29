package com.thphatts.clinicportal.modules.user.dto;

public record UserResponse(
        String id,
        String name,
        String phone,
        String email,
        String username,
        String address,
        String citizenId
) {
}
