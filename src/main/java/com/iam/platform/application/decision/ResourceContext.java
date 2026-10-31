package com.iam.platform.application.decision;

/**
 * 鉴权请求的资源上下文（判断“本人 / 本部门 / 本项目”所需）。
 * 字段为空表示该维度不参与数据范围判定。
 */
public record ResourceContext(Long targetUserId, Long targetOrgId, Long targetProjectId) {
}