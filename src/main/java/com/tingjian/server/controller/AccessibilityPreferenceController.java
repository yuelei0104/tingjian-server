package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.AccessibilityPreferenceResponse;
import com.tingjian.server.dto.AccessibilityPreferenceUpdateRequest;
import com.tingjian.server.service.AccessibilityPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accessibility/preferences")
public class AccessibilityPreferenceController {
    private final AccessibilityPreferenceService accessibilityPreferenceService;

    public AccessibilityPreferenceController(
            AccessibilityPreferenceService accessibilityPreferenceService) {
        this.accessibilityPreferenceService = accessibilityPreferenceService;
    }

    @GetMapping
    public ApiResponse<AccessibilityPreferenceResponse> get(
            @CurrentUserId String userId) {
        return ApiResponse.success(accessibilityPreferenceService.get(userId));
    }

    @PutMapping
    public ApiResponse<AccessibilityPreferenceResponse> update(
            @CurrentUserId String userId,
            @Valid @RequestBody AccessibilityPreferenceUpdateRequest request) {
        return ApiResponse.success(accessibilityPreferenceService.update(userId, request));
    }
}
