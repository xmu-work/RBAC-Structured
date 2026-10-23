package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 账号状态枚举。
 *
 * <p>对应数据库 iam_user.account_status 字段。
 */
@Getter
public enum AccountStatus {

    NORMAL("NORMAL", "正常"),
    DISABLED("DISABLED", "停用"),
    SEALED("SEALED", "封存（离职）"),
    LOCKED("LOCKED", "锁定（异常）");

    private final String code;
    private final String description;

    AccountStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AccountStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知账号状态: " + code));
    }
}
