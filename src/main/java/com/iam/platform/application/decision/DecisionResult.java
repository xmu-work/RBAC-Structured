package com.iam.platform.application.decision;

import java.util.List;

/**
 * 带资源上下文的鉴权结果。
 *
 * @param functionAllowed 功能权限是否通过
 * @param allowed         综合结果（功能权限 + 数据范围）
 * @param dataScopes      用户在该子系统的有效数据范围
 */
public record DecisionResult(boolean functionAllowed, boolean allowed, List<DataScopeView> dataScopes) {
}