package com.iam.platform.infrastructure.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密脱敏组件（M11）。
 *
 * <p>字段级加解密、展示脱敏、密钥管理。
 * 对应 FR-48～FR-50、NFR-01。
 *
 * <p>结构化编程原则：单一职责，所有加解密与脱敏逻辑集中于此，
 * 业务模块通过此组件调用，不自行实现密码学操作。
 *
 * <p>密钥管理：AES 加密与 HMAC 检索使用相互独立的密钥；密文带密钥版本前缀，
 * 密码哈希带算法与迭代次数，便于日后升级参数与渐进式重加密。
 */
@Component
public class CryptoComponent {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    /** 当前加密密钥版本号；更换密钥时递增，旧密文仍按旧版本解密（渐进式重加密） */
    private static final String KEY_VERSION = "v1";
    private static final String CIPHER_PREFIX = KEY_VERSION + ":";

    /** 密码哈希：算法标识与迭代次数内嵌，便于以后升级参数而不破坏旧哈希 */
    private static final String PWD_ALGO = "pbkdf2_sha256";
    private static final int PWD_ITERATIONS = 65536;
    private static final int PWD_KEY_LENGTH = 256;
    /** 兼容历史格式（Base64(盐):Base64(哈希)）所使用的迭代次数 */
    private static final int LEGACY_PWD_ITERATIONS = 65536;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${crypto.aes-key}")
    private String aesKey;

    /** 检索哈希（HMAC）专用密钥，绝不复用 AES 加密密钥 */
    @Value("${crypto.hmac-key}")
    private String hmacKey;

    @Value("${crypto.name-mask-rule:KEEP_SURNAME}")
    private String nameMaskRule;

    @Value("${crypto.phone-mask-rule:MIDDLE_MASK}")
    private String phoneMaskRule;

    /**
     * AES-256-GCM 加密。
     *
     * @param plaintext 明文
     * @return 带版本前缀的 Base64 密文（格式：v1:Base64(IV‖密文‖Tag)）
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            // getInstanceStrong() 在 Linux 服务器上可能因熵不足阻塞，用默认 SecureRandom 即可
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, derivedAesKeySpec(), gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密，兼容无版本前缀的历史密文。
     *
     * @param encrypted 带版本前缀的 Base64 密文，或历史 Base64 密文
     * @return 明文
     */
    public String decrypt(String encrypted) {
        try {
            boolean versioned = encrypted.startsWith(CIPHER_PREFIX);
            String payload = versioned ? encrypted.substring(CIPHER_PREFIX.length()) : encrypted;
            byte[] combined = Base64.getDecoder().decode(payload);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, versioned ? derivedAesKeySpec() : legacyAesKeySpec(), gcmSpec);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    /**
     * 真正的 HMAC-SHA256（使用独立的 crypto.hmac-key），用于等值检索。
     */
    public String hash(String value) {
        return hmacHex(hmacKey, value);
    }

    /**
     * 以指定密钥计算 HMAC-SHA256（十六进制小写），供开放 API 接入方签名校验使用。
     */
    public String hmacHex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    /**
     * 密码哈希：pbkdf2_sha256$迭代次数$Base64(盐)$Base64(哈希)，随机盐。
     */
    public String hashPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(pbkdf2(password, salt, PWD_ITERATIONS));
        return PWD_ALGO + "$" + PWD_ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    /**
     * 校验密码是否与哈希匹配（常量时间比较），兼容历史 盐:哈希 格式。
     */
    public boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (stored.contains("$")) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !PWD_ALGO.equals(parts[0])) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, Integer.parseInt(parts[1])));
        }
        if (!stored.contains(":")) {
            return false;
        }
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(expected, pbkdf2(password, salt, LEGACY_PWD_ITERATIONS));
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, PWD_KEY_LENGTH);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希失败", e);
        }
    }

    /**
     * 姓名脱敏：保留姓氏（如 张*）。
     */
    public String maskName(String realName) {
        if (realName == null || realName.isEmpty()) {
            return "";
        }
        if (realName.length() == 1) {
            return realName;
        }
        return realName.charAt(0) + "*".repeat(realName.length() - 1);
    }

    /**
     * 电话脱敏：中间四位掩码（如 138****5678）。
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** v1 版 AES 密钥：将口令经 SHA-256 派生为固定 256 位密钥，避免截断/补零带来的密钥弱化。 */
    private SecretKeySpec derivedAesKeySpec() {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(aesKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("派生 AES 密钥失败", e);
        }
    }

    /** 历史 AES 密钥：过长截断、过短补零，仅用于解密历史密文。 */
    private SecretKeySpec legacyAesKeySpec() {
        return new SecretKeySpec(padKey(aesKey), "AES");
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] padKey(String key) {
        byte[] keyBytes = new byte[32];
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
        return keyBytes;
    }
}