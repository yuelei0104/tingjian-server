package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.DevUser;
import com.tingjian.server.dto.SessionCreateRequest;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.dto.SessionMessageRequest;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.SessionResponse;
import com.tingjian.server.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@Profile("dev")
@RequestMapping("/api/dev/sessions")
public class DevSessionController {
    private final SessionService sessionService;

    public DevSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> create(@Valid @RequestBody SessionCreateRequest request) {
        return ApiResponse.success(sessionService.create(DevUser.OWNER_ID, request.title()));
    }

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(sessionService.list(DevUser.OWNER_ID, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.success(new SessionDetailResponse(
                sessionService.get(DevUser.OWNER_ID, id), sessionService.messages(DevUser.OWNER_ID, id)));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionMessageResponse> addMessage(
            @PathVariable String id,
            @Valid @RequestBody SessionMessageRequest request) {
        return ApiResponse.success(sessionService.addMessage(
                DevUser.OWNER_ID, id, request.speaker().name(), request.content()));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<SessionResponse> end(@PathVariable String id) {
        return ApiResponse.success(sessionService.end(DevUser.OWNER_ID, id));
    }
}
