package com.payment.backend.dto;

public record LoginRequest(
        @jakarta.validation.constraints.NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Must be a valid email")
        String email,

        @jakarta.validation.constraints.NotBlank(message = "Password is required")
        String password
) {}
