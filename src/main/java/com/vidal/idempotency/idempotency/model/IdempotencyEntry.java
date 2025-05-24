package com.vidal.idempotency.idempotency.model;

import java.time.LocalDateTime;

public record IdempotencyEntry(
        Object result,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {

    public IdempotencyEntry(Object result, LocalDateTime expiresAt) {
        this(result, LocalDateTime.now(), expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public long getTimeToLiveSeconds() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }
}