package com.iam.platform.application.permission;

import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.enums.OperationAction;
import com.iam.platform.common.exception.BusinessException;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final IamSystemRepository systemRepository;
    private final IamModuleRepository moduleRepository;
    private final IamFunctionPointRepository functionPointRepository;
    private final IamRoleFunctionRepository roleFunctionRepository;
    private final IamRoleRepository roleRepository;

    /**
     * 注册接入子系统。
     */
    @Transactional
    public IamSystem registerSystem(String systemCode, String systemName, String description) {
        if (systemRepository.findBySystemCode(systemCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "系统编码已存在: " + systemCode);
        }

        LocalDateTime now = LocalDateTime.now();
        IamSystem system = new IamSystem();
        system.setSystemCode(systemCode);
        system.setSystemName(systemName);
        system.setDescription(description);
        system.setStatus(STATUS_ACTIVE);
        system.setIsDeleted(0);
        system.setCreatedAt(now);
        system.setUpdatedAt(now);
        return systemRepository.save(system);
    }

    /**
     * 注册模块。
     */
    @Transactional
    public IamModule registerModule(Long systemId, String moduleCode, String moduleName, Integer sortNo) {
        if (!systemRepository.existsById(systemId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "系统不存在: " + systemId);
        }

        LocalDateTime now = LocalDateTime.now();
        IamModule module = new IamModule();
        module.setSystemId(systemId);
        module.setModuleCode(moduleCode);
        module.setModuleName(moduleName);
        module.setSortNo(sortNo == null ? 0 : sortNo);
        module.setStatus(STATUS_ACTIVE);
        module.setIsDeleted(0);
        module.setCreatedAt(now);
        module.setUpdatedAt(now);
        return moduleRepository.save(module);
    }

    /**
     * 注册功能点。
     *
     * <p>流程：校验模块存在 → 校验操作取值 → 校验 API（路径+方法）唯一 → 落库。
     * apiPath 写 Controller 映射模板（如 /users/{userId}），鉴权拦截器按此匹配。
     */
    @Transactional
    public IamFunctionPoint registerFunctionPoint(Long moduleId,
                                                   String pointCode, String pointName,
                                                   String action, String apiPath, String apiMethod) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "模块不存在: " + moduleId);
        }
        OperationAction.fromCode(action);
        String method = apiMethod == null ? null : apiMethod.toUpperCase();
        if (apiPath != null && method != null
                && functionPointRepository.findByApiPathAndApiMethod(apiPath, method).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "API 已注册为功能点: " + method + " " + apiPath);
        }

        LocalDateTime now = LocalDateTime.now();
        IamFunctionPoint point = new IamFunctionPoint();
        point.setModuleId(moduleId);
        point.setPointCode(pointCode);
        point.setPointName(pointName);
        point.setAction(action);
        point.setApiPath(apiPath);
        point.setApiMethod(method);
        point.setStatus(STATUS_ACTIVE);
        point.setIsDeleted(0);
        point.setCreatedAt(now);
        point.setUpdatedAt(now);
        return functionPointRepository.save(point);
    }

    /**
     * 为角色绑定功能权限。
     *
     * <p>流程：校验角色存在 → 校验操作取值 → 逐个校验功能点存在 → 跳过已绑定项 → 落库。
     */
    @Transactional
    public void bindRoleFunctions(Long roleId, List<Long> functionPointIds, String allowedAction, Long createdBy) {
        roleRepository.findById(roleId)
                .filter(r -> r.getIsDeleted() == 0 && RoleService.ROLE_ACTIVE.equals(r.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在或已停用: " + roleId));
        OperationAction.fromCode(allowedAction);

        Set<Long> bound = roleFunctionRepository.findByRoleId(roleId).stream()
                .map(IamRoleFunction::getFunctionPointId)
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        for (Long pointId : functionPointIds) {
            if (!functionPointRepository.existsById(pointId)) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "功能点不存在: " + pointId);
            }
            if (!bound.add(pointId)) {
                continue;
            }
            IamRoleFunction rf = new IamRoleFunction();
            rf.setRoleId(roleId);
            rf.setFunctionPointId(pointId);
            rf.setAllowedAction(allowedAction);
            rf.setCreatedAt(now);
            rf.setCreatedBy(createdBy);
            roleFunctionRepository.save(rf);
        }
    }

    /**
     * 解除角色的功能权限绑定，未绑定的功能点忽略。
     */
    @Transactional
    public void unbindRoleFunctions(Long roleId, List<Long> functionPointIds) {
        roleFunctionRepository.findByRoleId(roleId).stream()
                .filter(rf -> functionPointIds.contains(rf.getFunctionPointId()))
                .forEach(roleFunctionRepository::delete);
    }

    /**
     * 查询角色已绑定的功能权限。
     */
    public List<IamRoleFunction> getRoleFunctions(Long roleId) {
        return roleFunctionRepository.findByRoleId(roleId);
    }

    /**
     * 按 API（映射模板 + HTTP 方法）查找已注册的功能点，供鉴权拦截器使用。
     */
    public Optional<IamFunctionPoint> findByApi(String apiPath, String apiMethod) {
        return functionPointRepository.findByApiPathAndApiMethod(apiPath, apiMethod);
    }

    /**
     * 查询全部子系统。
     */
    public List<IamSystem> listSystems() {
        return systemRepository.findAll().stream().filter(s -> s.getIsDeleted() == 0).toList();
    }
}
