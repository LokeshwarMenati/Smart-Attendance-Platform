package com.smartattendance.platform.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}

