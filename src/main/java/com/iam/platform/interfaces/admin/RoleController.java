package com.iam.platform.interfaces.admin;

import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRoleExclusion;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.interfaces.AuthInterceptor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色与授权管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/roles/*、/api/v1/grants/*
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final GrantService grantService;

    /**
     * 新建角色。
     */
    @PostMapping
    public Result<IamRole> create(@RequestParam @NotBlank @Size(max = 64) String roleCode,
                                  @RequestParam @NotBlank @Size(max = 128) String roleName,
                                  @RequestParam @NotBlank String roleType,
                                  @RequestParam(required = false) @Size(max = 500) String description) {
        return Result.success(roleService.createRole(roleCode, roleName, roleType, description));
    }

    /**
     * 查询角色详情。
     */
    @GetMapping("/{roleId}")
    public Result<IamRole> get(@PathVariable Long roleId) {
        return Result.success(roleService.getRole(roleId));
    }

    /**
     * 查询全部角色。
     */
    @GetMapping
    public Result<List<IamRole>> list() {
        return Result.success(roleService.listRoles());
    }

    /**
     * 配置角色互斥。
     */
    @PostMapping("/{roleId}/exclusions")
    public Result<Void> addExclusion(@PathVariable Long roleId,
                                     @RequestParam @NotNull Long excludedRoleId,
                                     @RequestParam(required = false) @Size(max = 255) String reason) {
        roleService.addRoleExclusion(roleId, excludedRoleId, reason);
        return Result.success();
    }

    /**
     * 查询角色互斥列表。
     */
    @GetMapping("/{roleId}/exclusions")
    public Result<List<IamRoleExclusion>> exclusions(@PathVariable Long roleId) {
        return Result.success(roleService.getExclusions(roleId));
    }

    /**
     * 为用户授予角色。
     */
    @PostMapping("/grants")
    public Result<IamUserRole> grant(@RequestParam Long userId,
                                     @RequestParam Long roleId,
                                     @RequestParam(required = false) LocalDateTime expireTime,
                                     @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long grantBy,
                                     @RequestParam(required = false) @Size(max = 255) String grantReason) {
        return Result.success(grantService.grantRole(userId, roleId, expireTime, grantBy, grantReason));
    }

    /**
     * 撤销用户角色。
     */
    @DeleteMapping("/grants")
    public Result<Void> revoke(@RequestParam Long userId, @RequestParam Long roleId) {
        grantService.revokeRole(userId, roleId);
        return Result.success();
    }

    /**
     * 查询用户角色列表。
     */
    @GetMapping("/grants/{userId}")
    public Result<List<IamUserRole>> userRoles(@PathVariable Long userId) {
        return Result.success(grantService.getUserRoles(userId));
    }
}