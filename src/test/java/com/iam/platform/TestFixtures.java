package com.iam.platform;

import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 测试公共数据：真实的加解密组件与常用实体。
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static CryptoComponent crypto() {
        CryptoComponent crypto = new CryptoComponent();
        ReflectionTestUtils.setField(crypto, "aesKey", "test-aes-key");
        ReflectionTestUtils.setField(crypto, "hmacKey", "test-hmac-key");
        return crypto;
    }

    public static IamUser user(Long id, String status, CryptoComponent crypto, String password) {
        IamUser user = new IamUser();
        user.setId(id);
        user.setUserNo("u" + id);
        user.setPasswordHash(crypto.hashPassword(password));
        user.setNameEnc(crypto.encrypt("张三"));
        user.setOrgId(1L);
        user.setAccountStatus(status);
        user.setIsDeleted(0);
        return user;
    }

    public static IamRole role(Long id, String status) {
        IamRole role = new IamRole();
        role.setId(id);
        role.setRoleCode("R" + id);
        role.setStatus(status);
        role.setIsDeleted(0);
        return role;
    }

    public static IamUserRole grant(Long userId, Long roleId, String status) {
        IamUserRole grant = new IamUserRole();
        grant.setUserId(userId);
        grant.setRoleId(roleId);
        grant.setStatus(status);
        return grant;
    }
}
