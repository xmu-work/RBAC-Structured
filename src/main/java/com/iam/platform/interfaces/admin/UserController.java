package com.iam.platform.interfaces.admin;

import com.iam.platform.application.account.AccountService;
import com.iam.platform.application.account.UserView;
import com.iam.platform.common.result.PageResult;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 用户账号管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/users/*
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accountService;

    /**
     * 创建用户。
     */
    @PostMapping
    public Result<UserView> create(@RequestParam String userNo,
                                   @RequestParam(required = false) String password,
                                   @RequestParam String realName,
                                   @RequestParam(required = false) String phone,
                                   @RequestParam Long orgId,
                                   @RequestParam(required = false) String post) {
        return Result.success(accountService.toView(
                accountService.createUser(userNo, password, realName, phone, orgId, post)));
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{userId}")
    public Result<UserView> get(@PathVariable Long userId) {
        return Result.success(accountService.toView(accountService.getUser(userId)));
    }

    /**
     * 分页查询用户列表。
     */
    @GetMapping
    public Result<PageResult<UserView>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) Long orgId,
                                             @RequestParam(required = false) String accountStatus) {
        Page<IamUser> result = accountService.listUsers(PageRequest.of(Math.max(page, 1) - 1, size), orgId, accountStatus);
        return Result.success(PageResult.of(result.getTotalElements(), page, size,
                result.getContent().stream().map(accountService::toView).toList()));
    }

    /**
     * 变更账号状态。
     */
    @PutMapping("/{userId}/status")
    public Result<UserView> changeStatus(@PathVariable Long userId,
                                         @RequestParam String targetStatus,
                                         @RequestParam(required = false) String reason) {
        return Result.success(accountService.toView(accountService.changeStatus(userId, targetStatus, reason)));
    }
}
