package com.christn.salesinventoryapi.auth.jobs;

import com.christn.salesinventoryapi.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@ComponentScan
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpired() {
        refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now(), LocalDateTime.now().minusDays(7));
    }
}
