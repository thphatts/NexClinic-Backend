package com.thphatts.clinicportal.dto.request;

public record UserResponse(
        String id,
        String name,
        String phone,
        String email,
        String username,
        String address
) {
}
