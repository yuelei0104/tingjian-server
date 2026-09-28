package com.tingjian.server.service;

import com.tingjian.server.dao.UsageDao;
import com.tingjian.server.entity.UsageSummaryEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageServiceTests {
    private final UsageDao usageDao = mock(UsageDao.class);
    private final UsageService service = new UsageService(usageDao);

    @Test
    void getReturnsCurrentPeriodUsageAndLimits() {
        var period = YearMonth.now(ZoneOffset.UTC);
        var start = period.atDay(1).atStartOfDay();
        var end = period.plusMonths(1).atDay(1).atStartOfDay();
        when(usageDao.summary("owner", start, end))
                .thenReturn(new UsageSummaryEntity(12, 80, 4_000, 900));

        var response = service.get("owner");

        assertEquals(period.atDay(1), response.periodStart());
        assertEquals(period.atEndOfMonth(), response.periodEnd());
        assertFalse(response.purchasable());
        assertEquals(12, response.overview().conversationCount());
        assertEquals(80, response.overview().messageCount());
        assertEquals(4_000, response.overview().textCharacterCount());
        assertEquals(900, response.overview().totalDurationSeconds());
        assertEquals(3, response.metrics().size());
        assertEquals(2_700, response.metrics().getFirst().remaining());
        assertEquals(0.25D, response.metrics().getFirst().progress());
    }

    @Test
    void getCapsExceededUsageProgressAndRemaining() {
        when(usageDao.summary(
                org.mockito.ArgumentMatchers.eq("owner"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(new UsageSummaryEntity(120, 6_000, 120_000, 4_000));

        var response = service.get("owner");

        response.metrics().forEach(metric -> {
            assertEquals(0, metric.remaining());
            assertEquals(1D, metric.progress());
        });
    }

    @Test
    void getQueriesExactlyOneCalendarMonth() {
        when(usageDao.summary(
                org.mockito.ArgumentMatchers.eq("owner"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(new UsageSummaryEntity(0, 0, 0, 0));

        service.get("owner");

        var startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        var endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(usageDao).summary(
                org.mockito.ArgumentMatchers.eq("owner"), startCaptor.capture(), endCaptor.capture());
        assertEquals(startCaptor.getValue().plusMonths(1), endCaptor.getValue());
        assertEquals(1, startCaptor.getValue().getDayOfMonth());
    }
}
