package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 权限变更类型枚举。
 *
 * <p>对应数据库 iam_permission_change_log.change_type 字段。
 */
@Getter
public enum ChangeType {

    GRANT("GRANT", "授权"),
    REVOKE("REVOKE", "回收"),
    SEAL("SEAL", "封存"),
    ROLE_UPDATE("ROLE_UPDATE", "角色变更");

    private final String code;
    private final String description;

    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChangeType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知变更类型: " + code));
    }
}
