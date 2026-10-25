package com.iam.platform.application.decision;

import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 策略决策点（PDP）应用服务（M08）。
 *
 * <p>统一鉴权入口：功能权限判定 + 数据范围计算。
 * 被网关与业务系统通过 OpenAPI 调用。
 * 对应 FR-23～FR-26、UC-06。
 *
 * <p>结构化编程原则：决策过程为"收集策略→合并→判定"的线性流程，
 * 不使用面向对象的多态分发，保持过程内聚。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DecisionService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final IamUserRepository userRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamRoleRepository roleRepository;
    private final IamRoleFunctionRepository roleFunctionRepository;
    private final IamFunctionPointRepository functionPointRepository;
    private final IamModuleRepository moduleRepository;
    private final IamSystemRepository systemRepository;

    /**
     * 功能权限判定。
     *
     * @param userId     用户 ID
     * @param systemCode 子系统编码
     * @param functionPointId 功能点 ID
     * @return 允许返回 true
     */
    public boolean hasPermission(Long userId, String systemCode, Long functionPointId) {
        return getUserFunctionPoints(userId, systemCode).contains(functionPointId);
    }

    /**
     * 批量功能权限判定：只计算一次用户权限集合，再逐个比对。
     */
    public PermissionCheckResult checkPermissions(Long userId, String systemCode, List<Long> functionPointIds) {
        Set<Long> granted = getUserFunctionPoints(userId, systemCode);
        PermissionCheckResult result = new PermissionCheckResult();
        for (Long id : functionPointIds) {
            result.getResults().put(id, granted.contains(id));
        }
        return result;
    }

    /**
     * 查询用户在某子系统下已授权的功能点 ID 集合。
     *
     * <p>RBAC0 判定流程：
     * 用户正常 → 有效的用户-角色绑定（未回收、在有效期内）→ 有效角色 →
     * 角色-功能绑定 → 属于该子系统且有效的功能点。任一步为空即返回空集合。
     */
    // ponytail: 每次判定 5~6 次查库，高并发时在此加 Redis 权限快照缓存（PermissionCacheComponent），授权变更时失效
    public Set<Long> getUserFunctionPoints(Long userId, String systemCode) {
        // 1. 用户必须存在且为正常状态
        boolean userNormal = userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0 && AccountStatus.NORMAL.getCode().equals(u.getAccountStatus()))
                .isPresent();
        if (!userNormal) {
            return Set.of();
        }

        // 2. 有效的用户-角色绑定
        LocalDateTime now = LocalDateTime.now();
        Set<Long> grantedRoleIds = userRoleRepository.findByUserIdAndStatus(userId, GrantService.GRANT_ACTIVE).stream()
                .filter(ur -> isEffective(ur, now))
                .map(IamUserRole::getRoleId)
                .collect(Collectors.toSet());

        // 3. 过滤掉停用或已删除的角色
        Set<Long> roleIds = roleRepository.findAllById(grantedRoleIds).stream()
                .filter(r -> r.getIsDeleted() == 0 && RoleService.ROLE_ACTIVE.equals(r.getStatus()))
                .map(IamRole::getId)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        // 4. 角色 → 功能点
        Set<Long> pointIds = roleFunctionRepository.findByRoleIdIn(roleIds).stream()
                .map(IamRoleFunction::getFunctionPointId)
                .collect(Collectors.toSet());

        // 5. 只保留属于该子系统、且有效的功能点
        Set<Long> moduleIds = systemRepository.findBySystemCode(systemCode)
                .map(s -> moduleRepository.findBySystemId(s.getId()).stream()
                        .map(IamModule::getId)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        return functionPointRepository.findAllById(pointIds).stream()
                .filter(fp -> moduleIds.contains(fp.getModuleId()))
                .filter(fp -> fp.getIsDeleted() == 0 && STATUS_ACTIVE.equals(fp.getStatus()))
                .map(IamFunctionPoint::getId)
                .collect(Collectors.toSet());
    }

    private boolean isEffective(IamUserRole grant, LocalDateTime now) {
        boolean started = grant.getEffectiveTime() == null || !grant.getEffectiveTime().isAfter(now);
        boolean notExpired = grant.getExpireTime() == null || grant.getExpireTime().isAfter(now);
        return started && notExpired;
    }
}
