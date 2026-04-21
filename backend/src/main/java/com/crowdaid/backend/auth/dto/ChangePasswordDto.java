package com.crowdaid.backend.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(max = 255) String securityQuestion,
    @NotBlank @Size(max = 255) String securityAnswer,
    @NotBlank @Size(min = 6) String newPassword,
    @NotBlank String confirmPassword
) {
}