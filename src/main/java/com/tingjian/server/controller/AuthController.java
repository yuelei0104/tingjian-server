package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.dto.AuthTokenResponse;
import com.tingjian.server.dto.EmailVerificationRequest;
import com.tingjian.server.dto.LoginRequest;
import com.tingjian.server.dto.PasswordResetRequest;
import com.tingjian.server.dto.RefreshTokenRequest;
import com.tingjian.server.dto.RegisterRequest;
import com.tingjian.server.dto.VerificationChallengeResponse;
import com.tingjian.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIp(servletRequest)));
    }

    @PostMapping("/email-verification/request")
    public ApiResponse<VerificationChallengeResponse> requestRegistrationCode(
            @Valid @RequestBody EmailVerificationRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.requestRegistrationCode(
                request.email(), clientIp(servletRequest)));
    }

    @PostMapping("/password/forgot")
    public ApiResponse<VerificationChallengeResponse> forgotPassword(
            @Valid @RequestBody EmailVerificationRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.requestPasswordReset(
                request.email(), clientIp(servletRequest)));
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(null);
    }

    private static String clientIp(HttpServletRequest request) {
        // Do not trust X-Forwarded-For here unless the deployment has a trusted-proxy filter.
        return request.getRemoteAddr();
    }
}
