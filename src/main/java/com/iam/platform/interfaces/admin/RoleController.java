package com.iam.platform.interfaces.admin;

import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRolePermission;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.interfaces.AuthInterceptor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色资源控制器。
 *
 * <p>所有 URL 都以 {@code /roles} 为根；角色生命周期、用户角色分配和角色权限分配
 * 都能从 URL 最终资源直接定位到本控制器。
 */
@Validated
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public Result<IamRole> create(@RequestParam @NotBlank @Size(max = 64) String roleCode,
                                  @RequestParam @NotBlank @Size(max = 128) String roleName,
                                  @RequestParam(defaultValue = "FUNC") @NotBlank String roleType,
                                  @RequestParam(required = false) @Size(max = 500) String description) {
        return Result.success(roleService.createRole(roleCode, roleName, roleType, description));
    }

    @GetMapping("/{roleId}")
    public Result<IamRole> get(@PathVariable Long roleId) {
        return Result.success(roleService.getRole(roleId));
    }

    @GetMapping
    public Result<List<IamRole>> list() {
        return Result.success(roleService.listRoles());
    }

    /**
     * 将角色分配给用户。
     */
    @PostMapping("/{roleId}/assignments")
    public Result<IamUserRole> assignUser(@PathVariable Long roleId,
                                          @RequestParam @NotNull Long userId,
                                          @RequestParam(required = false) LocalDateTime expireTime,
                                          @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long grantBy,
                                          @RequestParam(required = false) @Size(max = 255) String grantReason) {
        return Result.success(roleService.grantRole(userId, roleId, expireTime, grantBy, grantReason));
    }

    @DeleteMapping("/{roleId}/assignments/{userId}")
    public Result<Void> revokeUser(@PathVariable Long roleId, @PathVariable Long userId,
                                   @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long operatorId) {
        roleService.revokeRole(userId, roleId, operatorId);
        return Result.success();
    }

    @GetMapping("/{roleId}/assignments")
    public Result<List<IamUserRole>> roleAssignments(@PathVariable Long roleId) {
        return Result.success(roleService.getRoleUsers(roleId));
    }

    @GetMapping("/assignments")
    public Result<List<IamUserRole>> userAssignments(@RequestParam @NotNull Long userId) {
        return Result.success(roleService.getUserRoles(userId));
    }

    /**
     * 给角色授予前端菜单/按钮权限。
     */
    @PostMapping("/{roleId}/permissions")
    public Result<Void> bindPermissions(
            @PathVariable Long roleId,
            @RequestBody @NotNull @Size(max = 200) List<Long> permissionIds,
            @RequestParam @NotBlank String allowedAction,
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long createdBy) {
        roleService.bindRolePermissions(roleId, permissionIds, allowedAction, createdBy);
        return Result.success();
    }

    @DeleteMapping("/{roleId}/permissions")
    public Result<Void> unbindPermissions(
            @PathVariable Long roleId,
            @RequestBody @NotNull @Size(max = 200) List<Long> permissionIds,
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long operatorId) {
        roleService.unbindRolePermissions(roleId, permissionIds, operatorId);
        return Result.success();
    }

    @GetMapping("/{roleId}/permissions")
    public Result<List<IamRolePermission>> rolePermissions(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissions(roleId));
    }
}
