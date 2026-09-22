package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dto.HistoryItemResponse;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.entity.HistorySummaryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {
    private final HistoryDao historyDao;
    private final SessionService sessionService;

    public HistoryService(HistoryDao historyDao, SessionService sessionService) {
        this.historyDao = historyDao;
        this.sessionService = sessionService;
    }

    public HistoryListResponse search(String ownerId, String keyword, int page, int size) {
        long offset = (long) page * size;
        long total = historyDao.count(ownerId, keyword);
        var items = historyDao.search(ownerId, keyword, size, offset).stream()
                .map(HistoryService::toResponse)
                .toList();
        return new HistoryListResponse(items, page, size, total, offset + items.size() < total);
    }

    public SessionDetailResponse detail(String ownerId, String id) {
        return new SessionDetailResponse(
                sessionService.get(ownerId, id),
                sessionService.messages(ownerId, id));
    }

    @Transactional
    public void delete(String ownerId, String id) {
        if (!historyDao.existsForUpdate(id, ownerId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        historyDao.deleteMessages(id);
        historyDao.deleteConversation(id, ownerId);
    }

    private static HistoryItemResponse toResponse(HistorySummaryEntity entity) {
        return new HistoryItemResponse(
                entity.id(), entity.title(), entity.status(), entity.startedAt(), entity.endedAt(),
                entity.messageCount(), entity.preview());
    }
}
