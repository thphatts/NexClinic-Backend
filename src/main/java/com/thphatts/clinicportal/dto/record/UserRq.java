package com.thphatts.clinicportal.dto.record;

public record UserRq(
        String name,
        String phone,
        String email,
        String username,
        String password,
        String address
) {
}
