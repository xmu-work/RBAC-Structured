package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 功能点操作取值枚举（共 12 种）。
 *
 * <p>对应数据库 iam_function_point.action 字段与 SRS 4.4 FR-18。
 */
@Getter
public enum OperationAction {

    NONE("NONE", "无"),
    VIEW("VIEW", "查看"),
    ALL("ALL", "全部"),
    VIEW_EDIT("VIEW_EDIT", "查看/编辑"),
    VIEW_ADD("VIEW_ADD", "查看/新增"),
    VIEW_APPROVE("VIEW_APPROVE", "查看/审批"),
    VIEW_EXPORT("VIEW_EXPORT", "查看/导出"),
    VIEW_EDIT_APPROVE("VIEW_EDIT_APPROVE", "查看/编辑/审批"),
    VIEW_ADD_EDIT("VIEW_ADD_EDIT", "查看/新增/编辑"),
    VIEW_EDIT_EXPORT("VIEW_EDIT_EXPORT", "查看/编辑/导出"),
    VIEW_SELF("VIEW_SELF", "查看本人"),
    VIEW_EDIT_SELF("VIEW_EDIT_SELF", "查看/编辑本人");

    private final String code;
    private final String description;

    OperationAction(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OperationAction fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知操作取值: " + code));
    }
}
