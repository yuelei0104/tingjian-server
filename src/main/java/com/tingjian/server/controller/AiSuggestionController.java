package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.AiSuggestionRequest;
import com.tingjian.server.dto.AiSuggestionResponse;
import com.tingjian.server.service.AiSuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/suggestions")
public class AiSuggestionController {
    private final AiSuggestionService service;

    public AiSuggestionController(AiSuggestionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiSuggestionResponse> suggest(
            @CurrentUserId String ownerId,
            @Valid @RequestBody AiSuggestionRequest request) {
        return ApiResponse.success(service.suggest(ownerId, request));
    }
}
