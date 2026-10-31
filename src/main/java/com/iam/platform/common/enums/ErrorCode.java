package com.iam.platform.common.enums;

import lombok.Getter;

/**
 * 统一错误码表。
 *
 * <p>对应 API 设计说明书 1.4 统一错误码表。
 */
@Getter
public enum ErrorCode {

    // 0 成功
    SUCCESS(0, "success", 200),

    // 1xxx 参数/校验
    PARAM_INVALID(1001, "参数校验失败", 400),
    PARAM_MISSING(1002, "必填参数缺失", 400),
    PARAM_FORMAT_ERROR(1003, "格式错误", 400),
    IDEMPOTENCY_CONFLICT(1004, "幂等键冲突且请求体不一致", 409),
    RESOURCE_NOT_FOUND(1005, "资源不存在", 404),

    // 2xxx 认证授权
    UNAUTHENTICATED(2001, "未登录或未携带令牌", 401),
    TOKEN_INVALID(2002, "令牌无效", 401),
    TOKEN_EXPIRED(2003, "令牌过期", 401),
    TOKEN_REVOKED(2004, "令牌已吊销", 401),
    ACCOUNT_SEALED(2005, "账号已封存（离职）", 403),
    ACCOUNT_DISABLED(2006, "账号停用或锁定", 403),
    NO_FUNCTION_PERM(2007, "无功能权限", 403),
    SIGNATURE_INVALID(2009, "对外接口签名校验失败", 401),
    REPLAY_DETECTED(2010, "防重放命中", 401),
    APP_DISABLED(2011, "业务系统接入已停用", 403),

    // 3xxx 业务规则
    ROLE_MUTEX_CONFLICT(3001, "角色互斥冲突", 409),
    ROLE_INHERIT_CYCLE(3002, "角色继承成环", 409),
    ORG_MOVE_INVALID(3003, "组织节点移动非法", 409),
    USER_ALREADY_SEALED(3004, "用户已封存", 409),
    GRANT_DUPLICATE(3005, "重复授权", 409),
    APPROVAL_REQUIRED(3006, "需审批流", 409),
    LOCK_ACQUIRE_FAILED(3008, "分布式锁获取失败", 409),
    RESOURCE_DUPLICATE(3009, "资源已存在", 409),
    // 操作对象（而非当前登录人）的账号状态不允许该操作；不能用 2005/2006，否则前端会把操作人强制登出
    TARGET_USER_ABNORMAL(3010, "目标用户非正常状态", 409),

    // 4xxx 限流
    RATE_LIMITED(4001, "触发限流", 429),

    // 5xxx 服务器
    INTERNAL_ERROR(5000, "服务器内部错误", 500),
    SERVICE_UNAVAILABLE(5001, "依赖服务不可用", 503),
    DEGRADED(5002, "决策服务降级", 200);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
