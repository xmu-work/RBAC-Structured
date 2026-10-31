package com.iam.platform.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 权限缓存与分布式锁组件（M10 基础设施）。
 *
 * <p>权限快照缓存、令牌会话缓存、分布式锁。
 * 对应 FR-40（即时生效）、FR-41（并发互斥）、NFR-08。
 *
 * <p>结构化编程原则：统一缓存访问层，收敛 M08 与 M10 共享 Redis 的公共耦合。
 *
 * <p>缓存失效策略：采用“不可变快照 + 版本号”而非扫描删除。快照 key 内嵌权限版本号，
 * 授权变更时只递增版本号即可让旧快照不可达，随后由 TTL 自然淘汰，
 * 避免在生产环境使用 {@code KEYS} 通配扫描阻塞 Redis。
 *
 * <p>分布式锁：锁值保存随机 owner token，释放时用 Lua 原子比较并删除，
 * 避免“锁因超时过期、他人获取后被原持有者误删”的问题。
 */
@Slf4j
@Component
public class PermissionCacheComponent {

    /** 解锁脚本：仅当锁值等于持有者 token 时才删除，保证原子性。 */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

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
     * 当前权限版本号（全局）。
     */
    public long currentVersion() {
        String value = redisTemplate.opsForValue().get(versionKey());
        return value == null ? 0L : Long.parseLong(value);
    }

    /**
     * 递增权限版本号，使既有快照立即失效（无需扫描删除）。
     */
    public long bumpVersion() {
        Long version = redisTemplate.opsForValue().increment(versionKey());
        return version == null ? 0L : version;
    }

    /**
     * 获取权限快照缓存。
     *
     * @param userId     用户 ID
     * @param systemCode 系统编码
     * @param version    权限版本号（取自 {@link #currentVersion()}）
     * @return 缓存的权限 JSON，未命中返回 null
     */
    public String getPermissionSnapshot(Long userId, String systemCode, long version) {
        return redisTemplate.opsForValue().get(buildPermKey(userId, systemCode, version));
    }

    /**
     * 写入权限快照缓存。
     */
    public void putPermissionSnapshot(Long userId, String systemCode, long version, String json) {
        redisTemplate.opsForValue().set(buildPermKey(userId, systemCode, version), json, Duration.ofSeconds(cacheTtl));
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey       锁键（如 auth:{userId}）
     * @param timeoutSeconds 锁超时秒数
     * @return 获取成功返回 owner token（释放时须回传），失败返回 null
     */
    public String tryLock(String lockKey, long timeoutSeconds) {
        String key = lockPrefix + ":" + lockKey;
        String ownerToken = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, ownerToken, Duration.ofSeconds(timeoutSeconds));
        return Boolean.TRUE.equals(ok) ? ownerToken : null;
    }

    /**
     * 释放分布式锁（Lua 原子比较并删除，只释放自己持有的锁）。
     *
     * @param ownerToken {@link #tryLock} 返回的持有者 token
     * @return 是否成功释放
     */
    public boolean unlock(String lockKey, String ownerToken) {
        if (ownerToken == null) {
            return false;
        }
        String key = lockPrefix + ":" + lockKey;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), ownerToken);
        return result != null && result > 0;
    }

    private String versionKey() {
        return cachePrefix + ":ver";
    }

    private String buildPermKey(Long userId, String systemCode, long version) {
        return cachePrefix + ":" + userId + ":" + systemCode + ":" + version;
    }
}