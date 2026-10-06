package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SmsPasswordResetRequest(
        @NotBlank @Pattern(regexp = "\\+[1-9]\\d{7,14}") String phone,
        @NotBlank @Size(max = 36) String verificationId,
        @NotBlank @Pattern(regexp = "\\d{6}") String verificationCode,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {
}
