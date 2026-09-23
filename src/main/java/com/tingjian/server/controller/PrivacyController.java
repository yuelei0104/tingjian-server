package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.PrivacyDeleteResponse;
import com.tingjian.server.service.PrivacyService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {
    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @DeleteMapping("/history")
    public ApiResponse<PrivacyDeleteResponse> deleteHistory(@CurrentUserId String userId) {
        return ApiResponse.success(privacyService.deleteHistory(userId));
    }

    @DeleteMapping("/personalization")
    public ApiResponse<PrivacyDeleteResponse> deletePersonalization(@CurrentUserId String userId) {
        return ApiResponse.success(privacyService.deletePersonalization(userId));
    }

    @DeleteMapping("/all-data")
    public ApiResponse<PrivacyDeleteResponse> deleteAllData(@CurrentUserId String userId) {
        return ApiResponse.success(privacyService.deleteAllLocalData(userId));
    }
}
