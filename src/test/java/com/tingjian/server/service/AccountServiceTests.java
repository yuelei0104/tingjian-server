package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.PrivacyDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTests {
    private final UserDao userDao = mock(UserDao.class);
    private final AuthSessionDao authSessionDao = mock(AuthSessionDao.class);
    private final PrivacyDao privacyDao = mock(PrivacyDao.class);
    private final AccountService service = new AccountService(userDao, authSessionDao, privacyDao);

    @Test
    void updateProfileTrimsAndReturnsLatestUser() {
        UserEntity before = user("correct-password");
        UserEntity after = new UserEntity(
                before.id(), before.email(), before.passwordHash(), "新昵称", before.status(),
                before.createdAt(), before.updatedAt());
        when(userDao.findById("owner")).thenReturn(Optional.of(before), Optional.of(after));
        when(userDao.updateDisplayName(eq("owner"), eq("新昵称"), any())).thenReturn(1);

        var response = service.updateProfile("owner", "  新昵称  ");

        assertEquals("新昵称", response.displayName());
        verify(userDao).updateDisplayName(eq("owner"), eq("新昵称"), any());
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        when(userDao.findById("owner")).thenReturn(Optional.of(user("correct-password")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.changePassword("owner", "wrong-password", "new-password"));

        assertEquals(401, error.errorCode().status().value());
        verify(userDao, never()).updatePassword(any(), any(), any());
        verify(authSessionDao, never()).deleteByUserId("owner");
    }

    @Test
    void changePasswordUpdatesHashAndInvalidatesEverySession() {
        when(userDao.findById("owner")).thenReturn(Optional.of(user("correct-password")));
        when(userDao.updatePassword(eq("owner"), any(), any())).thenReturn(1);

        service.changePassword("owner", "correct-password", "new-password");

        verify(userDao).updatePassword(eq("owner"),
                argThat(hash -> PasswordHasher.matches("new-password", hash)), any());
        verify(authSessionDao).deleteByUserId("owner");
    }

    @Test
    void deleteRequiresCurrentPassword() {
        when(userDao.findById("owner")).thenReturn(Optional.of(user("correct-password")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.delete("owner", "wrong-password"));

        assertEquals(401, error.errorCode().status().value());
        verify(authSessionDao, never()).deleteByUserId("owner");
        verify(userDao, never()).delete("owner");
    }

    @Test
    void deleteRemovesOwnedDataTokensAndAccountInOrder() {
        when(userDao.findById("owner")).thenReturn(Optional.of(user("correct-password")));
        when(userDao.delete("owner")).thenReturn(1);

        service.delete("owner", "correct-password");

        InOrder order = inOrder(privacyDao, authSessionDao, userDao);
        order.verify(privacyDao).deleteConversationMessages("owner");
        order.verify(privacyDao).deleteConversations("owner");
        order.verify(privacyDao).deleteKeywords("owner");
        order.verify(privacyDao).deleteGlossaryTerms("owner");
        order.verify(privacyDao).deleteQuickPhrases("owner");
        order.verify(privacyDao).deleteUserPreference("owner");
        order.verify(authSessionDao).deleteByUserId("owner");
        order.verify(userDao).delete("owner");
    }

    private static UserEntity user(String password) {
        LocalDateTime now = LocalDateTime.now();
        return new UserEntity("owner", "owner@example.com", PasswordHasher.hash(password),
                "Owner", "ACTIVE", now, now);
    }
}
