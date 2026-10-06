package com.tingjian.server.service.usage;

public final class AsrUsageWindow {
    public static final int PCM_BYTES_PER_SECOND = 16_000 * 2;
    public static final int BLOCK_SECONDS = 30;
    private static final long BLOCK_BYTES = (long) PCM_BYTES_PER_SECOND * BLOCK_SECONDS;

    private final UsageReservationGateway gateway;
    private final String userId;
    private final String sessionId;
    private int blockIndex;
    private long bytesInBlock;
    private boolean finished;
    private UsageReservationGateway.Reservation reservation;

    public AsrUsageWindow(UsageReservationGateway gateway, String userId, String sessionId) {
        this.gateway = gateway;
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public synchronized void start() {
        if (reservation != null || finished) return;
        reservation = reserveCurrentBlock();
    }

    public synchronized void recordAudioBytes(int count) {
        if (count <= 0 || finished) return;
        if (reservation == null) start();
        bytesInBlock += count;
        while (bytesInBlock >= BLOCK_BYTES) {
            gateway.commit(reservation);
            reservation = null;
            bytesInBlock -= BLOCK_BYTES;
            blockIndex++;
            reservation = reserveCurrentBlock();
        }
    }

    public synchronized void finish() {
        if (finished) return;
        finished = true;
        if (reservation == null) return;
        if (bytesInBlock > 0) {
            gateway.commit(reservation);
        } else {
            gateway.release(reservation);
        }
        reservation = null;
    }

    private UsageReservationGateway.Reservation reserveCurrentBlock() {
        return gateway.reserve(
                userId,
                UsageReservationGateway.Metric.ASR_SECONDS,
                BLOCK_SECONDS,
                "asr:" + sessionId + ":" + blockIndex);
    }
}
