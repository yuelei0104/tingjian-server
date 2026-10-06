package com.tingjian.server.service.usage;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsrUsageWindowTests {
    private final UsageReservationGateway gateway = mock(UsageReservationGateway.class);
    private final UsageReservationGateway.Reservation first =
            new UsageReservationGateway.Reservation("first", true);
    private final UsageReservationGateway.Reservation second =
            new UsageReservationGateway.Reservation("second", true);

    @Test
    void commitsStartedPartialBlockWhenSessionFinishes() {
        when(gateway.reserve(any(), any(), anyLong(), any())).thenReturn(first);
        AsrUsageWindow window = new AsrUsageWindow(gateway, "user-1", "session-1");

        window.start();
        window.recordAudioBytes(AsrUsageWindow.PCM_BYTES_PER_SECOND * 5);
        window.finish();

        verify(gateway).commit(first);
        verify(gateway, never()).release(first);
    }

    @Test
    void commitsFullBlockAndReleasesUnusedNextBlock() {
        when(gateway.reserve(any(), any(), anyLong(), any())).thenReturn(first, second);
        AsrUsageWindow window = new AsrUsageWindow(gateway, "user-1", "session-1");

        window.start();
        window.recordAudioBytes(
                AsrUsageWindow.PCM_BYTES_PER_SECOND * AsrUsageWindow.BLOCK_SECONDS);
        window.finish();

        var ordered = inOrder(gateway);
        ordered.verify(gateway).commit(first);
        ordered.verify(gateway).reserve(
                "user-1", UsageReservationGateway.Metric.ASR_SECONDS,
                AsrUsageWindow.BLOCK_SECONDS, "asr:session-1:1");
        ordered.verify(gateway).release(second);
    }

    @Test
    void releasesFirstBlockWhenNoAudioWasUploaded() {
        when(gateway.reserve(any(), any(), anyLong(), any())).thenReturn(first);
        AsrUsageWindow window = new AsrUsageWindow(gateway, "user-1", "session-1");

        window.start();
        window.finish();
        window.finish();

        verify(gateway).release(first);
        verify(gateway, never()).commit(first);
    }
}
