package com.iam.platform.application.decision;

import com.iam.platform.TestFixtures;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamRolePermissionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private IamUserRepository userRepository;
    @Mock
    private IamUserRoleRepository userRoleRepository;
    @Mock
    private IamRoleRepository roleRepository;
    @Mock
    private IamRolePermissionRepository rolePermissionRepository;
    @InjectMocks
    private DecisionService decisionService;

    private final CryptoComponent crypto = TestFixtures.crypto();
    private IamUserRole grant;
    private IamRole role;
    private IamUser user;

    @BeforeEach
    void setUp() {
        user = TestFixtures.user(1L, "NORMAL", crypto, "p");
        grant = TestFixtures.grant(1L, 2L, "ACTIVE");
        role = TestFixtures.role(2L, "ACTIVE");

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRoleRepository.findByUserIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(grant));
        lenient().when(roleRepository.findAllById(any())).thenReturn(List.of(role));
        lenient().when(rolePermissionRepository.findGrantedPermissions(any(), eq("ADMIN")))
                .thenReturn(List.of(granted(100L, "ALL", "ALL")));
        lenient().when(rolePermissionRepository.findGrantedPermissions(any(), eq("OA")))
                .thenReturn(List.of(granted(200L, "ALL", "ALL")));
    }

    private static IamRolePermissionRepository.GrantedPermissionView granted(
            Long permissionId, String allowed, String required) {
        return new IamRolePermissionRepository.GrantedPermissionView() {
            @Override
            public Long getPermissionId() {
                return permissionId;
            }

            @Override
            public String getAllowedAction() {
                return allowed;
            }

            @Override
            public String getRequiredAction() {
                return required;
            }
        };
    }

    @Test
    void returnsOnlyPermissionsOfRequestedSystem() {
        assertThat(decisionService.getUserPermissions(1L, "ADMIN")).containsExactly(100L);
        assertThat(decisionService.hasPermission(1L, "ADMIN", 100L)).isTrue();
        assertThat(decisionService.hasPermission(1L, "ADMIN", 200L)).isFalse();
        assertThat(decisionService.getUserPermissions(1L, "OA")).containsExactly(200L);
    }

    @Test
    void abnormalOrMissingUserHasNoPermissions() {
        user.setAccountStatus("DISABLED");
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(decisionService.getUserPermissions(1L, "ADMIN")).isEmpty();
        assertThat(decisionService.getUserPermissions(9L, "ADMIN")).isEmpty();
    }

    @Test
    void expiredGrantIsIgnored() {
        grant.setExpireTime(LocalDateTime.now().minusMinutes(1));
        assertThat(decisionService.getUserPermissions(1L, "ADMIN")).isEmpty();
    }

    @Test
    void disabledRoleGrantsNothing() {
        role.setStatus("DISABLED");
        assertThat(decisionService.getUserPermissions(1L, "ADMIN")).isEmpty();
    }

    @Test
    void allowedActionMustCoverRequiredAction() {
        when(rolePermissionRepository.findGrantedPermissions(any(), eq("ADMIN")))
                .thenReturn(List.of(granted(100L, "VIEW", "VIEW_EDIT")));
        assertThat(decisionService.getUserPermissions(1L, "ADMIN")).isEmpty();
    }

    @Test
    void batchCheckComputesEachPermission() {
        PermissionCheckResult result = decisionService.checkPermissions(1L, "ADMIN", List.of(100L, 200L));
        assertThat(result.getResults()).containsEntry(100L, true).containsEntry(200L, false);
    }
}
