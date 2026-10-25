package com.iam.platform.application.grant;

import com.iam.platform.TestFixtures;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrantServiceTest {

    @Mock
    private IamUserRoleRepository userRoleRepository;
    @Mock
    private IamUserRepository userRepository;
    @Mock
    private IamRoleRepository roleRepository;
    @InjectMocks
    private GrantService grantService;

    private final CryptoComponent crypto = TestFixtures.crypto();

    private static int code(Throwable e) {
        return ((BusinessException) e).getCode();
    }

    private void givenUserAndRole(String userStatus, String roleStatus) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user(1L, userStatus, crypto, "p")));
        lenient().when(roleRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.role(2L, roleStatus)));
    }

    @Test
    void grantCreatesActiveBinding() {
        givenUserAndRole("NORMAL", "ACTIVE");
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.empty());
        when(userRoleRepository.save(any(IamUserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        IamUserRole grant = grantService.grantRole(1L, 2L, null, 9L, "入职");

        assertThat(grant.getStatus()).isEqualTo("ACTIVE");
        assertThat(grant.getUserId()).isEqualTo(1L);
        assertThat(grant.getRoleId()).isEqualTo(2L);
        assertThat(grant.getGrantBy()).isEqualTo(9L);
        assertThat(grant.getEffectiveTime()).isNotNull();
    }

    @Test
    void regrantAfterRevokeReusesExistingRow() {
        givenUserAndRole("NORMAL", "ACTIVE");
        IamUserRole revoked = TestFixtures.grant(1L, 2L, "REVOKED");
        revoked.setId(30L);
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.of(revoked));
        when(userRoleRepository.save(revoked)).thenReturn(revoked);

        IamUserRole grant = grantService.grantRole(1L, 2L, null, null, null);

        assertThat(grant.getId()).isEqualTo(30L);
        assertThat(grant.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void grantRejectsDuplicateActiveBinding() {
        givenUserAndRole("NORMAL", "ACTIVE");
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.of(TestFixtures.grant(1L, 2L, "ACTIVE")));

        assertThatThrownBy(() -> grantService.grantRole(1L, 2L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.GRANT_DUPLICATE.getCode()));
    }

    @Test
    void grantRejectsAbnormalUserOrMissingUser() {
        givenUserAndRole("DISABLED", "ACTIVE");
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grantService.grantRole(1L, 2L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode()));
        assertThatThrownBy(() -> grantService.grantRole(3L, 2L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void grantRejectsMissingOrDisabledRole() {
        givenUserAndRole("NORMAL", "DISABLED");
        when(roleRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grantService.grantRole(1L, 2L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
        assertThatThrownBy(() -> grantService.grantRole(1L, 4L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void grantRejectsExpireTimeInThePast() {
        givenUserAndRole("NORMAL", "ACTIVE");

        assertThatThrownBy(() -> grantService.grantRole(1L, 2L, LocalDateTime.now().minusDays(1), null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void grantRolesGrantsEachRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user(1L, "NORMAL", crypto, "p")));
        when(roleRepository.findById(any())).thenAnswer(inv -> Optional.of(TestFixtures.role(inv.getArgument(0), "ACTIVE")));
        when(userRoleRepository.findByUserIdAndRoleId(eq(1L), any())).thenReturn(Optional.empty());

        grantService.grantRoles(1L, List.of(2L, 3L), null, null, null);

        verify(userRoleRepository, times(2)).save(any(IamUserRole.class));
    }

    @Test
    void revokeMarksActiveBindingRevoked() {
        IamUserRole active = TestFixtures.grant(1L, 2L, "ACTIVE");
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.of(active));

        grantService.revokeRole(1L, 2L);

        assertThat(active.getStatus()).isEqualTo("REVOKED");
        verify(userRoleRepository).save(active);
    }

    @Test
    void revokeRejectsMissingOrAlreadyRevokedBinding() {
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.of(TestFixtures.grant(1L, 2L, "REVOKED")));

        assertThatThrownBy(() -> grantService.revokeRole(1L, 2L))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void revokeAllRolesRevokesEveryActiveBinding() {
        IamUserRole a = TestFixtures.grant(1L, 2L, "ACTIVE");
        IamUserRole b = TestFixtures.grant(1L, 3L, "ACTIVE");
        when(userRoleRepository.findByUserIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(a, b));

        grantService.revokeAllRoles(1L);

        assertThat(List.of(a, b)).allSatisfy(g -> assertThat(g.getStatus()).isEqualTo("REVOKED"));
    }

    @Test
    void getUserRolesReturnsAllBindings() {
        List<IamUserRole> grants = List.of(TestFixtures.grant(1L, 2L, "ACTIVE"));
        when(userRoleRepository.findByUserId(1L)).thenReturn(grants);

        assertThat(grantService.getUserRoles(1L)).isSameAs(grants);
    }
}
