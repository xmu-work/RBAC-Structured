package com.iam.platform.application.openapi;

/**
 * 开放 API nonce 防重放存储。
 *
 * <p>记录已使用过的 (接入系统, nonce)，TTL 内重复出现即判定为重放。
 */
public interface ReplayGuard {

    /**
     * 尝试占用 nonce。
     *
     * @return true 表示首次使用（通过），false 表示已被使用（重放）
     */
    boolean tryUse(String appId, String nonce);
}