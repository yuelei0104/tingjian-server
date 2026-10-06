package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationRequest(
        @NotBlank @Pattern(regexp = "\\+[1-9]\\d{7,14}") String phone) {
}
