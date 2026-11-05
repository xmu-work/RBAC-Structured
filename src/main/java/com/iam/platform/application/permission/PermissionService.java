package com.iam.platform.application.permission;

import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.enums.OperationAction;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import com.iam.platform.infrastructure.persistence.repository.IamApiRepository;
import com.iam.platform.infrastructure.persistence.repository.IamPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 前端菜单/按钮权限树能力。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Set<String> PERMISSION_TYPES = Set.of("MENU", "BUTTON");

    private final IamPermissionRepository permissionRepository;
    private final IamApiRepository apiRepository;
    private final SystemService systemService;

    @Transactional
    public IamPermission createPermission(Long systemId, Long parentId, String permissionCode,
                                          String permissionName, String permissionType,
                                          String routePath, String action, Integer sortNo) {
        systemService.getSystem(systemId);
        String type = permissionType == null ? "MENU" : permissionType.toUpperCase();
        if (!PERMISSION_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "权限类型必须为 MENU 或 BUTTON");
        }
        if (parentId != null) {
            IamPermission parent = getPermission(parentId);
            if (!systemId.equals(parent.getSystemId())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "父子权限必须属于同一系统");
            }
        }
        if (permissionRepository.findBySystemIdAndPermissionCode(systemId, permissionCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "权限编码已存在: " + permissionCode);
        }
        OperationAction.fromCode(action);

        LocalDateTime now = LocalDateTime.now();
        IamPermission permission = new IamPermission();
        permission.setSystemId(systemId);
        permission.setParentId(parentId);
        permission.setPermissionCode(permissionCode);
        permission.setPermissionName(permissionName);
        permission.setPermissionType(type);
        permission.setRoutePath(routePath);
        permission.setAction(action);
        permission.setSortNo(sortNo == null ? 0 : sortNo);
        permission.setStatus(STATUS_ACTIVE);
        permission.setIsDeleted(0);
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        return permissionRepository.save(permission);
    }

    public IamPermission getPermission(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .filter(p -> p.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "权限不存在: " + permissionId));
    }

    public List<IamPermission> listPermissions(Long systemId) {
        return permissionRepository.findBySystemIdAndIsDeleted(systemId, 0);
    }

    /**
     * 将一个后端 API 挂到前端权限。API 保留独立身份，一个权限可挂多个 API。
     */
    @Transactional
    public IamApi bindApi(Long permissionId, Long apiId) {
        IamPermission permission = getPermission(permissionId);
        IamApi api = apiRepository.findById(apiId)
                .filter(a -> a.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "API 不存在: " + apiId));
        if (!permission.getSystemId().equals(api.getSystemId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "权限与 API 必须属于同一系统");
        }
        api.setPermissionId(permissionId);
        api.setUpdatedAt(LocalDateTime.now());
        return apiRepository.save(api);
    }

    @Transactional
    public void unbindApi(Long permissionId, Long apiId) {
        IamApi api = apiRepository.findById(apiId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "API 不存在: " + apiId));
        if (permissionId.equals(api.getPermissionId())) {
            api.setPermissionId(null);
            api.setUpdatedAt(LocalDateTime.now());
            apiRepository.save(api);
        }
    }

    public List<IamApi> getPermissionApis(Long permissionId) {
        getPermission(permissionId);
        return apiRepository.findByPermissionId(permissionId);
    }
}
