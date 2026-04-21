package com.crowdaid.backend.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    @NotBlank String phone,
    @Size(min = 6) String password,
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(max = 255) String securityQuestion,
    @NotBlank @Size(max = 255) String securityAnswer,
    @NotNull Boolean isVolunteer,
    @NotNull Boolean agreeToTerms,
    @NotNull Boolean agreeToEmergencyContact
) {
}