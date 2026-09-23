package com.tingjian.server.service;

import com.tingjian.server.dao.PrivacyDao;
import com.tingjian.server.dto.PrivacyDeleteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyService {
    private final PrivacyDao privacyDao;

    public PrivacyService(PrivacyDao privacyDao) {
        this.privacyDao = privacyDao;
    }

    @Transactional
    public PrivacyDeleteResponse deleteHistory(String ownerId) {
        int messages = privacyDao.deleteConversationMessages(ownerId);
        int conversations = privacyDao.deleteConversations(ownerId);
        return new PrivacyDeleteResponse(conversations, messages, 0, 0, 0);
    }

    @Transactional
    public PrivacyDeleteResponse deletePersonalization(String ownerId) {
        int keywords = privacyDao.deleteKeywords(ownerId);
        int glossaryTerms = privacyDao.deleteGlossaryTerms(ownerId);
        int quickPhrases = privacyDao.deleteQuickPhrases(ownerId);
        return new PrivacyDeleteResponse(0, 0, keywords, glossaryTerms, quickPhrases);
    }

    @Transactional
    public PrivacyDeleteResponse deleteAllLocalData(String ownerId) {
        int messages = privacyDao.deleteConversationMessages(ownerId);
        int conversations = privacyDao.deleteConversations(ownerId);
        int keywords = privacyDao.deleteKeywords(ownerId);
        int glossaryTerms = privacyDao.deleteGlossaryTerms(ownerId);
        int quickPhrases = privacyDao.deleteQuickPhrases(ownerId);
        return new PrivacyDeleteResponse(
                conversations, messages, keywords, glossaryTerms, quickPhrases);
    }
}
