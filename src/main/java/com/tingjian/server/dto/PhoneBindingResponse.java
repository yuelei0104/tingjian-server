package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record PhoneBindingResponse(
        boolean bound,
        String maskedPhone,
        LocalDateTime verifiedAt) {
}
