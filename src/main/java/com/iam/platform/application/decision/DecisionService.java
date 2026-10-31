package com.iam.platform.application.decision;

import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.OperationAction;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 策略决策点（PDP）应用服务（M08）。
 *
 * <p>统一鉴权入口：功能权限判定 + 数据范围计算。
 * 被网关与业务系统通过 OpenAPI 调用。
 * 对应 FR-23～FR-26、UC-06。
 *
 * <p>RBAC 判定链：用户正常 → 有效用户-角色绑定（未回收、在有效期内）→ 有效角色（含父角色继承）
 * → 角色-功能绑定（allowed_action 覆盖功能点声明的操作）→ 功能点属于该子系统且系统/模块/功能点均启用。
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

        // 2. 有效的用户-角色绑定（未回收、在有效期内）
        LocalDateTime now = LocalDateTime.now();
        Set<Long> directRoleIds = userRoleRepository.findByUserIdAndStatus(userId, GrantService.GRANT_ACTIVE).stream()
                .filter(ur -> isEffective(ur, now))
                .map(IamUserRole::getRoleId)
                .collect(Collectors.toSet());

        // 3. 有效角色 + 父角色（继承）
        Set<Long> roleIds = collectActiveRolesWithAncestors(directRoleIds);
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        // 4. 角色 → 功能点，合并同一功能点的 allowed_action（取最宽松）
        Map<Long, OperationAction> grantedActions = grantedActionsOf(roleIds);
        if (grantedActions.isEmpty()) {
            return Set.of();
        }

        // 5. 定位子系统及其启用模块
        Long systemId = systemRepository.findBySystemCode(systemCode)
                .filter(s -> s.getIsDeleted() == 0 && STATUS_ACTIVE.equals(s.getStatus()))
                .map(IamSystem::getId)
                .orElse(null);
        if (systemId == null) {
            return Set.of();
        }
        Set<Long> moduleIds = moduleRepository.findBySystemId(systemId).stream()
                .filter(m -> m.getIsDeleted() == 0 && STATUS_ACTIVE.equals(m.getStatus()))
                .map(IamModule::getId)
                .collect(Collectors.toSet());
        if (moduleIds.isEmpty()) {
            return Set.of();
        }

        // 6. 功能点启用、属于该子系统、且已授予操作覆盖功能点声明的操作
        return functionPointRepository.findAllById(grantedActions.keySet()).stream()
                .filter(fp -> fp.getIsDeleted() == 0 && STATUS_ACTIVE.equals(fp.getStatus()))
                .filter(fp -> moduleIds.contains(fp.getModuleId()))
                .filter(fp -> actionCovers(grantedActions.get(fp.getId()), fp.getAction()))
                .map(IamFunctionPoint::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 收集启用角色及其祖先（parent_id 链），visited 去重以防御继承成环。
     */
    private Set<Long> collectActiveRolesWithAncestors(Set<Long> directRoleIds) {
        Set<Long> result = new HashSet<>();
        Set<Long> toVisit = new HashSet<>(directRoleIds);
        while (!toVisit.isEmpty()) {
            List<IamRole> roles = roleRepository.findAllById(toVisit);
            Set<Long> next = new HashSet<>();
            for (IamRole role : roles) {
                if (role.getIsDeleted() != 0 || !RoleService.ROLE_ACTIVE.equals(role.getStatus())) {
                    continue;
                }
                if (!result.add(role.getId())) {
                    continue;
                }
                if (role.getParentId() != null) {
                    next.add(role.getParentId());
                }
            }
            toVisit = next;
        }
        return result;
    }

    /**
     * 角色功能绑定的 allowed_action 合并：NONE 不授权，同一功能点多角色取能力最多的一项。
     */
    private Map<Long, OperationAction> grantedActionsOf(Set<Long> roleIds) {
        Map<Long, OperationAction> grantedActions = new HashMap<>();
        for (IamRoleFunction rf : roleFunctionRepository.findByRoleIdIn(roleIds)) {
            OperationAction granted = parseAction(rf.getAllowedAction());
            if (granted == null || granted == OperationAction.NONE) {
                continue;
            }
            grantedActions.merge(rf.getFunctionPointId(), granted,
                    (left, right) -> left.capabilityCount() >= right.capabilityCount() ? left : right);
        }
        return grantedActions;
    }

    private boolean actionCovers(OperationAction granted, String requiredCode) {
        OperationAction required = parseAction(requiredCode);
        return granted != null && required != null && granted.covers(required);
    }

    private OperationAction parseAction(String code) {
        try {
            return OperationAction.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isEffective(IamUserRole grant, LocalDateTime now) {
        boolean started = grant.getEffectiveTime() == null || !grant.getEffectiveTime().isAfter(now);
        boolean notExpired = grant.getExpireTime() == null || grant.getExpireTime().isAfter(now);
        return started && notExpired;
    }
}