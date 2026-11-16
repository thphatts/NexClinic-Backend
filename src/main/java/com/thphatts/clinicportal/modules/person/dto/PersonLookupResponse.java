package com.thphatts.clinicportal.modules.person.dto;

import java.time.LocalDate;

public record PersonLookupResponse(
        Long personId,
        String fullName,
        LocalDate dob,
        String gender,
        String phoneNumber,
        String citizenId,
        String address,
        Profiles profiles
) {
    public record Profiles(Long patientId, String patientCode, Long doctorId, boolean hasUserAccount) {}
}
