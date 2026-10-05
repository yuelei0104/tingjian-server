package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AiSuggestionDao;
import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.AiSuggestionAction;
import com.tingjian.server.dto.AiSuggestionRequest;
import com.tingjian.server.dto.AiSuggestionResponse;
import com.tingjian.server.entity.AiSuggestionRequestEntity;
import com.tingjian.server.service.ai.AiExpressionProvider;
import com.tingjian.server.service.ai.AiInputSanitizer;
import com.tingjian.server.service.ai.AiProviderInput;
import com.tingjian.server.service.ai.AiProviderResult;
import com.tingjian.server.service.ai.LocalTemplateAiProvider;
import com.tingjian.server.util.TokenGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AiSuggestionService {
    private static final long PROVIDER_TIMEOUT_SECONDS = 3;

    private final AiSuggestionDao dao;
    private final SessionDao sessionDao;
    private final AiExpressionProvider provider;

    public AiSuggestionService(AiSuggestionDao dao, SessionDao sessionDao, AiExpressionProvider provider) {
        this.dao = dao;
        this.sessionDao = sessionDao;
        this.provider = provider;
    }

    @Transactional
    public AiSuggestionResponse suggest(String ownerId, AiSuggestionRequest request) {
        if (request.action() != AiSuggestionAction.REPLY
                && (request.sourceText() == null || request.sourceText().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请输入需要改写的文字");
        }
        if (request.sessionId() != null && !request.sessionId().isBlank()
                && sessionDao.find(request.sessionId(), ownerId).isEmpty()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        String sourceText = AiInputSanitizer.clip(AiInputSanitizer.sanitize(request.sourceText()), 500);
        List<AiContextMessageRequest> context = AiInputSanitizer.context(request.context());
        AiProviderInput input = new AiProviderInput(request.action(), request.language(), sourceText, context);
        String inputHash = inputHash(input);

        AiSuggestionRequestEntity existing = dao.find(ownerId, request.clientRequestId()).orElse(null);
        if (existing != null) return existingResponse(existing, inputHash);

        AiProviderResult result = invokeProvider(input);
        String suggestion = AiInputSanitizer.clip(AiInputSanitizer.sanitize(result.suggestion()), 1000);
        LocalDateTime now = LocalDateTime.now();
        int inputCharacters = sourceText.length()
                + context.stream().mapToInt(message -> message.content().length()).sum();
        AiSuggestionRequestEntity entity = new AiSuggestionRequestEntity(
                ownerId, request.clientRequestId(), inputHash, request.action(), request.language(),
                suggestion, result.provider(), result.fallback(), context.size(), inputCharacters,
                suggestion.length(), now);
        try {
            dao.insert(entity);
        } catch (DuplicateKeyException race) {
            return existingResponse(dao.find(ownerId, request.clientRequestId())
                    .orElseThrow(() -> race), inputHash);
        }
        return toResponse(entity);
    }

    private AiProviderResult invokeProvider(AiProviderInput input) {
        try {
            AiProviderResult result = CompletableFuture.supplyAsync(() -> provider.suggest(input))
                    .orTimeout(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
            if (result == null || result.suggestion() == null || result.suggestion().isBlank()) {
                return LocalTemplateAiProvider.fallback(input);
            }
            return result;
        } catch (RuntimeException ignored) {
            return LocalTemplateAiProvider.fallback(input);
        }
    }

    private String inputHash(AiProviderInput input) {
        StringBuilder canonical = new StringBuilder()
                .append(input.action()).append('|')
                .append(input.language()).append('|')
                .append(input.sourceText());
        input.context().forEach(message -> canonical.append('|')
                .append(message.speaker()).append(':').append(message.content()));
        return TokenGenerator.hash(canonical.toString());
    }

    private AiSuggestionResponse existingResponse(AiSuggestionRequestEntity existing, String inputHash) {
        if (!existing.inputHash().equals(inputHash)) {
            throw new BusinessException(ErrorCode.AI_IDEMPOTENCY_CONFLICT, "请求编号已用于其他内容");
        }
        return toResponse(existing);
    }

    private AiSuggestionResponse toResponse(AiSuggestionRequestEntity entity) {
        return new AiSuggestionResponse(
                entity.clientRequestId(), entity.suggestion(), entity.action(), entity.language(),
                entity.provider(), entity.fallback(), entity.contextMessages(),
                entity.inputCharacters(), entity.outputCharacters(), entity.createdAt());
    }
}
