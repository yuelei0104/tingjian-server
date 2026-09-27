package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.UserPreferenceResponse;
import com.tingjian.server.dto.UserPreferenceUpdateRequest;
import com.tingjian.server.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
public class UserPreferenceController {
    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public ApiResponse<UserPreferenceResponse> get(@CurrentUserId String userId) {
        return ApiResponse.success(userPreferenceService.get(userId));
    }

    @PutMapping
    public ApiResponse<UserPreferenceResponse> update(
            @CurrentUserId String userId,
            @Valid @RequestBody UserPreferenceUpdateRequest request) {
        return ApiResponse.success(userPreferenceService.update(userId, request));
    }
}
