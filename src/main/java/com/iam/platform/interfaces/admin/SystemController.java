package com.iam.platform.interfaces.admin;

import com.iam.platform.application.permission.DataPermissionService;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamDataScope;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.interfaces.AuthInterceptor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 子系统与功能权限管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/systems/*、/api/v1/data-scopes/*
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/systems")
@RequiredArgsConstructor
public class SystemController {

    /** 单次批量绑定功能点数量上限 */
    private static final int MAX_BATCH_FUNCTION_POINTS = 200;

    private final FunctionPermissionService functionPermissionService;
    private final DataPermissionService dataPermissionService;

    /**
     * 注册接入子系统。
     */
    @PostMapping
    public Result<IamSystem> register(@RequestParam @NotBlank @Size(max = 64) String systemCode,
                                      @RequestParam @NotBlank @Size(max = 128) String systemName,
                                      @RequestParam(required = false) @Size(max = 500) String description) {
        return Result.success(functionPermissionService.registerSystem(systemCode, systemName, description));
    }

    /**
     * 查询全部子系统。
     */
    @GetMapping
    public Result<List<IamSystem>> list() {
        return Result.success(functionPermissionService.listSystems());
    }

    /**
     * 注册模块。
     */
    @PostMapping("/modules")
    public Result<IamModule> registerModule(@RequestParam @NotNull Long systemId,
                                            @RequestParam @NotBlank @Size(max = 64) String moduleCode,
                                            @RequestParam @NotBlank @Size(max = 128) String moduleName,
                                            @RequestParam(required = false) Integer sortNo) {
        return Result.success(functionPermissionService.registerModule(systemId, moduleCode, moduleName, sortNo));
    }

    /**
     * 注册功能点。
     */
    @PostMapping("/function-points")
    public Result<IamFunctionPoint> registerFunctionPoint(@RequestParam @NotNull Long moduleId,
                                                          @RequestParam @NotBlank @Size(max = 64) String pointCode,
                                                          @RequestParam @NotBlank @Size(max = 128) String pointName,
                                                          @RequestParam @NotBlank String action,
                                                          @RequestParam(required = false) @Size(max = 255) String apiPath,
                                                          @RequestParam(required = false) @Size(max = 16) String apiMethod) {
        return Result.success(functionPermissionService.registerFunctionPoint(
                moduleId, pointCode, pointName, action, apiPath, apiMethod));
    }

    /**
     * 为角色绑定功能权限。
     */
    @PostMapping("/role-functions")
    public Result<Void> bindRoleFunctions(@RequestParam @NotNull Long roleId,
                                          @RequestBody @NotNull @Size(max = MAX_BATCH_FUNCTION_POINTS) List<Long> functionPointIds,
                                          @RequestParam @NotBlank String allowedAction,
                                          @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long createdBy) {
        functionPermissionService.bindRoleFunctions(roleId, functionPointIds, allowedAction, createdBy);
        return Result.success();
    }

    /**
     * 解除角色的功能权限绑定。
     */
    @DeleteMapping("/role-functions")
    public Result<Void> unbindRoleFunctions(@RequestParam @NotNull Long roleId,
                                            @RequestBody @NotNull @Size(max = MAX_BATCH_FUNCTION_POINTS) List<Long> functionPointIds) {
        functionPermissionService.unbindRoleFunctions(roleId, functionPointIds);
        return Result.success();
    }

    /**
     * 查询角色已绑定的功能权限。
     */
    @GetMapping("/role-functions/{roleId}")
    public Result<List<IamRoleFunction>> roleFunctions(@PathVariable Long roleId) {
        return Result.success(functionPermissionService.getRoleFunctions(roleId));
    }

    /**
     * 创建数据范围。
     */
    @PostMapping("/data-scopes")
    public Result<IamDataScope> createDataScope(@RequestParam @NotBlank @Size(max = 32) String scopeCode,
                                                @RequestParam @NotBlank @Size(max = 64) String scopeName,
                                                @RequestParam(required = false) @Size(max = 255) String description,
                                                @RequestParam(required = false) Integer sortNo) {
        return Result.success(dataPermissionService.createDataScope(scopeCode, scopeName, description, sortNo));
    }

    /**
     * 查询全部数据范围。
     */
    @GetMapping("/data-scopes")
    public Result<List<IamDataScope>> listDataScopes() {
        return Result.success(dataPermissionService.listDataScopes());
    }

    /**
     * 为角色绑定数据范围。
     */
    @PostMapping("/role-data-scopes")
    public Result<Void> bindRoleDataScope(@RequestParam @NotNull Long roleId,
                                          @RequestParam @NotBlank String scopeType,
                                          @RequestParam(required = false) Long orgId,
                                          @RequestParam(required = false) Long projectId,
                                          @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long createdBy) {
        dataPermissionService.bindRoleDataScope(roleId, scopeType, orgId, projectId, createdBy);
        return Result.success();
    }
}