package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.QuickPhraseResponse;
import com.tingjian.server.dto.QuickPhraseUpsertRequest;
import com.tingjian.server.service.QuickPhraseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quick-phrases")
public class QuickPhraseController {
    private final QuickPhraseService quickPhraseService;

    public QuickPhraseController(QuickPhraseService quickPhraseService) {
        this.quickPhraseService = quickPhraseService;
    }

    @GetMapping
    public ApiResponse<List<QuickPhraseResponse>> list(@CurrentUserId String userId) {
        return ApiResponse.success(quickPhraseService.list(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuickPhraseResponse> create(
            @CurrentUserId String userId, @Valid @RequestBody QuickPhraseUpsertRequest request) {
        return ApiResponse.success(quickPhraseService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuickPhraseResponse> update(
            @CurrentUserId String userId,
            @PathVariable String id,
            @Valid @RequestBody QuickPhraseUpsertRequest request) {
        return ApiResponse.success(quickPhraseService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId String userId, @PathVariable String id) {
        quickPhraseService.delete(userId, id);
        return ApiResponse.success(null);
    }
}
