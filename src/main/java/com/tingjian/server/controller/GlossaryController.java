package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.GlossaryResponse;
import com.tingjian.server.dto.GlossaryUpsertRequest;
import com.tingjian.server.service.GlossaryService;
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
@RequestMapping("/api/v1/glossary")
public class GlossaryController {
    private final GlossaryService glossaryService;

    public GlossaryController(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @GetMapping
    public ApiResponse<List<GlossaryResponse>> list(@CurrentUserId String userId) {
        return ApiResponse.success(glossaryService.list(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GlossaryResponse> create(
            @CurrentUserId String userId, @Valid @RequestBody GlossaryUpsertRequest request) {
        return ApiResponse.success(glossaryService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GlossaryResponse> update(
            @CurrentUserId String userId,
            @PathVariable String id,
            @Valid @RequestBody GlossaryUpsertRequest request) {
        return ApiResponse.success(glossaryService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId String userId, @PathVariable String id) {
        glossaryService.delete(userId, id);
        return ApiResponse.success(null);
    }
}
