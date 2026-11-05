package com.iam.platform.application.role;

import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.log.LogService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.enums.OperationAction;
import com.iam.platform.common.enums.RoleType;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRolePermission;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamPermissionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRolePermissionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import com.iam.platform.interfaces.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色能力服务。
 *
 * <p>角色的整个生命周期集中在这里：角色定义、用户-角色授权、角色-前端权限授权。
 * 不再另设 GrantService，避免同一角色能力散落在多个服务中。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RoleService {

    public static final String ROLE_ACTIVE = "ACTIVE";
    public static final String GRANT_ACTIVE = "ACTIVE";
    public static final String GRANT_REVOKED = "REVOKED";

    private static final int MAX_BATCH_PERMISSIONS = 200;

    private final IamRoleRepository roleRepository;
    private final IamUserRepository userRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamPermissionRepository permissionRepository;
    private final IamRolePermissionRepository rolePermissionRepository;
    private final DecisionService decisionService;
    private final LogService logService;

    @Transactional
    public IamRole createRole(String roleCode, String roleName, String roleType, String description) {
        RoleType.fromCode(roleType);
        if (roleRepository.findByRoleCode(roleCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "角色编码已存在: " + roleCode);
        }

        LocalDateTime now = LocalDateTime.now();
        IamRole role = new IamRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleType(roleType);
        role.setDescription(description);
        role.setStatus(ROLE_ACTIVE);
        role.setIsDeleted(0);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        return roleRepository.save(role);
    }

    public IamRole getRole(Long roleId) {
        return roleRepository.findById(roleId)
                .filter(this::isFunctionRole)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在: " + roleId));
    }

    public List<IamRole> listRoles() {
        return roleRepository.findAll().stream().filter(this::isFunctionRole).toList();
    }

    /**
     * 为用户授予角色。一个用户在同一时刻可以拥有多个角色。
     */
    @Transactional
    public IamUserRole grantRole(Long userId, Long roleId, LocalDateTime expireTime,
                                 Long grantBy, String grantReason) {
        IamUser user = userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在: " + userId));
        if (!AccountStatus.NORMAL.getCode().equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.TARGET_USER_ABNORMAL, "用户非正常状态，不能授权: " + userId);
        }

        IamRole role = getRole(roleId);
        if (!ROLE_ACTIVE.equals(role.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "角色已停用: " + roleId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (expireTime != null && !expireTime.isAfter(now)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "失效时间必须晚于当前时间");
        }

        IamUserRole grant = userRoleRepository.findByUserIdAndRoleId(userId, roleId).orElse(null);
        if (grant != null && GRANT_ACTIVE.equals(grant.getStatus())) {
            throw new BusinessException(ErrorCode.GRANT_DUPLICATE);
        }
        checkGrantBoundary(grantBy, roleId);
        Map<String, Object> before = grant == null ? null : grantSnapshot(grant);
        if (grant == null) {
            grant = new IamUserRole();
            grant.setUserId(userId);
            grant.setRoleId(roleId);
            grant.setCreatedAt(now);
        }
        grant.setEffectiveTime(now);
        grant.setExpireTime(expireTime);
        grant.setGrantBy(grantBy);
        grant.setGrantReason(grantReason);
        grant.setStatus(GRANT_ACTIVE);
        grant.setUpdatedAt(now);
        log.info("授予角色: userId={}, roleId={}", userId, roleId);
        IamUserRole saved = userRoleRepository.save(grant);
        recordPermissionChange(grantBy, "GRANT_ROLE", "user:" + userId + "/role:" + roleId,
                before, grantSnapshot(saved));
        return saved;
    }

    @Transactional
    public void grantRoles(Long userId, List<Long> roleIds, LocalDateTime expireTime,
                           Long grantBy, String grantReason) {
        for (Long roleId : roleIds) {
            grantRole(userId, roleId, expireTime, grantBy, grantReason);
        }
    }

    @Transactional
    public void revokeRole(Long userId, Long roleId, Long operatorId) {
        IamUserRole grant = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .filter(g -> GRANT_ACTIVE.equals(g.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户未被授予该角色"));
        revokeGrant(grant, operatorId);
    }

    @Transactional
    public void revokeAllRoles(Long userId) {
        revokeAllRoles(userId, null);
    }

    @Transactional
    public void revokeAllRoles(Long userId, Long operatorId) {
        userRoleRepository.findByUserIdAndStatus(userId, GRANT_ACTIVE)
                .forEach(grant -> revokeGrant(grant, operatorId));
    }

    public List<IamUserRole> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }

    public List<IamUserRole> getRoleUsers(Long roleId) {
        return userRoleRepository.findByRoleId(roleId);
    }

    /**
     * 为角色授予前端菜单/按钮权限。
     */
    @Transactional
    public void bindRolePermissions(Long roleId, List<Long> permissionIds,
                                    String allowedAction, Long createdBy) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "权限列表不能为空");
        }
        if (permissionIds.size() > MAX_BATCH_PERMISSIONS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "单次授权权限数量不能超过 " + MAX_BATCH_PERMISSIONS);
        }
        IamRole role = getRole(roleId);
        if (!ROLE_ACTIVE.equals(role.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "角色已停用: " + roleId);
        }
        OperationAction.fromCode(allowedAction);

        Set<Long> bound = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(IamRolePermission::getPermissionId)
                .collect(Collectors.toSet());
        Set<Long> before = new HashSet<>(bound);
        LocalDateTime now = LocalDateTime.now();
        for (Long permissionId : permissionIds) {
            IamPermission permission = permissionRepository.findById(permissionId)
                    .filter(p -> p.getIsDeleted() == 0)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND, "权限不存在: " + permissionId));
            if (!ROLE_ACTIVE.equals(permission.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "权限已停用: " + permissionId);
            }
            if (!bound.add(permissionId)) {
                continue;
            }
            IamRolePermission binding = new IamRolePermission();
            binding.setRoleId(roleId);
            binding.setPermissionId(permissionId);
            binding.setAllowedAction(allowedAction);
            binding.setCreatedAt(now);
            binding.setCreatedBy(createdBy);
            rolePermissionRepository.save(binding);
        }
        Set<Long> after = currentPermissionIds(roleId);
        if (!before.equals(after)) {
            recordPermissionChange(createdBy, "BIND_ROLE_PERMISSION", "role:" + roleId,
                    permissionSnapshot(roleId, before, allowedAction),
                    permissionSnapshot(roleId, after, allowedAction));
        }
    }

    @Transactional
    public void unbindRolePermissions(Long roleId, List<Long> permissionIds, Long operatorId) {
        Set<Long> before = currentPermissionIds(roleId);
        rolePermissionRepository.findByRoleId(roleId).stream()
                .filter(rp -> permissionIds.contains(rp.getPermissionId()))
                .forEach(rolePermissionRepository::delete);
        Set<Long> after = currentPermissionIds(roleId);
        if (!before.equals(after)) {
            recordPermissionChange(operatorId, "UNBIND_ROLE_PERMISSION", "role:" + roleId,
                    permissionSnapshot(roleId, before, null),
                    permissionSnapshot(roleId, after, null));
        }
    }

    public List<IamRolePermission> getRolePermissions(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }

    private void revokeGrant(IamUserRole grant, Long operatorId) {
        Map<String, Object> before = grantSnapshot(grant);
        grant.setStatus(GRANT_REVOKED);
        grant.setUpdatedAt(LocalDateTime.now());
        IamUserRole saved = userRoleRepository.save(grant);
        recordPermissionChange(operatorId, "REVOKE_ROLE",
                "user:" + saved.getUserId() + "/role:" + saved.getRoleId(),
                before, grantSnapshot(saved));
    }

    /**
     * 防提权：操作人只能授予权限集合不超出自身的角色。
     */
    private void checkGrantBoundary(Long operatorId, Long roleId) {
        if (operatorId == null) {
            return;
        }
        Set<Long> rolePermissions = collectRolePermissionIds(roleId);
        if (rolePermissions.isEmpty()) {
            return;
        }
        Set<Long> operatorPermissions =
                decisionService.getUserPermissions(operatorId, AuthService.ADMIN_SYSTEM_CODE);
        if (!operatorPermissions.containsAll(rolePermissions)) {
            throw new BusinessException(ErrorCode.NO_FUNCTION_PERM, "不能授予超出自身权限的角色: " + roleId);
        }
    }

    private Set<Long> collectRolePermissionIds(Long roleId) {
        Set<Long> roleIds = new HashSet<>();
        Set<Long> toVisit = new HashSet<>();
        toVisit.add(roleId);
        while (!toVisit.isEmpty()) {
            Set<Long> next = new HashSet<>();
            for (IamRole role : roleRepository.findAllById(toVisit)) {
                if (role.getIsDeleted() != 0 || !ROLE_ACTIVE.equals(role.getStatus())) {
                    continue;
                }
                if (!roleIds.add(role.getId())) {
                    continue;
                }
                if (role.getParentId() != null) {
                    next.add(role.getParentId());
                }
            }
            toVisit = next;
        }
        Set<Long> permissions = new HashSet<>();
        for (Long id : roleIds) {
            rolePermissionRepository.findByRoleId(id)
                    .forEach(rp -> permissions.add(rp.getPermissionId()));
        }
        return permissions;
    }

    private boolean isFunctionRole(IamRole role) {
        return role.getIsDeleted() == 0 && RoleType.FUNC.getCode().equals(role.getRoleType());
    }

    private Set<Long> currentPermissionIds(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(IamRolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> grantSnapshot(IamUserRole grant) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("grantId", grant.getId());
        snapshot.put("userId", grant.getUserId());
        snapshot.put("roleId", grant.getRoleId());
        snapshot.put("effectiveTime", grant.getEffectiveTime());
        snapshot.put("expireTime", grant.getExpireTime());
        snapshot.put("status", grant.getStatus());
        snapshot.put("grantReason", grant.getGrantReason());
        return snapshot;
    }

    private Map<String, Object> permissionSnapshot(Long roleId, Set<Long> permissionIds,
                                                   String allowedAction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("roleId", roleId);
        snapshot.put("permissionIds", permissionIds.stream().sorted().toList());
        if (allowedAction != null) {
            snapshot.put("allowedAction", allowedAction);
        }
        return snapshot;
    }

    private void recordPermissionChange(Long operatorId, String action, String target,
                                        Object before, Object after) {
        logService.recordPermissionChange(operatorId, AuthService.ADMIN_SYSTEM_CODE,
                action, target, before, after, null, RequestIdFilter.currentRequestId());
    }
}
