package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.QuickPhraseDao;
import com.tingjian.server.dto.QuickPhraseResponse;
import com.tingjian.server.dto.QuickPhraseUpsertRequest;
import com.tingjian.server.entity.QuickPhraseEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class QuickPhraseService {
    private final QuickPhraseDao quickPhraseDao;

    public QuickPhraseService(QuickPhraseDao quickPhraseDao) {
        this.quickPhraseDao = quickPhraseDao;
    }

    public List<QuickPhraseResponse> list(String ownerId) {
        return quickPhraseDao.list(ownerId).stream().map(QuickPhraseService::toResponse).toList();
    }

    public QuickPhraseResponse create(String ownerId, QuickPhraseUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        QuickPhraseEntity entity = new QuickPhraseEntity(
                IdGenerator.uuid(), ownerId, request.content().strip(), request.category().strip(),
                request.sortOrder(), request.enabled(), now, now);
        try {
            quickPhraseDao.create(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "常用语已存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public QuickPhraseResponse update(String ownerId, String id, QuickPhraseUpsertRequest request) {
        QuickPhraseEntity current = quickPhraseDao.find(id, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUICK_PHRASE_NOT_FOUND));
        QuickPhraseEntity updated = new QuickPhraseEntity(
                id, ownerId, request.content().strip(), request.category().strip(), request.sortOrder(),
                request.enabled(), current.createdAt(), LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (quickPhraseDao.update(updated) == 0) {
                throw new BusinessException(ErrorCode.QUICK_PHRASE_NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "常用语已存在");
        }
        return toResponse(updated);
    }

    public void delete(String ownerId, String id) {
        if (quickPhraseDao.delete(id, ownerId) == 0) {
            throw new BusinessException(ErrorCode.QUICK_PHRASE_NOT_FOUND);
        }
    }

    private static QuickPhraseResponse toResponse(QuickPhraseEntity entity) {
        return new QuickPhraseResponse(
                entity.id(), entity.content(), entity.category(), entity.sortOrder(), entity.enabled(),
                entity.createdAt(), entity.updatedAt());
    }
}
