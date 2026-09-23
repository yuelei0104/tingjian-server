package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.KeywordDao;
import com.tingjian.server.dto.KeywordResponse;
import com.tingjian.server.dto.KeywordUpsertRequest;
import com.tingjian.server.entity.KeywordEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class KeywordService {
    private final KeywordDao keywordDao;

    public KeywordService(KeywordDao keywordDao) {
        this.keywordDao = keywordDao;
    }

    public List<KeywordResponse> list(String ownerId) {
        return keywordDao.list(ownerId).stream().map(KeywordService::toResponse).toList();
    }

    public KeywordResponse create(String ownerId, KeywordUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        KeywordEntity entity = new KeywordEntity(
                IdGenerator.uuid(), ownerId, request.phrase().strip(), request.vibrationEnabled(),
                request.priority(), request.enabled(), now, now);
        try {
            keywordDao.create(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "关键词已存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public KeywordResponse update(String ownerId, String id, KeywordUpsertRequest request) {
        KeywordEntity current = keywordDao.find(id, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
        KeywordEntity updated = new KeywordEntity(
                id, ownerId, request.phrase().strip(), request.vibrationEnabled(), request.priority(),
                request.enabled(), current.createdAt(), LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (keywordDao.update(updated) == 0) {
                throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "关键词已存在");
        }
        return toResponse(updated);
    }

    public void delete(String ownerId, String id) {
        if (keywordDao.delete(id, ownerId) == 0) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }
    }

    private static KeywordResponse toResponse(KeywordEntity entity) {
        return new KeywordResponse(
                entity.id(), entity.phrase(), entity.vibrationEnabled(), entity.priority(), entity.enabled(),
                entity.createdAt(), entity.updatedAt());
    }
}
