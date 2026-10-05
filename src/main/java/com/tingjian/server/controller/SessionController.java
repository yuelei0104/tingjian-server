package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.ActiveSessionResponse;
import com.tingjian.server.dto.SessionCreateRequest;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.dto.SessionMessageRequest;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.SessionMessagePageResponse;
import com.tingjian.server.dto.SessionRenameRequest;
import com.tingjian.server.dto.SessionResponse;
import com.tingjian.server.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> create(
            @CurrentUserId String userId,
            @Valid @RequestBody SessionCreateRequest request) {
        return ApiResponse.success(sessionService.create(userId, request.title()));
    }

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(sessionService.list(userId, page, size));
    }

    @GetMapping("/active")
    public ApiResponse<ActiveSessionResponse> active(
            @CurrentUserId String userId) {
        return ApiResponse.success(sessionService.active(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDetailResponse> detail(
            @CurrentUserId String userId, @PathVariable String id) {
        return ApiResponse.success(new SessionDetailResponse(
                sessionService.get(userId, id), sessionService.messages(userId, id)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<SessionResponse> rename(
            @CurrentUserId String userId,
            @PathVariable String id,
            @Valid @RequestBody SessionRenameRequest request) {
        return ApiResponse.success(sessionService.rename(userId, id, request.title()));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionMessageResponse> addMessage(
            @CurrentUserId String userId,
            @PathVariable String id,
            @Valid @RequestBody SessionMessageRequest request) {
        return ApiResponse.success(sessionService.addMessage(
                userId, id, request.clientMessageId(),
                request.speaker().name(), request.content()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<SessionMessagePageResponse> messages(
            @CurrentUserId String userId,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") @Min(0) long afterSequence,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                sessionService.messagePage(userId, id, afterSequence, size));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<SessionResponse> end(
            @CurrentUserId String userId, @PathVariable String id) {
        return ApiResponse.success(sessionService.end(userId, id));
    }
}
