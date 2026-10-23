package com.iam.platform.application.permission;

import com.iam.platform.infrastructure.persistence.entity.IamDataScope;
import com.iam.platform.infrastructure.persistence.entity.IamRoleDataScope;
import com.iam.platform.infrastructure.persistence.repository.IamDataScopeRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleDataScopeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据权限管理应用服务（M06）。
 *
 * <p>数据范围定义（全公司/分公司/本部门/本项目部/自定义）、
 * 角色-数据范围绑定、数据范围计算。
 * 对应 FR-20～FR-22。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataPermissionService {

    private final IamDataScopeRepository dataScopeRepository;
    private final IamRoleDataScopeRepository roleDataScopeRepository;

    /**
     * 创建数据范围。
     */
    @Transactional
    public IamDataScope createDataScope(String scopeCode, String scopeName,
                                        String description, Integer sortNo) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 为角色绑定数据范围。
     */
    @Transactional
    public void bindRoleDataScope(Long roleId, String scopeType, Long orgId, Long projectId, Long createdBy) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询角色的数据范围。
     */
    public List<IamRoleDataScope> getRoleDataScopes(Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询全部数据范围定义。
     */
    public List<IamDataScope> listDataScopes() {
        throw new UnsupportedOperationException("待实现");
    }
}
