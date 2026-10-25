package com.iam.platform.interfaces.admin;

import com.iam.platform.application.organization.OrganizationService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamOrg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/orgs/*
 */
@Slf4j
@RestController
@RequestMapping("/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrganizationService organizationService;

    /**
     * 新建组织节点。
     */
    @PostMapping
    public Result<IamOrg> create(@RequestParam String orgCode,
                                 @RequestParam String orgName,
                                 @RequestParam String orgType,
                                 @RequestParam Integer orgLevel,
                                 @RequestParam(required = false) Long parentId,
                                 @RequestParam(required = false) Integer sortNo) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询组织详情。
     */
    @GetMapping("/{orgId}")
    public Result<IamOrg> get(@PathVariable Long orgId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询组织树。
     */
    @GetMapping
    public Result<List<IamOrg>> tree(@RequestParam(required = false) Long rootId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询子节点。
     */
    @GetMapping("/{orgId}/children")
    public Result<List<IamOrg>> children(@PathVariable Long orgId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 移动组织节点。
     */
    @PutMapping("/{orgId}/move")
    public Result<IamOrg> move(@PathVariable Long orgId, @RequestParam Long newParentId) {
        throw new UnsupportedOperationException("待实现");
    }
}
