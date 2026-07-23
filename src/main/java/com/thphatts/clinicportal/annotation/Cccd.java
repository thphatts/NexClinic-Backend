package com.thphatts.clinicportal.annotation;


import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CccdValidator.class)
public @interface Cccd {
    public String message() default "Số căn cước công dân phải gồm 12 chữ số!!";
}
