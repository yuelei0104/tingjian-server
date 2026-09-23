package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dto.LoginRequest;
import com.tingjian.server.dto.RegisterRequest;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.PasswordHasher;
import com.tingjian.server.util.TokenGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {
    private final UserDao userDao = mock(UserDao.class);
    private final AuthSessionDao authSessionDao = mock(AuthSessionDao.class);
    private final AuthService service = new AuthService(userDao, authSessionDao);

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        var response = service.register(new RegisterRequest(
                "  USER@Example.COM  ", "correct-password", "  小明  "));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userDao).create(captor.capture());
        assertEquals("user@example.com", captor.getValue().email());
        assertEquals("小明", captor.getValue().displayName());
        assertTrue(PasswordHasher.matches("correct-password", captor.getValue().passwordHash()));
        assertEquals("user@example.com", response.user().email());
        verify(authSessionDao).create(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        var user = new UserEntity(
                "id", "user@example.com", PasswordHasher.hash("correct-password"),
                "User", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());
        when(userDao.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("user@example.com", "wrong-password")));
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(authSessionDao.findActiveByRefreshTokenForUpdate(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.refresh("unknown-token"));
    }

    @Test
    void logoutRevokesHashedRefreshToken() {
        service.logout("refresh-token");

        verify(authSessionDao).revokeByRefreshToken(
                org.mockito.ArgumentMatchers.eq(TokenGenerator.hash("refresh-token")), any());
    }
}
