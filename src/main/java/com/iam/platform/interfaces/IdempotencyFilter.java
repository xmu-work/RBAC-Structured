package com.iam.platform.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iam.platform.application.idempotency.IdempotencyStore;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 幂等过滤器：前端写请求携带 {@code Idempotency-Key} 时，
 * 相同请求重试重放首次响应，不同请求体复用同一 key 返回 1004，避免重复执行。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final int MAX_KEY_LENGTH = 128;
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9._:-]+");

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Value("${idempotency.ttl:600}")
    private long ttlSeconds;

    @Value("${idempotency.max-body-bytes:1048576}")
    private int maxBodyBytes;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank() || !MUTATING_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (key.length() > MAX_KEY_LENGTH || !SAFE_KEY.matcher(key).matches()) {
            writeError(response, HttpStatus.BAD_REQUEST, ErrorCode.PARAM_INVALID,
                    "Idempotency-Key 长度或格式非法", RequestIdFilter.requestId(request));
            return;
        }

        // 先解析表单参数，再读取原始请求体；这样表单请求仍可由后续 MVC 参数绑定读取。
        Map<String, String[]> parameters = request.getParameterMap();
        byte[] body = request.getInputStream().readNBytes(maxBodyBytes + 1);
        if (body.length > maxBodyBytes) {
            writeError(response, HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PARAM_INVALID,
                    "幂等请求体超过大小上限", RequestIdFilter.requestId(request));
            return;
        }
        HttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
        String fingerprint = fingerprint(wrappedRequest, parameters, body);

        IdempotencyStore.Status status;
        try {
            status = idempotencyStore.acquire(key, fingerprint, ttlSeconds);
        } catch (DataAccessException ex) {
            logger.error("幂等存储不可用", ex);
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE,
                    "幂等服务暂不可用，请稍后重试", RequestIdFilter.requestId(request));
            return;
        }

        switch (status) {
            case CONFLICT -> {
                writeError(response, HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT,
                        "幂等键已用于不同的请求", RequestIdFilter.requestId(request));
                return;
            }
            case IN_PROGRESS -> {
                writeError(response, HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT,
                        "相同请求正在处理中，请稍后重试", RequestIdFilter.requestId(request));
                return;
            }
            case COMPLETED -> {
                replay(response, idempotencyStore.getResponse(key), RequestIdFilter.requestId(request));
                return;
            }
            default -> {
                // ACQUIRED：继续处理
            }
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrapper);
            int responseStatus = response.getStatus();
            if (responseStatus < 400) {
                idempotencyStore.complete(key, responseStatus,
                        new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8), ttlSeconds);
            } else {
                idempotencyStore.release(key);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            idempotencyStore.release(key);
            throw e;
        } finally {
            wrapper.copyBodyToResponse();
        }
    }

    private String fingerprint(HttpServletRequest request, Map<String, String[]> parameters, byte[] body) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(request.getMethod()).append('\n')
                .append(request.getRequestURI()).append('\n')
                .append(request.getContentType() == null ? "" : request.getContentType()).append('\n')
                .append(authorizationDigest(request.getHeader("Authorization"))).append('\n');

        parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    canonical.append(entry.getKey()).append('=');
                    String[] values = entry.getValue() == null ? new String[0] : entry.getValue();
                    Arrays.stream(values).sorted().forEach(value -> canonical.append(value).append(','));
                    canonical.append(';');
                });
        canonical.append('\n').append(sha256(body));
        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    private String authorizationDigest(String authorization) {
        return authorization == null ? "" : sha256(authorization);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, ErrorCode errorCode,
                            String message, String requestId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(errorCode, message);
        result.setRequestId(requestId);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void replay(HttpServletResponse response, String stored, String requestId) throws IOException {
        int status = HttpStatus.OK.value();
        String body = "";
        if (stored != null) {
            int separator = stored.indexOf('\n');
            if (separator >= 0) {
                status = Integer.parseInt(stored.substring(0, separator));
                body = stored.substring(separator + 1);
            } else {
                body = stored;
            }
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(withCurrentRequestId(body, requestId));
    }

    private String withCurrentRequestId(String body, String requestId) throws IOException {
        if (body == null || body.isBlank() || requestId == null) {
            return body;
        }
        try {
            if (objectMapper.readTree(body) instanceof ObjectNode result) {
                result.put("requestId", requestId);
                return objectMapper.writeValueAsString(result);
            }
        } catch (IOException ignored) {
            // 非 JSON 响应保持原样；统一响应包正常情况下均为 JSON。
        }
        return body;
    }
}
