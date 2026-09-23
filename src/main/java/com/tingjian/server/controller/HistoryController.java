package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.service.HistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Profile("dev")
@RequestMapping("/api/dev/history")
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ApiResponse<HistoryListResponse> search(
            @RequestParam(defaultValue = "") @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(historyService.search(DevUser.OWNER_ID, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(historyService.detail(DevUser.OWNER_ID, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        historyService.delete(DevUser.OWNER_ID, id);
        return ApiResponse.success(null);
    }
}
