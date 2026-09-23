package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.GlossaryDao;
import com.tingjian.server.dto.GlossaryResponse;
import com.tingjian.server.dto.GlossaryUpsertRequest;
import com.tingjian.server.entity.GlossaryTermEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GlossaryService {
    private final GlossaryDao glossaryDao;

    public GlossaryService(GlossaryDao glossaryDao) {
        this.glossaryDao = glossaryDao;
    }

    public List<GlossaryResponse> list(String ownerId) {
        return glossaryDao.list(ownerId).stream().map(GlossaryService::toResponse).toList();
    }

    public GlossaryResponse create(String ownerId, GlossaryUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        GlossaryTermEntity entity = new GlossaryTermEntity(
                IdGenerator.uuid(), ownerId, request.term().strip(), normalizeAlias(request.alias()),
                request.language().strip(), request.category().strip(), request.priority(), request.enabled(),
                now, now);
        try {
            glossaryDao.create(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该语言下的术语已存在");
        }
        return toResponse(entity);
    }

    @Transactional
    public GlossaryResponse update(String ownerId, String id, GlossaryUpsertRequest request) {
        GlossaryTermEntity current = glossaryDao.find(id, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GLOSSARY_NOT_FOUND));
        GlossaryTermEntity updated = new GlossaryTermEntity(
                id, ownerId, request.term().strip(), normalizeAlias(request.alias()),
                request.language().strip(), request.category().strip(), request.priority(), request.enabled(),
                current.createdAt(), LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (glossaryDao.update(updated) == 0) {
                throw new BusinessException(ErrorCode.GLOSSARY_NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该语言下的术语已存在");
        }
        return toResponse(updated);
    }

    public void delete(String ownerId, String id) {
        if (glossaryDao.delete(id, ownerId) == 0) {
            throw new BusinessException(ErrorCode.GLOSSARY_NOT_FOUND);
        }
    }

    private static String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? null : alias.strip();
    }

    private static GlossaryResponse toResponse(GlossaryTermEntity entity) {
        return new GlossaryResponse(
                entity.id(), entity.term(), entity.alias(), entity.language(), entity.category(),
                entity.priority(), entity.enabled(), entity.createdAt(), entity.updatedAt());
    }
}
