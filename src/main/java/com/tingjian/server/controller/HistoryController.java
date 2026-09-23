package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.service.HistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ApiResponse<HistoryListResponse> search(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "") @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(historyService.search(userId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDetailResponse> detail(
            @CurrentUserId String userId, @PathVariable String id) {
        return ApiResponse.success(historyService.detail(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId String userId, @PathVariable String id) {
        historyService.delete(userId, id);
        return ApiResponse.success(null);
    }
}
