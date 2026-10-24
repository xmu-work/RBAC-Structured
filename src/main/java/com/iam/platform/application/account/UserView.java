package com.iam.platform.application.account;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户安全视图 DTO。
 *
 * <p>对外只返回解密后的姓名与脱敏电话，不返回密码哈希、密文与检索哈希。
 */
@Data
public class UserView {

    private Long id;
    private String userNo;
    private String realName;
    private String phoneMasked;
    private Long orgId;
    private String post;
    private String accountStatus;
    private LocalDateTime leaveTime;
    private LocalDateTime lastLoginAt;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
