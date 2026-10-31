package com.iam.platform.application.idempotency;

/**
 * 幂等键存储：用于写请求重试时避免重复执行，并重放首次响应。
 */
public interface IdempotencyStore {

    enum Status {
        /** 首次占用成功，可继续处理 */
        ACQUIRED,
        /** 相同指纹的请求正在处理中 */
        IN_PROGRESS,
        /** 相同指纹的请求已完成，可重放响应 */
        COMPLETED,
        /** 幂等键被不同指纹的请求占用 */
        CONFLICT
    }

    /**
     * 尝试占用幂等键。
     *
     * @param key         幂等键（Idempotency-Key）
     * @param fingerprint 请求指纹（方法、路径、调用者、参数和请求体哈希）
     * @param ttlSeconds  记录有效期
     */
    Status acquire(String key, String fingerprint, long ttlSeconds);

    /**
     * 读取首次成功响应（格式：状态码 + '\n' + 响应体），无则返回 null。
     */
    String getResponse(String key);

    /**
     * 记录成功响应，供后续重试重放。
     */
    void complete(String key, int status, String body, long ttlSeconds);

    /**
     * 释放占用（处理失败时调用，允许重试）。
     */
    void release(String key);
}
