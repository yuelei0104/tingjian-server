package com.tingjian.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 36) String verificationId,
        @NotBlank @Pattern(regexp = "\\d{6}") String verificationCode,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {
}
