package com.iam.platform.interfaces.admin;

import com.iam.platform.application.permission.PermissionService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端菜单/按钮权限树控制器。
 */
@Validated
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public Result<IamPermission> create(
            @RequestParam @NotNull Long systemId,
            @RequestParam(required = false) Long parentId,
            @RequestParam @NotBlank @Size(max = 64) String permissionCode,
            @RequestParam @NotBlank @Size(max = 128) String permissionName,
            @RequestParam(defaultValue = "MENU") @NotBlank String permissionType,
            @RequestParam(required = false) @Size(max = 255) String routePath,
            @RequestParam @NotBlank String action,
            @RequestParam(required = false) Integer sortNo) {
        return Result.success(permissionService.createPermission(
                systemId, parentId, permissionCode, permissionName,
                permissionType, routePath, action, sortNo));
    }

    @GetMapping
    public Result<List<IamPermission>> list(@RequestParam @NotNull Long systemId) {
        return Result.success(permissionService.listPermissions(systemId));
    }

    @PostMapping("/{permissionId}/apis/{apiId}")
    public Result<IamApi> bindApi(@PathVariable Long permissionId, @PathVariable Long apiId) {
        return Result.success(permissionService.bindApi(permissionId, apiId));
    }

    @DeleteMapping("/{permissionId}/apis/{apiId}")
    public Result<Void> unbindApi(@PathVariable Long permissionId, @PathVariable Long apiId) {
        permissionService.unbindApi(permissionId, apiId);
        return Result.success();
    }

    @GetMapping("/{permissionId}/apis")
    public Result<List<IamApi>> permissionApis(@PathVariable Long permissionId) {
        return Result.success(permissionService.getPermissionApis(permissionId));
    }
}
