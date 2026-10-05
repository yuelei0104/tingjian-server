package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AiSuggestionDao;
import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.AiSuggestionAction;
import com.tingjian.server.dto.AiSuggestionRequest;
import com.tingjian.server.entity.AiSuggestionRequestEntity;
import com.tingjian.server.service.ai.AiExpressionProvider;
import com.tingjian.server.service.ai.AiProviderInput;
import com.tingjian.server.service.ai.AiProviderResult;
import com.tingjian.server.util.TokenGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTests {
    private final AiSuggestionDao dao = mock(AiSuggestionDao.class);
    private final SessionDao sessionDao = mock(SessionDao.class);
    private final AiExpressionProvider provider = mock(AiExpressionProvider.class);
    private final AiSuggestionService service = new AiSuggestionService(dao, sessionDao, provider);

    @Test
    void sanitizesInputBeforeCallingProviderAndStoresUsage() {
        var request = request("request-1", "联系 user@example.com", AiSuggestionAction.POLITE);
        when(dao.find("owner", "request-1")).thenReturn(Optional.empty());
        when(provider.suggest(any())).thenReturn(new AiProviderResult("好的，谢谢。", "TEST", false));

        var response = service.suggest("owner", request);

        var providerInput = ArgumentCaptor.forClass(AiProviderInput.class);
        verify(provider).suggest(providerInput.capture());
        assertEquals("联系 [邮箱]", providerInput.getValue().sourceText());
        assertEquals("TEST", response.provider());
        assertTrue(response.inputCharacters() > 0);
        verify(dao).insert(any());
    }

    @Test
    void providerFailureUsesLocalFallback() {
        var request = request("request-2", "你稍等", AiSuggestionAction.POLITE);
        when(dao.find("owner", "request-2")).thenReturn(Optional.empty());
        when(provider.suggest(any())).thenThrow(new IllegalStateException("provider unavailable"));

        var response = service.suggest("owner", request);

        assertEquals("LOCAL_FALLBACK", response.provider());
        assertTrue(response.fallback());
        assertTrue(response.suggestion().contains("您"));
    }

    @Test
    void identicalRequestReturnsStoredResultWithoutCallingProvider() {
        var request = request("same", "你好", AiSuggestionAction.POLITE);
        String hash = TokenGenerator.hash("POLITE|中文|你好|OTHER:请回复");
        var stored = new AiSuggestionRequestEntity(
                "owner", "same", hash, AiSuggestionAction.POLITE, "中文", "您好，谢谢。",
                "LOCAL_TEMPLATE", true, 1, 6, 6, LocalDateTime.now());
        when(dao.find("owner", "same")).thenReturn(Optional.of(stored));

        var response = service.suggest("owner", request);

        assertEquals("您好，谢谢。", response.suggestion());
        verify(provider, never()).suggest(any());
        verify(dao, never()).insert(any());
    }

    @Test
    void reusedRequestIdWithDifferentInputIsRejected() {
        var request = request("same", "不同内容", AiSuggestionAction.POLITE);
        var stored = new AiSuggestionRequestEntity(
                "owner", "same", "different-hash", AiSuggestionAction.POLITE, "中文", "old",
                "LOCAL_TEMPLATE", true, 0, 1, 3, LocalDateTime.now());
        when(dao.find("owner", "same")).thenReturn(Optional.of(stored));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.suggest("owner", request));

        assertEquals(ErrorCode.AI_IDEMPOTENCY_CONFLICT, error.errorCode());
    }

    private AiSuggestionRequest request(String id, String source, AiSuggestionAction action) {
        return new AiSuggestionRequest(
                id, null, source, action, "中文",
                List.of(new AiContextMessageRequest("OTHER", "请回复")));
    }
}
