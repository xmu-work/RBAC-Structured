package com.iam.platform.interfaces;

import com.iam.platform.application.api.ApiService;
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
 * 启动期 API 登记检查。
 *
 * <p>未登记到 iam_api、未映射前端权限或未受保护的管理端接口会进入告警列表，
 * 运行期由鉴权拦截器按 fail-closed 拒绝。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRegistrationAudit {

    private static final Set<String> IGNORED_PREFIXES = Set.of("/open/", "/actuator", "/error", "/druid");
    private static final Set<String> IGNORED_PATTERNS =
            Set.of("/auth/login", "/auth/logout", "/auth/validate", "/auth/password");

    private final ApplicationContext applicationContext;
    private final ApiService apiService;

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
                        if (apiService.findByRequest(pattern, method)
                                .filter(api -> api.getPermissionId() != null).isEmpty()) {
                            missing.add(method + " " + pattern);
                        }
                    }
                }));

        if (missing.isEmpty()) {
            log.info("API 登记检查通过：全部管理端接口均已登记并映射到前端权限");
        } else {
            log.warn("有 {} 个管理端 API 未登记或未映射到前端权限，运行期将按 fail-closed 策略拒绝：\n  {}",
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
