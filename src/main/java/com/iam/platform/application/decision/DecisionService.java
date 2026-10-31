package com.iam.platform.application.decision;

import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.OperationAction;
import com.iam.platform.common.enums.RoleType;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 策略决策点（PDP）应用服务（M08）。
 *
 * <p>统一鉴权入口：只做功能权限判定。
 * 被网关与业务系统通过 OpenAPI 调用。
 * 对应功能权限查询与判定。
 *
 * <p>RBAC 判定链：用户正常 → 有效用户-角色绑定（未回收、在有效期内）→ 有效角色（含父角色继承）
 * → 角色-功能绑定（allowed_action 覆盖功能点声明的操作）→ 功能点属于该子系统且系统/模块/功能点均启用。
 * 角色→功能点的候选由单条 join 查询一次性取得，避免多轮查库。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DecisionService {

    private final IamUserRepository userRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamRoleRepository roleRepository;
    private final IamRoleFunctionRepository roleFunctionRepository;

    /**
     * 功能权限判定。
     *
     * @param userId     用户 ID
     * @param systemCode 子系统编码
     * @param functionPointId 功能点 ID
     * @return 允许返回 true
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String systemCode, Long functionPointId) {
        return getUserFunctionPoints(userId, systemCode).contains(functionPointId);
    }

    /**
     * 批量功能权限判定：只计算一次用户权限集合，再逐个比对。
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Set<Long> getUserFunctionPoints(Long userId, String systemCode) {
        // 1. 用户必须存在且为正常状态
        boolean userNormal = userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0 && AccountStatus.NORMAL.getCode().equals(u.getAccountStatus()))
                .isPresent();
        if (!userNormal) {
            return Set.of();
        }

        // 2. 有效角色（含父角色继承）
        Set<Long> roleIds = activeRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        // 3. 单条 join 查询取候选功能点（已按系统/模块/功能点启用过滤），并合并 allowed_action 覆盖判定
        Set<Long> granted = new HashSet<>();
        for (IamRoleFunctionRepository.GrantedPointView view
                : roleFunctionRepository.findGrantedPoints(roleIds, systemCode)) {
            OperationAction allowed = parseAction(view.getAllowedAction());
            if (allowed == null || allowed == OperationAction.NONE) {
                continue;
            }
            OperationAction required = parseAction(view.getRequiredAction());
            if (required != null && allowed.covers(required)) {
                granted.add(view.getFunctionPointId());
            }
        }
        return granted;
    }

    /**
     * 用户的有效角色集合（含父角色继承，visited 去重以防御继承成环）。
     */
    private Set<Long> activeRoleIds(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> directRoleIds = userRoleRepository.findByUserIdAndStatus(userId, GrantService.GRANT_ACTIVE).stream()
                .filter(ur -> isEffective(ur, now))
                .map(IamUserRole::getRoleId)
                .collect(Collectors.toSet());
        return collectActiveRolesWithAncestors(directRoleIds);
    }

    private Set<Long> collectActiveRolesWithAncestors(Set<Long> directRoleIds) {
        Set<Long> result = new HashSet<>();
        Set<Long> toVisit = new HashSet<>(directRoleIds);
        while (!toVisit.isEmpty()) {
            List<IamRole> roles = roleRepository.findAllById(toVisit);
            Set<Long> next = new HashSet<>();
            for (IamRole role : roles) {
                if (role.getIsDeleted() != 0
                        || !RoleType.FUNC.getCode().equals(role.getRoleType())
                        || !RoleService.ROLE_ACTIVE.equals(role.getStatus())) {
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
