package com.iam.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 功能点操作取值枚举（共 12 种）。
 *
 * <p>对应数据库 iam_function_point.action 字段与 SRS 4.4 FR-18。
 *
 * <p>每种取值映射为一组基础操作能力（VIEW/ADD/EDIT/APPROVE/EXPORT），
 * 判定时要求“已授予操作（iam_role_function.allowed_action）覆盖功能点声明的操作”，
 * 从而让查看、编辑、审批等差异真正参与决策，而不是只比对功能点 ID。
 */
@Getter
public enum OperationAction {

    NONE("NONE", "无", Set.of()),
    VIEW("VIEW", "查看", Set.of("VIEW")),
    ALL("ALL", "全部", Set.of("VIEW", "ADD", "EDIT", "APPROVE", "EXPORT")),
    VIEW_EDIT("VIEW_EDIT", "查看/编辑", Set.of("VIEW", "EDIT")),
    VIEW_ADD("VIEW_ADD", "查看/新增", Set.of("VIEW", "ADD")),
    VIEW_APPROVE("VIEW_APPROVE", "查看/审批", Set.of("VIEW", "APPROVE")),
    VIEW_EXPORT("VIEW_EXPORT", "查看/导出", Set.of("VIEW", "EXPORT")),
    VIEW_EDIT_APPROVE("VIEW_EDIT_APPROVE", "查看/编辑/审批", Set.of("VIEW", "EDIT", "APPROVE")),
    VIEW_ADD_EDIT("VIEW_ADD_EDIT", "查看/新增/编辑", Set.of("VIEW", "ADD", "EDIT")),
    VIEW_EDIT_EXPORT("VIEW_EDIT_EXPORT", "查看/编辑/导出", Set.of("VIEW", "EDIT", "EXPORT")),
    VIEW_SELF("VIEW_SELF", "查看本人", Set.of("VIEW")),
    VIEW_EDIT_SELF("VIEW_EDIT_SELF", "查看/编辑本人", Set.of("VIEW", "EDIT"));

    private final String code;
    private final String description;
    private final Set<String> capabilities;

    OperationAction(String code, String description, Set<String> capabilities) {
        this.code = code;
        this.description = description;
        this.capabilities = capabilities;
    }

    public static OperationAction fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知操作取值: " + code));
    }

    /** 当前授权是否覆盖功能点所声明的操作。 */
    public boolean covers(OperationAction required) {
        return capabilities.containsAll(required.capabilities);
    }

    /** 基础能力数量，用于同一功能点多角色授权时取“最宽松”的一项。 */
    public int capabilityCount() {
        return capabilities.size();
    }
}