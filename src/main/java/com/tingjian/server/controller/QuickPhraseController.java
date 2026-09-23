package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.QuickPhraseResponse;
import com.tingjian.server.dto.QuickPhraseUpsertRequest;
import com.tingjian.server.service.QuickPhraseService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
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
@Profile("dev")
@RequestMapping("/api/dev/quick-phrases")
public class QuickPhraseController {
    private final QuickPhraseService quickPhraseService;

    public QuickPhraseController(QuickPhraseService quickPhraseService) {
        this.quickPhraseService = quickPhraseService;
    }

    @GetMapping
    public ApiResponse<List<QuickPhraseResponse>> list() {
        return ApiResponse.success(quickPhraseService.list(DevUser.OWNER_ID));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuickPhraseResponse> create(@Valid @RequestBody QuickPhraseUpsertRequest request) {
        return ApiResponse.success(quickPhraseService.create(DevUser.OWNER_ID, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuickPhraseResponse> update(
            @PathVariable String id, @Valid @RequestBody QuickPhraseUpsertRequest request) {
        return ApiResponse.success(quickPhraseService.update(DevUser.OWNER_ID, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        quickPhraseService.delete(DevUser.OWNER_ID, id);
        return ApiResponse.success(null);
    }
}
