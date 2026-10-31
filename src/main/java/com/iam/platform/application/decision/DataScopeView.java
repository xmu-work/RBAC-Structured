package com.iam.platform.application.decision;

/**
 * 用户在某子系统下的有效数据范围（对外视图）。
 *
 * @param scopeType ALL / BRANCH / DEPT / PROJECT / SELF
 * @param orgId     绑定的组织（BRANCH/DEPT 时使用，其余为空）
 * @param projectId 绑定的项目（PROJECT 时使用，其余为空）
 */
public record DataScopeView(String scopeType, Long orgId, Long projectId) {

    public String key() {
        return scopeType + ":" + orgId + ":" + projectId;
    }
}