package com.iam.platform.interfaces.admin;

import com.iam.platform.application.account.AccountService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accountService;

    /**
     * 创建用户。
     */
    @PostMapping
    public Result<IamUser> create(@RequestParam String userNo,
                                  @RequestParam String realName,
                                  @RequestParam String phone,
                                  @RequestParam Long orgId,
                                  @RequestParam(required = false) String post) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{userId}")
    public Result<IamUser> get(@PathVariable Long userId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 分页查询用户列表。
     */
    @GetMapping
    public Result<Page<IamUser>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) Long orgId,
                                      @RequestParam(required = false) String accountStatus) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 变更账号状态。
     */
    @PutMapping("/{userId}/status")
    public Result<IamUser> changeStatus(@PathVariable Long userId,
                                        @RequestParam String targetStatus,
                                        @RequestParam(required = false) String reason) {
        throw new UnsupportedOperationException("待实现");
    }
}
