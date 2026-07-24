package com.thphatts.clinicportal.dto.record;

import org.hibernate.validator.constraints.UniqueElements;

import com.thphatts.clinicportal.annotation.Cccd;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotEmpty(message = "field do not null ")
        @NotNull(message = "this field do not null")
        @NotBlank(message = "this field do not null")
        String name,
        @Min(value=0,message = "value must to > 0")
        @Max(value = 9, message = "value must to < 9")
        String address,
        @Size(min = 9, max = 11, message = "number phone must rage from 9 to 11")
        String phone,
        @Email
        @UniqueElements
        String email,
        String username,
        String password,
        @Cccd
        String cccd
) {
}
