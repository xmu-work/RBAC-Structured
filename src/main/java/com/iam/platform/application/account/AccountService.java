package com.iam.platform.application.account;

import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.grant.GrantService;
import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
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
    private final AuthService authService;
    private final GrantService grantService;

    @Value("${iam.default-password:Iam@123456}")
    private String defaultPassword;

    /**
     * 创建用户账号。
     *
     * <p>流程：校验账号唯一 → 加密姓名/电话、哈希密码 → 落库。
     * 未指定密码时使用初始密码 iam.default-password。
     */
    @Transactional
    public IamUser createUser(String userNo, String password, String realName, String phone, Long orgId, String post) {
        if (userRepository.findByUserNo(userNo).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "账号已存在: " + userNo);
        }

        LocalDateTime now = LocalDateTime.now();
        IamUser user = new IamUser();
        user.setUserNo(userNo);
        user.setPasswordHash(cryptoComponent.hashPassword(password == null || password.isBlank() ? defaultPassword : password));
        user.setNameEnc(cryptoComponent.encrypt(realName));
        user.setNameHash(cryptoComponent.hash(realName));
        if (phone != null && !phone.isBlank()) {
            user.setPhoneEnc(cryptoComponent.encrypt(phone));
            user.setPhoneHash(cryptoComponent.hash(phone));
        }
        user.setOrgId(orgId);
        user.setPost(post);
        user.setAccountStatus(AccountStatus.NORMAL.getCode());
        user.setIsDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    /**
     * 查询用户列表（分页），orgId / accountStatus 为空时不作为条件。
     */
    public Page<IamUser> listUsers(Pageable pageable, Long orgId, String accountStatus) {
        IamUser probe = new IamUser();
        probe.setOrgId(orgId);
        probe.setAccountStatus(accountStatus);
        probe.setIsDeleted(0);
        return userRepository.findAll(Example.of(probe), pageable);
    }

    /**
     * 查询用户详情，不存在或已删除时抛出 RESOURCE_NOT_FOUND。
     */
    public IamUser getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在: " + userId));
    }

    /**
     * 变更账号状态（停用/封存/锁定/启用）。
     *
     * <p>流程：校验目标状态 → 更新状态 → 非正常状态吊销全部令牌 → 封存时回收全部角色。
     * 封存（离职）：保留账号、禁用登录、解绑角色与权限，不做物理删除。
     * 对应 FR-05、5.3 离职封存流程。
     */
    @Transactional
    public IamUser changeStatus(Long userId, String targetStatus, String reason) {
        AccountStatus target = AccountStatus.fromCode(targetStatus);
        IamUser user = getUser(userId);
        if (AccountStatus.SEALED.getCode().equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_SEALED);
        }

        LocalDateTime now = LocalDateTime.now();
        user.setAccountStatus(target.getCode());
        user.setUpdatedAt(now);
        if (target != AccountStatus.NORMAL) {
            authService.revokeUserTokens(userId);
        }
        if (target == AccountStatus.SEALED) {
            user.setLeaveTime(now);
            grantService.revokeAllRoles(userId);
        }
        log.info("账号状态变更: userId={}, status={}, reason={}", userId, target.getCode(), reason);
        return userRepository.save(user);
    }

    /**
     * 实体转安全视图：解密姓名、电话脱敏。
     */
    public UserView toView(IamUser user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUserNo(user.getUserNo());
        view.setRealName(cryptoComponent.decrypt(user.getNameEnc()));
        if (user.getPhoneEnc() != null) {
            view.setPhoneMasked(cryptoComponent.maskPhone(cryptoComponent.decrypt(user.getPhoneEnc())));
        }
        view.setOrgId(user.getOrgId());
        view.setPost(user.getPost());
        view.setAccountStatus(user.getAccountStatus());
        view.setLeaveTime(user.getLeaveTime());
        view.setLastLoginAt(user.getLastLoginAt());
        view.setIsDeleted(user.getIsDeleted());
        view.setCreatedAt(user.getCreatedAt());
        view.setUpdatedAt(user.getUpdatedAt());
        return view;
    }
}
