package com.iam.platform.interfaces;

import com.iam.platform.common.result.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应包 requestId 回填：把 {@link RequestIdFilter} 写入的请求追踪 ID 填进 {@code Result.requestId}，
 * 实现前后端请求 ID 闭环。
 */
@ControllerAdvice
public class ResultRequestIdAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result && result.getRequestId() == null) {
            result.setRequestId(RequestIdFilter.currentRequestId());
        }
        return body;
    }
}