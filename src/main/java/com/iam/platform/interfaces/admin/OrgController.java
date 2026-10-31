package com.iam.platform.interfaces.admin;

import com.iam.platform.application.organization.OrganizationService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamOrg;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/orgs/*
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrganizationService organizationService;

    /**
     * 新建组织节点。
     */
    @PostMapping
    public Result<IamOrg> create(@RequestParam @NotBlank @Size(max = 64) String orgCode,
                                 @RequestParam @NotBlank @Size(max = 128) String orgName,
                                 @RequestParam @NotBlank String orgType,
                                 @RequestParam @NotNull @Min(1) @Max(4) Integer orgLevel,
                                 @RequestParam(required = false) Long parentId,
                                 @RequestParam(required = false) Integer sortNo) {
        return Result.success(organizationService.createOrg(orgCode, orgName, orgType, orgLevel, parentId, sortNo));
    }

    /**
     * 查询组织详情。
     */
    @GetMapping("/{orgId}")
    public Result<IamOrg> get(@PathVariable Long orgId) {
        return Result.success(organizationService.getOrg(orgId));
    }

    /**
     * 查询组织树。
     */
    @GetMapping
    public Result<List<IamOrg>> tree(@RequestParam(required = false) Long rootId) {
        return Result.success(organizationService.getOrgTree(rootId));
    }

    /**
     * 查询子节点。
     */
    @GetMapping("/{orgId}/children")
    public Result<List<IamOrg>> children(@PathVariable Long orgId) {
        return Result.success(organizationService.getChildren(orgId));
    }

    /**
     * 移动组织节点。
     */
    @PutMapping("/{orgId}/move")
    public Result<IamOrg> move(@PathVariable Long orgId, @RequestParam @NotNull Long newParentId) {
        return Result.success(organizationService.moveOrg(orgId, newParentId));
    }
}