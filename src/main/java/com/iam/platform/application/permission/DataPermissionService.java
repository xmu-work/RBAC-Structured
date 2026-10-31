package com.iam.platform.application.permission;

import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.DataScopeType;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamDataScope;
import com.iam.platform.infrastructure.persistence.entity.IamRoleDataScope;
import com.iam.platform.infrastructure.persistence.repository.IamDataScopeRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleDataScopeRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据权限管理应用服务（M06）。
 *
 * <p>数据范围字典（全公司/分公司/本部门/本项目部/本人）、角色-数据范围绑定。
 * 对应 FR-20～FR-22。
 *
 * <p>生效的数据范围解析（用户 → 有效角色 → 数据范围）在 {@code DecisionService#resolveDataScopes}。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataPermissionService {

    private static final String SCOPE_ACTIVE = "ACTIVE";

    private final IamDataScopeRepository dataScopeRepository;
    private final IamRoleDataScopeRepository roleDataScopeRepository;
    private final IamRoleRepository roleRepository;

    /**
     * 创建数据范围定义（字典）。
     */
    @Transactional
    public IamDataScope createDataScope(String scopeCode, String scopeName,
                                        String description, Integer sortNo) {
        DataScopeType.fromCode(scopeCode);
        if (dataScopeRepository.findByScopeCode(scopeCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "数据范围编码已存在: " + scopeCode);
        }

        LocalDateTime now = LocalDateTime.now();
        IamDataScope scope = new IamDataScope();
        scope.setScopeCode(scopeCode);
        scope.setScopeName(scopeName);
        scope.setDescription(description);
        scope.setSortNo(sortNo == null ? 0 : sortNo);
        scope.setStatus(SCOPE_ACTIVE);
        scope.setCreatedAt(now);
        return dataScopeRepository.save(scope);
    }

    /**
     * 查询全部数据范围定义。
     */
    public List<IamDataScope> listDataScopes() {
        return dataScopeRepository.findAll();
    }

    /**
     * 为角色绑定数据范围。
     *
     * <p>流程：校验角色存在且有效 → 校验范围类型取值 → 落库。
     */
    @Transactional
    public void bindRoleDataScope(Long roleId, String scopeType, Long orgId, Long projectId, Long createdBy) {
        roleRepository.findById(roleId)
                .filter(r -> r.getIsDeleted() == 0 && RoleService.ROLE_ACTIVE.equals(r.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在或已停用: " + roleId));
        DataScopeType type = DataScopeType.fromCode(scopeType);

        LocalDateTime now = LocalDateTime.now();
        IamRoleDataScope binding = new IamRoleDataScope();
        binding.setRoleId(roleId);
        binding.setScopeType(type.getCode());
        binding.setOrgId(orgId);
        binding.setProjectId(projectId);
        binding.setCreatedAt(now);
        binding.setCreatedBy(createdBy);
        roleDataScopeRepository.save(binding);
    }

    /**
     * 查询角色的数据范围绑定。
     */
    public List<IamRoleDataScope> getRoleDataScopes(Long roleId) {
        return roleDataScopeRepository.findByRoleId(roleId);
    }
}