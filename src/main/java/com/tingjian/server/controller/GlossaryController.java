package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.GlossaryResponse;
import com.tingjian.server.dto.GlossaryUpsertRequest;
import com.tingjian.server.service.GlossaryService;
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
@RequestMapping("/api/dev/glossary")
public class GlossaryController {
    private final GlossaryService glossaryService;

    public GlossaryController(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @GetMapping
    public ApiResponse<List<GlossaryResponse>> list() {
        return ApiResponse.success(glossaryService.list(DevUser.OWNER_ID));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GlossaryResponse> create(@Valid @RequestBody GlossaryUpsertRequest request) {
        return ApiResponse.success(glossaryService.create(DevUser.OWNER_ID, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GlossaryResponse> update(
            @PathVariable String id, @Valid @RequestBody GlossaryUpsertRequest request) {
        return ApiResponse.success(glossaryService.update(DevUser.OWNER_ID, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        glossaryService.delete(DevUser.OWNER_ID, id);
        return ApiResponse.success(null);
    }
}
