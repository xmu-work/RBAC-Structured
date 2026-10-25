package com.iam.platform.application.account;

import com.iam.platform.TestFixtures;
import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.grant.GrantService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private IamUserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private GrantService grantService;

    private final CryptoComponent crypto = TestFixtures.crypto();
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(userRepository, crypto, authService, grantService);
        ReflectionTestUtils.setField(accountService, "defaultPassword", "Init@123");
    }

    private static int code(Throwable e) {
        return ((BusinessException) e).getCode();
    }

    @Test
    void createUserEncryptsSensitiveFieldsAndHashesPassword() {
        when(userRepository.findByUserNo("zhang")).thenReturn(Optional.empty());
        when(userRepository.save(any(IamUser.class))).thenAnswer(inv -> inv.getArgument(0));

        IamUser user = accountService.createUser("zhang", "Pwd@1", "张三", "13812345678", 1L, "工程师");

        assertThat(user.getAccountStatus()).isEqualTo("NORMAL");
        assertThat(user.getNameEnc()).isNotEqualTo("张三");
        assertThat(crypto.decrypt(user.getNameEnc())).isEqualTo("张三");
        assertThat(user.getPhoneHash()).isEqualTo(crypto.hash("13812345678"));
        assertThat(crypto.verifyPassword("Pwd@1", user.getPasswordHash())).isTrue();
    }

    @Test
    void createUserWithoutPasswordOrPhoneUsesDefaultPassword() {
        when(userRepository.findByUserNo("li")).thenReturn(Optional.empty());
        when(userRepository.save(any(IamUser.class))).thenAnswer(inv -> inv.getArgument(0));

        IamUser user = accountService.createUser("li", null, "李四", null, 1L, null);

        assertThat(crypto.verifyPassword("Init@123", user.getPasswordHash())).isTrue();
        assertThat(user.getPhoneEnc()).isNull();
    }

    @Test
    void createUserRejectsDuplicateUserNo() {
        when(userRepository.findByUserNo("zhang")).thenReturn(Optional.of(new IamUser()));

        assertThatThrownBy(() -> accountService.createUser("zhang", "p", "张三", null, 1L, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
        verify(userRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersFiltersByGivenConditionsAndExcludesDeleted() {
        Page<IamUser> page = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Example.class), any(PageRequest.class))).thenReturn(page);

        assertThat(accountService.listUsers(PageRequest.of(0, 20), 3L, "NORMAL")).isSameAs(page);

        verify(userRepository).findAll(argThat((Example<IamUser> ex) ->
                ex.getProbe().getOrgId() == 3L
                        && "NORMAL".equals(ex.getProbe().getAccountStatus())
                        && ex.getProbe().getIsDeleted() == 0), any(PageRequest.class));
    }

    @Test
    void getUserThrowsNotFoundForMissingOrDeleted() {
        IamUser deleted = TestFixtures.user(2L, "NORMAL", crypto, "p");
        deleted.setIsDeleted(1);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> accountService.getUser(1L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        assertThatThrownBy(() -> accountService.getUser(2L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void sealingRevokesTokensAndRoles() {
        IamUser user = TestFixtures.user(5L, "NORMAL", crypto, "p");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        accountService.changeStatus(5L, "SEALED", "离职");

        assertThat(user.getAccountStatus()).isEqualTo("SEALED");
        assertThat(user.getLeaveTime()).isNotNull();
        verify(authService).revokeUserTokens(5L);
        verify(grantService).revokeAllRoles(5L);
    }

    @Test
    void disablingRevokesTokensButKeepsRoles() {
        IamUser user = TestFixtures.user(5L, "NORMAL", crypto, "p");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        accountService.changeStatus(5L, "DISABLED", null);

        verify(authService).revokeUserTokens(5L);
        verifyNoInteractions(grantService);
    }

    @Test
    void enablingDoesNotRevokeAnything() {
        IamUser user = TestFixtures.user(5L, "DISABLED", crypto, "p");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        accountService.changeStatus(5L, "NORMAL", null);

        assertThat(user.getAccountStatus()).isEqualTo("NORMAL");
        verifyNoInteractions(authService, grantService);
    }

    @Test
    void changeStatusRejectsSealedUserAndUnknownStatus() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(TestFixtures.user(5L, "SEALED", crypto, "p")));

        assertThatThrownBy(() -> accountService.changeStatus(5L, "NORMAL", null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.USER_ALREADY_SEALED.getCode()));
        assertThatThrownBy(() -> accountService.changeStatus(5L, "WHATEVER", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toViewDecryptsNameAndMasksPhone() {
        IamUser user = TestFixtures.user(5L, "NORMAL", crypto, "p");
        user.setPhoneEnc(crypto.encrypt("13812345678"));

        UserView view = accountService.toView(user);

        assertThat(view.getRealName()).isEqualTo("张三");
        assertThat(view.getPhoneMasked()).isEqualTo("138****5678");
        assertThat(view.getUserNo()).isEqualTo("u5");
    }
}
