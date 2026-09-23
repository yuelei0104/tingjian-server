package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.PrivacyDeleteResponse;
import com.tingjian.server.service.PrivacyService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/privacy")
public class PrivacyController {
    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @DeleteMapping("/history")
    public ApiResponse<PrivacyDeleteResponse> deleteHistory() {
        return ApiResponse.success(privacyService.deleteHistory(DevUser.OWNER_ID));
    }

    @DeleteMapping("/personalization")
    public ApiResponse<PrivacyDeleteResponse> deletePersonalization() {
        return ApiResponse.success(privacyService.deletePersonalization(DevUser.OWNER_ID));
    }

    @DeleteMapping("/all-data")
    public ApiResponse<PrivacyDeleteResponse> deleteAllData() {
        return ApiResponse.success(privacyService.deleteAllLocalData(DevUser.OWNER_ID));
    }
}
