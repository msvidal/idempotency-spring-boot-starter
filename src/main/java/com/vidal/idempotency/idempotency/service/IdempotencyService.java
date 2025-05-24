package com.vidal.idempotency.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidal.idempotency.idempotency.model.IdempotencyEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${idempotency.ttl.minutes:5}")
    private int defaultTtlMinutes;

    @Value("${idempotency.key-pattern:*}")
    private String keyPattern;

    public IdempotencyService( RedisTemplate<String, Object> redisTemplate,
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void store(String key, Object result, Integer ttlMinutes) {
        try {

            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusMinutes(ttlMinutes != null ? ttlMinutes : defaultTtlMinutes);

            IdempotencyEntry entry = new IdempotencyEntry(result, expiresAt);

            Duration ttl = Duration.ofMinutes(ttlMinutes != null ? ttlMinutes : defaultTtlMinutes);
            redisTemplate.opsForValue().set(key, entry, ttl);

            log.info("Entrada de idempotência armazenada no Redis - Chave: {}, TTL: {}min", key, ttl.toMinutes());

        } catch (Exception e) {
            throw new RuntimeException("Error storing result in Redis", e);
        }
    }

    public IdempotencyEntry retrieve(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached == null) {
                log.debug("Item não encontrado no cache para a chave: {}", key);
                return null;
            }

            IdempotencyEntry entry;
            if (cached instanceof IdempotencyEntry) {
                entry = (IdempotencyEntry) cached;
            } else {
                String jsonString = objectMapper.writeValueAsString(cached);
                entry = objectMapper.readValue(jsonString, IdempotencyEntry.class);
            }

            if (entry.isExpired()) {
                redisTemplate.delete(key);
                log.debug("Removed expired entry for key: {}", key);
                return null;
            }

            log.debug("Item encontrado no cache para a chave: {} (TTL: {}s)",
                    key, entry.getTimeToLiveSeconds());
            return entry;

        } catch (Exception e) {
            log.error("Error retrieving from Redis for key: {}", key, e);
            return null;
        }
    }

    public boolean remove(String key) {
        try {
            Boolean removed = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(removed)) {
                log.info("Manually removed entry for key: {}", key);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error removing key from Redis: {}", key, e);
            return false;
        }
    }
}