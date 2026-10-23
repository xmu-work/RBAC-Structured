package com.iam.platform.interfaces.admin;

import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRoleExclusion;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色与授权管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/roles/*、/api/v1/grants/*
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final GrantService grantService;

    /**
     * 新建角色。
     */
    @PostMapping
    public Result<IamRole> create(@RequestParam String roleCode,
                                  @RequestParam String roleName,
                                  @RequestParam String roleType,
                                  @RequestParam(required = false) String description) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询角色详情。
     */
    @GetMapping("/{roleId}")
    public Result<IamRole> get(@PathVariable Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询全部角色。
     */
    @GetMapping
    public Result<List<IamRole>> list() {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 配置角色互斥。
     */
    @PostMapping("/{roleId}/exclusions")
    public Result<Void> addExclusion(@PathVariable Long roleId,
                                     @RequestParam Long excludedRoleId,
                                     @RequestParam(required = false) String reason) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询角色互斥列表。
     */
    @GetMapping("/{roleId}/exclusions")
    public Result<List<IamRoleExclusion>> exclusions(@PathVariable Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 为用户授予角色。
     */
    @PostMapping("/grants")
    public Result<IamUserRole> grant(@RequestParam Long userId,
                                     @RequestParam Long roleId,
                                     @RequestParam(required = false) LocalDateTime expireTime,
                                     @RequestParam(required = false) Long grantBy,
                                     @RequestParam(required = false) String grantReason) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 撤销用户角色。
     */
    @DeleteMapping("/grants")
    public Result<Void> revoke(@RequestParam Long userId, @RequestParam Long roleId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询用户角色列表。
     */
    @GetMapping("/grants/{userId}")
    public Result<List<IamUserRole>> userRoles(@PathVariable Long userId) {
        throw new UnsupportedOperationException("待实现");
    }
}
