package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dao.UserPhoneDao;
import com.tingjian.server.dto.AccountExportResponse;
import com.tingjian.server.dto.ConversationExportResponse;
import com.tingjian.server.dto.MessageExportResponse;
import com.tingjian.server.dto.PrivacyExportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DataExportService {
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final KeywordService keywordService;
    private final GlossaryService glossaryService;
    private final QuickPhraseService quickPhraseService;
    private final UserPreferenceService userPreferenceService;
    private final AccessibilityPreferenceService accessibilityPreferenceService;
    private final UserPhoneDao userPhoneDao;

    public DataExportService(
            UserDao userDao,
            SessionDao sessionDao,
            KeywordService keywordService,
            GlossaryService glossaryService,
            QuickPhraseService quickPhraseService,
            UserPreferenceService userPreferenceService,
            AccessibilityPreferenceService accessibilityPreferenceService,
            UserPhoneDao userPhoneDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.keywordService = keywordService;
        this.glossaryService = glossaryService;
        this.quickPhraseService = quickPhraseService;
        this.userPreferenceService = userPreferenceService;
        this.accessibilityPreferenceService = accessibilityPreferenceService;
        this.userPhoneDao = userPhoneDao;
    }

    @Transactional(readOnly = true)
    public PrivacyExportResponse export(String ownerId) {
        var user = userDao.findById(ownerId)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        var conversations = sessionDao.listAll(ownerId).stream()
                .map(session -> new ConversationExportResponse(
                        session.id(), session.title(), session.status(),
                        session.startedAt(), session.endedAt(),
                        sessionDao.messages(session.id()).stream()
                                .map(message -> new MessageExportResponse(
                                        message.speaker(), message.content(), message.createdAt()))
                                .toList()))
                .toList();
        return new PrivacyExportResponse(
                LocalDateTime.now(ZoneOffset.UTC),
                new AccountExportResponse(
                        user.email(),
                        userPhoneDao.findByUserId(ownerId).map(phone -> phone.phone()).orElse(null),
                        user.displayName(), user.createdAt()),
                conversations,
                keywordService.list(ownerId),
                glossaryService.list(ownerId),
                quickPhraseService.list(ownerId),
                userPreferenceService.get(ownerId),
                accessibilityPreferenceService.get(ownerId));
    }
}
