package com.thphatts.clinicportal.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CccdValidatorTest {

    private final CccdValidator validator = new CccdValidator();

    @Test
    void acceptsMissingOptionalCitizenId() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    void requiresTwelveDigitsWhenCitizenIdIsProvided() {
        assertTrue(validator.isValid("079123456789", null));
        assertFalse(validator.isValid("079123", null));
        assertFalse(validator.isValid("07912345678A", null));
    }
}
