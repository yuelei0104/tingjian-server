package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.common.CurrentSessionId;
import com.tingjian.server.dto.AccountDeleteRequest;
import com.tingjian.server.dto.AccountPasswordChangeRequest;
import com.tingjian.server.dto.AccountProfileUpdateRequest;
import com.tingjian.server.dto.AccountSessionResponse;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.dto.PhoneBindingRequest;
import com.tingjian.server.dto.PhoneBindingResponse;
import com.tingjian.server.dto.PhoneVerificationRequest;
import com.tingjian.server.dto.VerificationChallengeResponse;
import com.tingjian.server.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<AuthUserResponse> profile(@CurrentUserId String userId) {
        return ApiResponse.success(accountService.profile(userId));
    }

    @PatchMapping
    public ApiResponse<AuthUserResponse> updateProfile(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountProfileUpdateRequest request) {
        return ApiResponse.success(accountService.updateProfile(userId, request.displayName()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountPasswordChangeRequest request) {
        accountService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AccountSessionResponse>> sessions(
            @CurrentUserId String userId,
            @CurrentSessionId String currentSessionId) {
        return ApiResponse.success(accountService.sessions(userId, currentSessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> revokeSession(
            @CurrentUserId String userId, @PathVariable String sessionId) {
        accountService.revokeSession(userId, sessionId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/sessions")
    public ApiResponse<Void> revokeOtherSessions(
            @CurrentUserId String userId,
            @CurrentSessionId String currentSessionId) {
        accountService.revokeOtherSessions(userId, currentSessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/phone")
    public ApiResponse<PhoneBindingResponse> phone(@CurrentUserId String userId) {
        return ApiResponse.success(accountService.phone(userId));
    }

    @PostMapping("/phone/verification")
    public ApiResponse<VerificationChallengeResponse> requestPhoneCode(
            @CurrentUserId String userId,
            @Valid @RequestBody PhoneVerificationRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(accountService.requestPhoneCode(
                userId, request.phone(), servletRequest.getRemoteAddr()));
    }

    @PutMapping("/phone")
    public ApiResponse<PhoneBindingResponse> bindPhone(
            @CurrentUserId String userId,
            @Valid @RequestBody PhoneBindingRequest request) {
        return ApiResponse.success(accountService.bindPhone(
                userId, request.phone(), request.verificationId(), request.verificationCode()));
    }

    @org.springframework.web.bind.annotation.RequestMapping(
            path = "/phone", method = org.springframework.web.bind.annotation.RequestMethod.DELETE)
    public ApiResponse<Void> unbindPhone(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountDeleteRequest request) {
        accountService.unbindPhone(userId, request.password());
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAccount(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountDeleteRequest request) {
        accountService.delete(userId, request.password());
        return ApiResponse.success(null);
    }
}
