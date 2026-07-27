package com.thphatts.clinicportal.dto.request;

import com.thphatts.clinicportal.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Tên không được để trống")
        String name,

        String address,

        @Size(min = 9, max = 15, message = "Số điện thoại phải từ 9 đến 15 ký tự")
        String phone,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Username không được để trống")
        @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
        String password,

        Role role
) {}
