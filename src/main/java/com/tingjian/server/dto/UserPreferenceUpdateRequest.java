package com.tingjian.server.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserPreferenceUpdateRequest(
        @NotNull Boolean largeText,
        @NotBlank @Pattern(regexp = "自动|中文|English") String voiceMode,
        @NotBlank @Pattern(regexp = "自然|清晰|舒缓") String voiceStyle,
        @NotNull @DecimalMin("0.5") @DecimalMax("2.0") Double ttsSpeed,
        @NotBlank @Pattern(regexp = "中英混合|中文|English") String recognitionLanguage,
        @NotNull Boolean keywordVibration,
        @NotNull Boolean keywordHighlight,
        @NotNull Boolean autoSummary) {
}
