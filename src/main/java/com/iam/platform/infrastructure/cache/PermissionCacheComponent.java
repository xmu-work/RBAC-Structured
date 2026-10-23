package com.iam.platform.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 权限缓存与分布式锁组件（M10 基础设施）。
 *
 * <p>权限快照缓存、令牌会话缓存、分布式锁。
 * 对应 FR-40（即时生效）、FR-41（并发互斥）、NFR-08。
 *
 * <p>结构化编程原则：统一缓存访问层，收敛 M08 与 M10 共享 Redis 的公共耦合。
 */
@Slf4j
@Component
public class PermissionCacheComponent {

    private final StringRedisTemplate redisTemplate;

    @Value("${permission.cache.ttl:300}")
    private long cacheTtl;

    @Value("${permission.cache.prefix:iam:perm}")
    private String cachePrefix;

    @Value("${permission.lock-prefix:iam:lock}")
    private String lockPrefix;

    public PermissionCacheComponent(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取权限快照缓存。
     *
     * @param userId          用户 ID
     * @param systemCode      系统编码
     * @param permVersion     权限版本号
     * @return 缓存的权限 JSON，未命中返回 null
     */
    public String getPermissionSnapshot(Long userId, String systemCode, Long permVersion) {
        String key = buildPermKey(userId, systemCode, permVersion);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 写入权限快照缓存。
     */
    public void putPermissionSnapshot(Long userId, String systemCode, Long permVersion, String json) {
        String key = buildPermKey(userId, systemCode, permVersion);
        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(cacheTtl));
    }

    /**
     * 失效用户在某系统的全部权限缓存。
     */
    public void invalidatePermission(Long userId, String systemCode) {
        String pattern = cachePrefix + ":" + userId + ":" + systemCode + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey  锁键（如 auth:{userId}）
     * @param timeout  超时秒数
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long timeout) {
        String key = lockPrefix + ":" + lockKey;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放分布式锁。
     */
    public void unlock(String lockKey) {
        String key = lockPrefix + ":" + lockKey;
        redisTemplate.delete(key);
    }

    private String buildPermKey(Long userId, String systemCode, Long permVersion) {
        return cachePrefix + ":" + userId + ":" + systemCode + ":" + permVersion;
    }
}
