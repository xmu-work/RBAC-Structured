package com.iam.platform.application.role;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.enums.RoleType;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRoleExclusion;
import com.iam.platform.infrastructure.persistence.repository.IamRoleExclusionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理应用服务（M04）。
 *
 * <p>角色定义（功能角色/岗位型数据角色）、继承、互斥、复制、停用、预置模板。
 * 对应 FR-12～FR-16。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RoleService {

    public static final String ROLE_ACTIVE = "ACTIVE";

    private final IamRoleRepository roleRepository;
    private final IamRoleExclusionRepository exclusionRepository;

    /**
     * 新建角色。
     */
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

    /**
     * 查询角色详情。
     */
    public IamRole getRole(Long roleId) {
        return roleRepository.findById(roleId)
                .filter(r -> r.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在: " + roleId));
    }

    /**
     * 查询全部角色。
     */
    public List<IamRole> listRoles() {
        return roleRepository.findAll().stream().filter(r -> r.getIsDeleted() == 0).toList();
    }

    /**
     * 配置角色互斥。
     *
     * <p>互斥角色不能同时授予同一用户（如会计与出纳）。
     * 属于 RBAC2 约束，留待 RBAC3 阶段实现。
     * 对应 FR-14、UC-05。
     */
    @Transactional
    public void addRoleExclusion(Long roleIdA, Long roleIdB, String reason) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询角色的互斥角色列表。
     */
    public List<IamRoleExclusion> getExclusions(Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 校验角色互斥：检查给定角色集合中是否存在互斥对。
     *
     * @param roleIds 待校验的角色 ID 集合
     * @return 冲突的角色对，无冲突返回 null
     */
    public long[] checkMutex(List<Long> roleIds) {
        throw new UnsupportedOperationException("待实现");
    }

    private void saveExclusion(Long roleIdA, Long roleIdB, String reason) {
        throw new UnsupportedOperationException("待实现");
    }
}
