package com.iam.platform.application.grant;

import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    private final IamUserRoleRepository userRoleRepository;
    private final IamUserRepository userRepository;
    private final IamRoleRepository roleRepository;
    private final RoleService roleService;

    /**
     * 为用户授予角色。
     *
     * <p>流程：校验用户存在 → 校验角色存在 → 校验互斥 → 落库。
     * 对应 UC-04 授予角色。
     */
    @Transactional
    public IamUserRole grantRole(Long userId, Long roleId, LocalDateTime expireTime, Long grantBy, String grantReason) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 批量授予角色。
     */
    @Transactional
    public void grantRoles(Long userId, List<Long> roleIds, LocalDateTime expireTime, Long grantBy, String grantReason) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 撤销用户角色。
     */
    @Transactional
    public void revokeRole(Long userId, Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询用户的角色列表。
     */
    public List<IamUserRole> getUserRoles(Long userId) {
        throw new UnsupportedOperationException("待实现");
    }
}
