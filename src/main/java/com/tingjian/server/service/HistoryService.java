package com.tingjian.server.service;

import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dto.HistoryItemResponse;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.HistorySummaryResponse;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.entity.HistorySummaryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;

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

    public HistorySummaryResponse summarize(String ownerId, String id) {
        var detail = detail(ownerId, id);
        var contents = detail.messages().stream()
                .map(message -> message.content().strip())
                .filter(content -> !content.isBlank())
                .toList();
        if (contents.isEmpty()) {
            return new HistorySummaryResponse(id, "这段会话暂时没有可供整理的文字。", 0,
                    "EXTRACTIVE_V1");
        }

        var highlights = new LinkedHashSet<String>();
        for (String content : contents) {
            highlights.add(content.length() > 80 ? content.substring(0, 80) + "…" : content);
            if (highlights.size() == 3) {
                break;
            }
        }
        String summary = "会话共 " + contents.size() + " 条文字。主要内容："
                + String.join("；", highlights) + "。";
        return new HistorySummaryResponse(id, summary, contents.size(), "EXTRACTIVE_V1");
    }

    @Transactional
    public void delete(String ownerId, String id) {
        if (!historyDao.existsForUpdate(id, ownerId)) {
            // DELETE 保持幂等：响应丢失后客户端可以安全重试，也不暴露其他账号的数据。
            return;
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
