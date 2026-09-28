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
