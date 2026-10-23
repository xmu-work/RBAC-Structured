package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 数据范围类型枚举。
 *
 * <p>对应数据库 iam_data_scope.scope_code 字段。
 * 全公司 / 本分公司 / 本部门 / 本项目 / 本人。
 */
@Getter
public enum DataScopeType {

    ALL("ALL", "全公司"),
    BRANCH("BRANCH", "本分公司"),
    DEPT("DEPT", "本部门"),
    PROJECT("PROJECT", "本项目"),
    SELF("SELF", "本人");

    private final String code;
    private final String description;

    DataScopeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DataScopeType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知数据范围: " + code));
    }
}
