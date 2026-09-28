package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.AccountDeleteRequest;
import com.tingjian.server.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @DeleteMapping
    public ApiResponse<Void> deleteAccount(
            @CurrentUserId String userId,
            @Valid @RequestBody AccountDeleteRequest request) {
        accountService.delete(userId, request.password());
        return ApiResponse.success(null);
    }
}
