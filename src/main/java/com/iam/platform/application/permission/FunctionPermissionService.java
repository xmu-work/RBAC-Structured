package com.iam.platform.application.permission;

import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 功能权限管理应用服务（M05）。
 *
 * <p>子系统/模块/功能点注册、角色-功能绑定。
 * 对应 FR-12～FR-13、FR-17～FR-19。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FunctionPermissionService {

    private final IamSystemRepository systemRepository;
    private final IamModuleRepository moduleRepository;
    private final IamFunctionPointRepository functionPointRepository;
    private final IamRoleFunctionRepository roleFunctionRepository;

    /**
     * 注册接入子系统。
     */
    @Transactional
    public IamSystem registerSystem(String systemCode, String systemName, String description) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 注册模块。
     */
    @Transactional
    public IamModule registerModule(Long systemId, String moduleCode, String moduleName, Integer sortNo) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 注册功能点。
     */
    @Transactional
    public IamFunctionPoint registerFunctionPoint(Long moduleId,
                                                   String pointCode, String pointName,
                                                   String action, String apiPath, String apiMethod) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 为角色绑定功能权限。
     */
    @Transactional
    public void bindRoleFunctions(Long roleId, List<Long> functionPointIds, String allowedAction, Long createdBy) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询角色已绑定的功能权限。
     */
    public List<IamRoleFunction> getRoleFunctions(Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询全部子系统。
     */
    public List<IamSystem> listSystems() {
        throw new UnsupportedOperationException("待实现");
    }
}
