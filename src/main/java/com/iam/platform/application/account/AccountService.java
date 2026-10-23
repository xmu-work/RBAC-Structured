package com.iam.platform.application.account;

import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户与账号管理应用服务（M02）。
 *
 * <p>账号建立、状态管理（正常/停用/封存/锁定）、批量导入、账号-角色绑定查询。
 * 对应 FR-01～FR-07。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {

    private final IamUserRepository userRepository;
    private final CryptoComponent cryptoComponent;

    /**
     * 创建用户账号。
     *
     * <p>姓名、电话加密存储。
     */
    @Transactional
    public IamUser createUser(String userNo, String realName, String phone, Long orgId, String post) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询用户列表（分页）。
     */
    public Page<IamUser> listUsers(Pageable pageable, Long orgId, String accountStatus) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 查询用户详情。
     */
    public IamUser getUser(Long userId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 变更账号状态（停用/封存/锁定/启用）。
     *
     * <p>封存（离职）：保留账号、禁用登录、解绑角色与权限，不做物理删除。
     * 对应 FR-05、5.3 离职封存流程。
     */
    @Transactional
    public IamUser changeStatus(Long userId, String targetStatus, String reason) {
        throw new UnsupportedOperationException("待实现");
    }
}
