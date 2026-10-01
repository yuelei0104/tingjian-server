package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.PrivacyDeleteResponse;
import com.tingjian.server.dto.PrivacyExportResponse;
import com.tingjian.server.service.DataExportService;
import com.tingjian.server.service.PrivacyService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {
    private final PrivacyService privacyService;
    private final DataExportService dataExportService;

    public PrivacyController(
            PrivacyService privacyService,
            DataExportService dataExportService) {
        this.privacyService = privacyService;
        this.dataExportService = dataExportService;
    }

    @GetMapping("/export")
    public ApiResponse<PrivacyExportResponse> export(@CurrentUserId String userId) {
        return ApiResponse.success(dataExportService.export(userId));
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
