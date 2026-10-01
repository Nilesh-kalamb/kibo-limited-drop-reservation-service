package com.kibo.drop.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpiryScheduler {
    private final HoldService holdService;

    public HoldExpiryScheduler(HoldService holdService) {
        this.holdService = holdService;
    }

    @Scheduled(fixedDelayString = "${app.expiry-scan-ms:10000}")
    public void expire() {
        holdService.expireHolds();
    }
}
