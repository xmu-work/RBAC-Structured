package com.iam.platform.infrastructure.cache;

import com.iam.platform.application.idempotency.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的幂等键存储。
 */
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String FP_PREFIX = "iam:idem:fp:";
    private static final String RESP_PREFIX = "iam:idem:resp:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Status acquire(String key, String fingerprint, long ttlSeconds) {
        String fpKey = FP_PREFIX + key;
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        Boolean created = redisTemplate.opsForValue().setIfAbsent(fpKey, fingerprint, ttl);
        if (Boolean.TRUE.equals(created)) {
            // 指纹过期后可能留下旧响应；新的执行必须先清除，避免误重放上一轮结果。
            redisTemplate.delete(RESP_PREFIX + key);
            return Status.ACQUIRED;
        }
        String existing = redisTemplate.opsForValue().get(fpKey);
        if (existing == null) {
            // 占用记录恰好过期，重试一次
            Boolean retry = redisTemplate.opsForValue().setIfAbsent(fpKey, fingerprint, ttl);
            if (Boolean.TRUE.equals(retry)) {
                redisTemplate.delete(RESP_PREFIX + key);
                return Status.ACQUIRED;
            }
            return Status.IN_PROGRESS;
        }
        if (!existing.equals(fingerprint)) {
            return Status.CONFLICT;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(RESP_PREFIX + key)) ? Status.COMPLETED : Status.IN_PROGRESS;
    }

    @Override
    public String getResponse(String key) {
        return redisTemplate.opsForValue().get(RESP_PREFIX + key);
    }

    @Override
    public void complete(String key, int status, String body, long ttlSeconds) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redisTemplate.opsForValue().set(RESP_PREFIX + key,
                status + "\n" + (body == null ? "" : body), ttl);
        redisTemplate.expire(FP_PREFIX + key, ttl);
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(List.of(FP_PREFIX + key, RESP_PREFIX + key));
    }
}
