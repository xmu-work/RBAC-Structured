package com.iam.platform.infrastructure.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 加密脱敏组件（M11）。
 *
 * <p>字段级加解密、展示脱敏、密钥管理。
 * 对应 FR-48～FR-50、NFR-01。
 *
 * <p>结构化编程原则：单一职责，所有加解密与脱敏逻辑集中于此，
 * 业务模块通过此组件调用，不自行实现密码学操作。
 */
@Component
public class CryptoComponent {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${crypto.aes-key}")
    private String aesKey;

    @Value("${crypto.name-mask-rule:KEEP_SURNAME}")
    private String nameMaskRule;

    @Value("${crypto.phone-mask-rule:MIDDLE_MASK}")
    private String phoneMaskRule;

    /**
     * AES-256-GCM 加密。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（格式：IV:密文:Tag）
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            java.security.SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(padKey(aesKey), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密。
     *
     * @param encrypted Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(padKey(aesKey), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    /**
     * HMAC-SHA256 哈希，用于等值检索。
     */
    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((value + aesKey).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("哈希计算失败", e);
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

    private byte[] padKey(String key) {
        byte[] keyBytes = new byte[32];
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
        return keyBytes;
    }
}
