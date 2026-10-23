package com.iam.platform.interfaces.admin;

import com.iam.platform.application.permission.DataPermissionService;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamDataScope;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 子系统与功能权限管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/systems/*、/api/v1/data-scopes/*
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/systems")
@RequiredArgsConstructor
public class SystemController {

    private final FunctionPermissionService functionPermissionService;
    private final DataPermissionService dataPermissionService;

    /**
     * 注册接入子系统。
     */
    @PostMapping
    public Result<IamSystem> register(@RequestParam String systemCode,
                                      @RequestParam String systemName,
                                      @RequestParam(required = false) String description) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询全部子系统。
     */
    @GetMapping
    public Result<List<IamSystem>> list() {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 注册模块。
     */
    @PostMapping("/modules")
    public Result<IamModule> registerModule(@RequestParam Long systemId,
                                            @RequestParam String moduleCode,
                                            @RequestParam String moduleName,
                                            @RequestParam(required = false) Integer sortNo) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 注册功能点。
     */
    @PostMapping("/function-points")
    public Result<IamFunctionPoint> registerFunctionPoint(@RequestParam Long moduleId,
                                                          @RequestParam String pointCode,
                                                          @RequestParam String pointName,
                                                          @RequestParam String action,
                                                          @RequestParam(required = false) String apiPath,
                                                          @RequestParam(required = false) String apiMethod) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 为角色绑定功能权限。
     */
    @PostMapping("/role-functions")
    public Result<Void> bindRoleFunctions(@RequestParam Long roleId,
                                          @RequestBody List<Long> functionPointIds,
                                          @RequestParam String allowedAction,
                                          @RequestParam(required = false) Long createdBy) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 创建数据范围。
     */
    @PostMapping("/data-scopes")
    public Result<IamDataScope> createDataScope(@RequestParam String scopeCode,
                                                @RequestParam String scopeName,
                                                @RequestParam(required = false) String description,
                                                @RequestParam(required = false) Integer sortNo) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询全部数据范围。
     */
    @GetMapping("/data-scopes")
    public Result<List<IamDataScope>> listDataScopes() {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 为角色绑定数据范围。
     */
    @PostMapping("/role-data-scopes")
    public Result<Void> bindRoleDataScope(@RequestParam Long roleId,
                                          @RequestParam String scopeType,
                                          @RequestParam(required = false) Long orgId,
                                          @RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) Long createdBy) {
        throw new UnsupportedOperationException("待实现");
    }
}
