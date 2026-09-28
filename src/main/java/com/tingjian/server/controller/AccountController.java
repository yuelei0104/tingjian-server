package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.AccountDeleteRequest;
import com.tingjian.server.dto.AccountPasswordChangeRequest;
import com.tingjian.server.dto.AccountProfileUpdateRequest;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @DeleteMapping
    public ApiResponse<Void> deleteAccount(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountDeleteRequest request) {
        accountService.delete(userId, request.password());
        return ApiResponse.success(null);
    }
}
