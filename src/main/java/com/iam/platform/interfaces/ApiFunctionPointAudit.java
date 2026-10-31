package com.iam.platform.interfaces;

import com.iam.platform.application.permission.FunctionPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 启动期接口登记检查（fail-closed 配置自检）。
 *
 * <p>扫描全部管理端 Controller 映射，凡未在 iam_function_point 登记 (api_path, api_method)
 * 的接口都会在启动日志中以 WARN 列出——这些接口在运行期会被 {@link AuthInterceptor} 按
 * fail-closed 策略直接拒绝，便于管理员及时发现“新增接口忘了登记”。
 *
 * <p>白名单（登录、登出、令牌校验、自助改密、开放 API、监控/健康检查）不参与检查。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiFunctionPointAudit {

    private static final Set<String> IGNORED_PREFIXES = Set.of("/open/", "/actuator", "/error", "/druid");
    private static final Set<String> IGNORED_PATTERNS =
            Set.of("/auth/login", "/auth/logout", "/auth/validate", "/auth/password");

    private final ApplicationContext applicationContext;
    private final FunctionPermissionService functionPermissionService;

    @EventListener(ContextRefreshedEvent.class)
    public void auditUnregisteredApis() {
        RequestMappingHandlerMapping mapping;
        try {
            mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        } catch (Exception e) {
            log.debug("未找到 RequestMappingHandlerMapping，跳过接口登记检查");
            return;
        }

        List<String> missing = new ArrayList<>();
        mapping.getHandlerMethods().forEach((info, handler) ->
                patternsOf(info).forEach(pattern -> {
                    if (isIgnored(pattern)) {
                        return;
                    }
                    for (String method : methodsOf(info)) {
                        if (functionPermissionService.findByApi(pattern, method).isEmpty()) {
                            missing.add(method + " " + pattern);
                        }
                    }
                }));

        if (missing.isEmpty()) {
            log.info("接口登记检查通过：全部管理端接口均已登记为功能点");
        } else {
            log.warn("有 {} 个管理端接口未登记为功能点，运行期将按 fail-closed 策略被拒绝，请补充登记：\n  {}",
                    missing.size(), String.join("\n  ", missing));
        }
    }

    private Set<String> patternsOf(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() == null) {
            return Set.of();
        }
        return info.getPathPatternsCondition().getPatternValues();
    }

    private Set<String> methodsOf(RequestMappingInfo info) {
        Set<String> methods = new HashSet<>();
        for (RequestMethod method : info.getMethodsCondition().getMethods()) {
            methods.add(method.name());
        }
        return methods;
    }

    private boolean isIgnored(String pattern) {
        return IGNORED_PATTERNS.contains(pattern) || IGNORED_PREFIXES.stream().anyMatch(pattern::startsWith);
    }
}