package com.iam.platform.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iam.platform.common.enums.ErrorCode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应包。
 *
 * <p>对应 API 设计说明书 1.3 统一响应包：
 * <pre>
 * { "code": 0, "message": "success", "data": {}, "request_id": "...", "timestamp": "..." }
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 业务码，0 表示成功 */
    private int code;

    /** 人类可读信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 请求追踪 ID */
    private String requestId;

    /** 服务端时间 */
    private LocalDateTime timestamp;

    private Result() {
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = message;
        r.timestamp = LocalDateTime.now();
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = LocalDateTime.now();
        return r;
    }
}
