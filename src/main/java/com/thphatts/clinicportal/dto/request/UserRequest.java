package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class UserRequest {
    @NotEmpty(message = "field do not null ")
    @NotNull
    @NotBlank(message = "this field do not null")
    private String name;
    @Min(value=0,message = "value must to > 0")
    @Max(value = 9, message = "value must to < 9")
    private String address;
    @Size(min = 9, max = 11, message = "number phone must rage from 9 to 11")
    private String phone;
    @Email
    @UniqueElements
    private String email;
    private String username;
    private String password;
}
