package com.thphatts.clinicportal.dto.record;

public record UserResponse(
        String id,
        String name,
        String phone,
        String email,
        String username,
        String address
) {
}
