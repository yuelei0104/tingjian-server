package com.tingjian.server.service;

import com.tingjian.server.dao.UserPreferenceDao;
import com.tingjian.server.dto.UserPreferenceResponse;
import com.tingjian.server.dto.UserPreferenceUpdateRequest;
import com.tingjian.server.entity.UserPreferenceEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class UserPreferenceService {
    private final UserPreferenceDao userPreferenceDao;

    public UserPreferenceService(UserPreferenceDao userPreferenceDao) {
        this.userPreferenceDao = userPreferenceDao;
    }

    public UserPreferenceResponse get(String ownerId) {
        return userPreferenceDao.find(ownerId)
                .map(entity -> toResponse(entity, true))
                .orElseGet(UserPreferenceService::defaults);
    }

    public UserPreferenceResponse update(String ownerId, UserPreferenceUpdateRequest request) {
        UserPreferenceEntity entity = new UserPreferenceEntity(
                ownerId, request.largeText(), request.voiceMode(), request.voiceStyle(),
                request.ttsSpeed(), request.recognitionLanguage(), request.keywordVibration(),
                request.keywordHighlight(), request.autoSummary(),
                LocalDateTime.now(ZoneOffset.UTC));
        userPreferenceDao.upsert(entity);
        return toResponse(entity, true);
    }

    private static UserPreferenceResponse defaults() {
        return new UserPreferenceResponse(
                false, false, "自动", "自然", 1.0, "中英混合",
                true, true, false, null);
    }

    private static UserPreferenceResponse toResponse(
            UserPreferenceEntity entity, boolean configured) {
        return new UserPreferenceResponse(
                configured, entity.largeText(), entity.voiceMode(), entity.voiceStyle(),
                entity.ttsSpeed(), entity.recognitionLanguage(), entity.keywordVibration(),
                entity.keywordHighlight(), entity.autoSummary(), entity.updatedAt());
    }
}
