package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 组织机构类型枚举。
 *
 * <p>对应数据库 iam_org.org_type 字段。
 * HQ=总公司 / BRANCH=分公司 / DEPT=部门 / PROJECT=项目部。
 */
@Getter
public enum OrgType {

    HQ("HQ", "总公司", 1),
    BRANCH("BRANCH", "分公司", 2),
    DEPT("DEPT", "部门", 3),
    PROJECT("PROJECT", "项目部", 4);

    private final String code;
    private final String description;
    private final int level;

    OrgType(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public static OrgType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知组织类型: " + code));
    }
}
