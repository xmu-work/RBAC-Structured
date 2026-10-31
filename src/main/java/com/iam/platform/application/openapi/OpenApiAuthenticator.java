package com.iam.platform.application.openapi;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.IamSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 开放 API 接入方认证（M12 接入管理）。
 *
 * <p>校验接入方签名：系统编码 + 时间戳 + nonce + HMAC-SHA256 签名，并做时间窗与防重放校验。
 * 对应《目前项目的问题》P0-2：开放 API 原先没有“调用方是谁”的认证。
 *
 * <p>签名算法：signature = hex(HMAC-SHA256(appSecret, appId + "\n" + timestamp + "\n" + nonce))，
 * 其中 timestamp 为 epoch 毫秒。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiAuthenticator {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final IamSystemRepository systemRepository;
    private final ReplayGuard replayGuard;
    private final CryptoComponent cryptoComponent;

    @Value("${open-api.timestamp-tolerance:300}")
    private long timestampToleranceSeconds;

    public void verify(String appId, String timestamp, String nonce, String signature) {
        if (isBlank(appId) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID, "缺少接入方签名请求头");
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID, "时间戳格式非法");
        }
        if (Math.abs(System.currentTimeMillis() - ts) > timestampToleranceSeconds * 1000) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID, "请求时间戳超出允许范围");
        }

        IamSystem system = systemRepository.findBySystemCode(appId)
                .filter(s -> s.getIsDeleted() == 0)
                .filter(s -> STATUS_ACTIVE.equals(s.getStatus()))
                .filter(s -> !isBlank(s.getAppSecret()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNATURE_INVALID, "接入系统不存在或未配置密钥: " + appId));

        String expected = cryptoComponent.hmacHex(system.getAppSecret(), canonical(appId, timestamp, nonce));
        if (!constantTimeEquals(expected, signature)) {
            throw new BusinessException(ErrorCode.SIGNATURE_INVALID, "签名校验失败");
        }
        if (!replayGuard.tryUse(appId, nonce)) {
            throw new BusinessException(ErrorCode.REPLAY_DETECTED, "nonce 已被使用");
        }
    }

    private String canonical(String appId, String timestamp, String nonce) {
        return appId + "\n" + timestamp + "\n" + nonce;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}