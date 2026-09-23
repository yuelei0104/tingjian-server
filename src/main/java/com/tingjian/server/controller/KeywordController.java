package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.KeywordResponse;
import com.tingjian.server.dto.KeywordUpsertRequest;
import com.tingjian.server.service.KeywordService;
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
@RequestMapping("/api/dev/keywords")
public class KeywordController {
    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping
    public ApiResponse<List<KeywordResponse>> list() {
        return ApiResponse.success(keywordService.list(DevUser.OWNER_ID));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KeywordResponse> create(@Valid @RequestBody KeywordUpsertRequest request) {
        return ApiResponse.success(keywordService.create(DevUser.OWNER_ID, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KeywordResponse> update(
            @PathVariable String id, @Valid @RequestBody KeywordUpsertRequest request) {
        return ApiResponse.success(keywordService.update(DevUser.OWNER_ID, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        keywordService.delete(DevUser.OWNER_ID, id);
        return ApiResponse.success(null);
    }
}
