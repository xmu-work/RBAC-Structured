package com.iam.platform.infrastructure.cache;

import com.iam.platform.application.openapi.ReplayGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的开放 API nonce 防重放实现。
 *
 * <p>SETNX + TTL：同一 (接入系统, nonce) 在 TTL 内只能成功占用一次。
 */
@Component
@RequiredArgsConstructor
public class RedisReplayGuard implements ReplayGuard {

    private static final String KEY_PREFIX = "iam:nonce:";

    private final StringRedisTemplate redisTemplate;

    @Value("${open-api.nonce-ttl:300}")
    private long nonceTtlSeconds;

    @Override
    public boolean tryUse(String appId, String nonce) {
        String key = KEY_PREFIX + appId + ":" + nonce;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(nonceTtlSeconds));
        return Boolean.TRUE.equals(created);
    }
}