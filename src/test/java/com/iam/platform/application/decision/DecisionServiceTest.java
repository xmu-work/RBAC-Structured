package com.iam.platform.application.decision;

import com.iam.platform.TestFixtures;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.*;
import com.iam.platform.infrastructure.persistence.repository.*;
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

/**
 * 场景：用户 1 拥有角色 2；角色 2 在 ADMIN 系统绑定功能点 100，在 OA 系统绑定功能点 200。
 * 角色→功能点候选由 {@code findGrantedPoints} 单条 join 查询返回（已含系统/模块/功能点启用过滤）。
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private IamUserRepository userRepository;
    @Mock
    private IamUserRoleRepository userRoleRepository;
    @Mock
    private IamRoleRepository roleRepository;
    @Mock
    private IamRoleFunctionRepository roleFunctionRepository;
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
        lenient().when(roleFunctionRepository.findGrantedPoints(any(), eq("ADMIN")))
                .thenReturn(List.of(granted(100L, "ALL", "ALL")));
        lenient().when(roleFunctionRepository.findGrantedPoints(any(), eq("OA")))
                .thenReturn(List.of(granted(200L, "ALL", "ALL")));
    }

    private static IamRoleFunctionRepository.GrantedPointView granted(Long id, String allowed, String required) {
        return new IamRoleFunctionRepository.GrantedPointView() {
            @Override
            public Long getFunctionPointId() {
                return id;
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
    void returnsOnlyFunctionPointsOfRequestedSystem() {
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).containsExactly(100L);
        assertThat(decisionService.hasPermission(1L, "ADMIN", 100L)).isTrue();
        assertThat(decisionService.hasPermission(1L, "ADMIN", 200L)).isFalse();
        assertThat(decisionService.getUserFunctionPoints(1L, "OA")).containsExactly(200L);
    }

    @Test
    void unknownSystemHasNoPermissions() {
        when(roleFunctionRepository.findGrantedPoints(any(), eq("NONE"))).thenReturn(List.of());

        assertThat(decisionService.getUserFunctionPoints(1L, "NONE")).isEmpty();
    }

    @Test
    void abnormalOrMissingUserHasNoPermissions() {
        user.setAccountStatus("DISABLED");
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
        assertThat(decisionService.getUserFunctionPoints(9L, "ADMIN")).isEmpty();
    }

    @Test
    void expiredOrNotYetEffectiveGrantIsIgnored() {
        grant.setExpireTime(LocalDateTime.now().minusMinutes(1));
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();

        grant.setExpireTime(null);
        grant.setEffectiveTime(LocalDateTime.now().plusDays(1));
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void disabledRoleGrantsNothing() {
        role.setStatus("DISABLED");
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void nonFunctionRoleGrantsNothing() {
        role.setRoleType("DATA");
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void disabledPointIsFilteredByQuery() {
        // 功能点停用由 join 查询（SQL）过滤，查询不再返回候选
        when(roleFunctionRepository.findGrantedPoints(any(), eq("ADMIN"))).thenReturn(List.of());
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void allowedActionMustCoverRequiredAction() {
        // 角色仅授予 VIEW，功能点要求“查看/编辑” → 不通过
        when(roleFunctionRepository.findGrantedPoints(any(), eq("ADMIN")))
                .thenReturn(List.of(granted(100L, "VIEW", "VIEW_EDIT")));
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void batchCheckComputesEachPoint() {
        PermissionCheckResult result = decisionService.checkPermissions(1L, "ADMIN", List.of(100L, 200L));

        assertThat(result.getResults()).containsEntry(100L, true).containsEntry(200L, false);
    }

}
