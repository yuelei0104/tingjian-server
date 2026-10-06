package com.tingjian.server.service;

import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dao.UserPhoneDao;
import com.tingjian.server.dto.GlossaryResponse;
import com.tingjian.server.dto.KeywordResponse;
import com.tingjian.server.dto.QuickPhraseResponse;
import com.tingjian.server.dto.AccessibilityPreferenceResponse;
import com.tingjian.server.dto.UserPreferenceResponse;
import com.tingjian.server.entity.ConversationEntity;
import com.tingjian.server.entity.ConversationMessageEntity;
import com.tingjian.server.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataExportServiceTests {
    private final UserDao userDao = mock(UserDao.class);
    private final SessionDao sessionDao = mock(SessionDao.class);
    private final KeywordService keywordService = mock(KeywordService.class);
    private final GlossaryService glossaryService = mock(GlossaryService.class);
    private final QuickPhraseService quickPhraseService = mock(QuickPhraseService.class);
    private final UserPreferenceService preferenceService = mock(UserPreferenceService.class);
    private final AccessibilityPreferenceService accessibilityPreferenceService =
            mock(AccessibilityPreferenceService.class);
    private final UserPhoneDao userPhoneDao = mock(UserPhoneDao.class);
    private final DataExportService service = new DataExportService(
            userDao, sessionDao, keywordService, glossaryService, quickPhraseService,
            preferenceService, accessibilityPreferenceService, userPhoneDao);

    @Test
    void exportCollectsOwnedAccountConversationsAndPersonalization() {
        LocalDateTime now = LocalDateTime.now();
        when(userDao.findById("owner")).thenReturn(Optional.of(new UserEntity(
                "owner", "owner@example.com", "hash", "Owner", "ACTIVE", now, now)));
        when(sessionDao.listAll("owner")).thenReturn(List.of(new ConversationEntity(
                "conversation-1", "owner", "演示会话", "ENDED", now, now)));
        when(sessionDao.messages("conversation-1")).thenReturn(List.of(
                new ConversationMessageEntity(
                        "message-1", "conversation-1", "client-1", 1,
                        "OTHER", "你好", now)));
        when(keywordService.list("owner")).thenReturn(List.of(
                new KeywordResponse("keyword-1", "姓名", true, 1, true, now, now)));
        when(glossaryService.list("owner")).thenReturn(List.of(
                new GlossaryResponse("term-1", "听见", "", "zh-CN", "通用", 1,
                        true, now, now)));
        when(quickPhraseService.list("owner")).thenReturn(List.of(
                new QuickPhraseResponse("phrase-1", "请再说一次", "日常", 1,
                        true, now, now)));
        when(preferenceService.get("owner")).thenReturn(new UserPreferenceResponse(
                true, false, "system", "default", 1.0, "zh-CN", true,
                true, false, now));
        when(accessibilityPreferenceService.get("owner")).thenReturn(
                new AccessibilityPreferenceResponse(
                        true, true, true, true, false, true, now));

        var response = service.export("owner");

        assertEquals("owner@example.com", response.account().email());
        assertEquals(1, response.conversations().size());
        assertEquals("你好", response.conversations().getFirst().messages().getFirst().content());
        assertEquals(1, response.keywords().size());
        assertEquals(1, response.glossaryTerms().size());
        assertEquals(1, response.quickPhrases().size());
        assertTrue(response.accessibility().highContrast());
    }

    @Test
    void exportRejectsInactiveAccount() {
        LocalDateTime now = LocalDateTime.now();
        when(userDao.findById("owner")).thenReturn(Optional.of(new UserEntity(
                "owner", "owner@example.com", "hash", "Owner", "DELETED", now, now)));

        assertThrows(RuntimeException.class, () -> service.export("owner"));
    }
}
