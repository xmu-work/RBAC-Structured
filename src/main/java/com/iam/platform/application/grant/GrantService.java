package com.iam.platform.application.grant;

import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权管理应用服务（M07）。
 *
 * <p>用户-角色绑定/解绑、角色-功能绑定、角色-数据范围绑定。
 * 授权前校验角色互斥、有效期、授权范围。
 * 对应 FR-17～FR-22、UC-04。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GrantService {

    public static final String GRANT_ACTIVE = "ACTIVE";
    public static final String GRANT_REVOKED = "REVOKED";

    private final IamUserRoleRepository userRoleRepository;
    private final IamUserRepository userRepository;
    private final IamRoleRepository roleRepository;
    private final RoleService roleService;

    /**
     * 为用户授予角色。
     *
     * <p>流程：校验用户存在且正常 → 校验角色存在且有效 → 校验有效期 → 校验重复 → 落库。
     * 同一用户同一角色只保留一行记录，回收后再次授予时复用原记录。
     * 角色互斥属于 RBAC2 约束，留待 RBAC3 阶段实现。
     * 对应 UC-04 授予角色。
     */
    @Transactional
    public IamUserRole grantRole(Long userId, Long roleId, LocalDateTime expireTime, Long grantBy, String grantReason) {
        IamUser user = userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在: " + userId));
        if (!AccountStatus.NORMAL.getCode().equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.TARGET_USER_ABNORMAL, "用户非正常状态，不能授权: " + userId);
        }

        IamRole role = roleRepository.findById(roleId)
                .filter(r -> r.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在: " + roleId));
        if (!RoleService.ROLE_ACTIVE.equals(role.getStatus())) {
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
        checkRoleMutex(userId, roleId);
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
        return userRoleRepository.save(grant);
    }

    /**
     * 批量授予角色，任一失败则整体回滚。
     */
    @Transactional
    public void grantRoles(Long userId, List<Long> roleIds, LocalDateTime expireTime, Long grantBy, String grantReason) {
        for (Long roleId : roleIds) {
            grantRole(userId, roleId, expireTime, grantBy, grantReason);
        }
    }

    /**
     * 撤销用户角色。
     */
    @Transactional
    public void revokeRole(Long userId, Long roleId) {
        IamUserRole grant = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .filter(g -> GRANT_ACTIVE.equals(g.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户未被授予该角色"));
        revoke(grant);
        log.info("撤销角色: userId={}, roleId={}", userId, roleId);
    }

    /**
     * 回收用户的全部有效角色（账号封存时调用）。
     */
    @Transactional
    public void revokeAllRoles(Long userId) {
        userRoleRepository.findByUserIdAndStatus(userId, GRANT_ACTIVE).forEach(this::revoke);
    }

    /**
     * 查询用户的角色列表（含已回收记录）。
     */
    public List<IamUserRole> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }

    private void revoke(IamUserRole grant) {
        grant.setStatus(GRANT_REVOKED);
        grant.setUpdatedAt(LocalDateTime.now());
        userRoleRepository.save(grant);
    }

    /**
     * 校验角色互斥：本次授予后，用户角色集合中不得出现互斥对（FR-14）。
     */
    private void checkRoleMutex(Long userId, Long roleId) {
        List<Long> candidate = userRoleRepository.findByUserIdAndStatus(userId, GRANT_ACTIVE).stream()
                .map(IamUserRole::getRoleId)
                .collect(Collectors.toCollection(ArrayList::new));
        candidate.add(roleId);
        long[] conflict = roleService.checkMutex(candidate);
        if (conflict != null) {
            throw new BusinessException(ErrorCode.ROLE_MUTEX_CONFLICT,
                    "角色互斥冲突: " + conflict[0] + " 与 " + conflict[1]);
        }
    }
}
