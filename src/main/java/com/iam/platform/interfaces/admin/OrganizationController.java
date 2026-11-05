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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分层组织部门资源控制器。
 */
@Validated
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public Result<IamOrg> create(@RequestParam @NotBlank @Size(max = 64) String orgCode,
                                 @RequestParam @NotBlank @Size(max = 128) String orgName,
                                 @RequestParam @NotBlank String orgType,
                                 @RequestParam @NotNull @Min(1) @Max(4) Integer orgLevel,
                                 @RequestParam(required = false) Long parentId,
                                 @RequestParam(required = false) Integer sortNo) {
        return Result.success(organizationService.createOrg(
                orgCode, orgName, orgType, orgLevel, parentId, sortNo));
    }

    @GetMapping("/{orgId}")
    public Result<IamOrg> get(@PathVariable Long orgId) {
        return Result.success(organizationService.getOrg(orgId));
    }

    @GetMapping
    public Result<List<IamOrg>> tree(@RequestParam(required = false) Long rootId) {
        return Result.success(organizationService.getOrgTree(rootId));
    }

    @GetMapping("/{orgId}/children")
    public Result<List<IamOrg>> children(@PathVariable Long orgId) {
        return Result.success(organizationService.getChildren(orgId));
    }

    @PutMapping("/{orgId}/parent")
    public Result<IamOrg> move(@PathVariable Long orgId, @RequestParam @NotNull Long newParentId) {
        return Result.success(organizationService.moveOrg(orgId, newParentId));
    }
}
