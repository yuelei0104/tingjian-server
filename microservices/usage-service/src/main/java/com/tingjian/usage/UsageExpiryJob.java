package com.tingjian.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class UsageExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(UsageExpiryJob.class);
    private final UsageQuotaService service;

    UsageExpiryJob(UsageQuotaService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${tingjian.usage.expiry-scan-delay-ms:60000}",
            initialDelayString = "${tingjian.usage.expiry-scan-delay-ms:60000}")
    void releaseExpiredReservations() {
        int released = service.releaseExpiredReservations();
        if (released > 0) log.info("Released {} expired usage reservations", released);
    }
}
