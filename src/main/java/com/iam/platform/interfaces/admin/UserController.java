package com.iam.platform.interfaces.admin;

import com.iam.platform.application.account.AccountService;
import com.iam.platform.application.account.UserView;
import com.iam.platform.common.result.PageResult;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户账号管理控制器（管理端）。
 *
 * <p>对应 API：/api/v1/users/*
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accountService;

    /**
     * 创建用户。
     */
    @PostMapping
    public Result<UserView> create(@RequestParam @NotBlank @Size(max = 32) String userNo,
                                   @RequestParam(required = false) @Size(max = 64) String password,
                                   @RequestParam @NotBlank @Size(max = 64) String realName,
                                   @RequestParam(required = false) @Size(max = 20) String phone,
                                   @RequestParam @NotNull Long orgId,
                                   @RequestParam(required = false) @Size(max = 64) String post) {
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
    public Result<PageResult<UserView>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                             @RequestParam(required = false) Long orgId,
                                             @RequestParam(required = false) @Size(max = 16) String accountStatus) {
        Page<IamUser> result = accountService.listUsers(PageRequest.of(page - 1, size), orgId, accountStatus);
        return Result.success(PageResult.of(result.getTotalElements(), page, size,
                result.getContent().stream().map(accountService::toView).toList()));
    }

    /**
     * 变更账号状态。
     */
    @PutMapping("/{userId}/status")
    public Result<UserView> changeStatus(@PathVariable Long userId,
                                         @RequestParam @NotBlank String targetStatus,
                                         @RequestParam(required = false) @Size(max = 255) String reason) {
        return Result.success(accountService.toView(accountService.changeStatus(userId, targetStatus, reason)));
    }
}