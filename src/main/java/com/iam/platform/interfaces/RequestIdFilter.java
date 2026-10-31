package com.iam.platform.interfaces;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求追踪过滤器：把前端传入的 {@code X-Request-Id}（缺省时新生成）写入请求属性并回写响应头，
 * 供统一响应包 {@code Result.requestId} 回填与日志关联。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String ATTR_REQUEST_ID = "iam.requestId";
    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = normalize(request.getHeader(HEADER));
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);
        org.slf4j.MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove(MDC_KEY);
        }
    }

    /**
     * 当前请求的追踪 ID，供非 Web 层（如审计）使用；非请求线程返回 null。
     */
    public static String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(ATTR_REQUEST_ID, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    /**
     * 直接读取当前 Servlet 请求上的追踪 ID，供 DispatcherServlet 初始化前的过滤器使用。
     */
    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_REQUEST_ID);
        return value == null ? null : value.toString();
    }

    private static String normalize(String candidate) {
        if (candidate == null || candidate.isBlank()
                || candidate.length() > MAX_REQUEST_ID_LENGTH
                || !SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
