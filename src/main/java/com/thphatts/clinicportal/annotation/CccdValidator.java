package com.thphatts.clinicportal.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CccdValidator implements ConstraintValidator<Cccd, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || value.matches("\\d{12}");
    }
}
