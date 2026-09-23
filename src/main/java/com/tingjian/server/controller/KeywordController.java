package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.KeywordResponse;
import com.tingjian.server.dto.KeywordUpsertRequest;
import com.tingjian.server.service.KeywordService;
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
@RequestMapping("/api/v1/keywords")
public class KeywordController {
    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping
    public ApiResponse<List<KeywordResponse>> list(@CurrentUserId String userId) {
        return ApiResponse.success(keywordService.list(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KeywordResponse> create(
            @CurrentUserId String userId, @Valid @RequestBody KeywordUpsertRequest request) {
        return ApiResponse.success(keywordService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KeywordResponse> update(
            @CurrentUserId String userId,
            @PathVariable String id,
            @Valid @RequestBody KeywordUpsertRequest request) {
        return ApiResponse.success(keywordService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId String userId, @PathVariable String id) {
        keywordService.delete(userId, id);
        return ApiResponse.success(null);
    }
}
