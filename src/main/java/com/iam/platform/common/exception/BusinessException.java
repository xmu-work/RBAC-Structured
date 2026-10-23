package com.iam.platform.common.exception;

import com.iam.platform.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>结构化编程原则：单一出口。所有业务规则违反统一抛出 BusinessException，
 * 由全局异常处理器转换为统一响应包。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
