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
import static org.mockito.Mockito.*;

/**
 * 场景：用户 1 拥有角色 2，角色 2 绑定功能点 100（属于 ADMIN 系统）和 200（属于 OA 系统）。
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
    @Mock
    private IamFunctionPointRepository functionPointRepository;
    @Mock
    private IamModuleRepository moduleRepository;
    @Mock
    private IamSystemRepository systemRepository;
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

        IamRoleFunction rf1 = new IamRoleFunction();
        rf1.setRoleId(2L);
        rf1.setFunctionPointId(100L);
        IamRoleFunction rf2 = new IamRoleFunction();
        rf2.setRoleId(2L);
        rf2.setFunctionPointId(200L);
        IamSystem admin = new IamSystem();
        admin.setId(1L);
        IamModule module = new IamModule();
        module.setId(10L);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRoleRepository.findByUserIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(grant));
        lenient().when(roleRepository.findAllById(any())).thenReturn(List.of(role));
        lenient().when(roleFunctionRepository.findByRoleIdIn(Set.of(2L))).thenReturn(List.of(rf1, rf2));
        lenient().when(systemRepository.findBySystemCode("ADMIN")).thenReturn(Optional.of(admin));
        lenient().when(moduleRepository.findBySystemId(1L)).thenReturn(List.of(module));
        lenient().when(functionPointRepository.findAllById(any())).thenReturn(List.of(point(100L, 10L), point(200L, 20L)));
    }

    private static IamFunctionPoint point(Long id, Long moduleId) {
        IamFunctionPoint fp = new IamFunctionPoint();
        fp.setId(id);
        fp.setModuleId(moduleId);
        fp.setStatus("ACTIVE");
        fp.setIsDeleted(0);
        return fp;
    }

    @Test
    void returnsOnlyFunctionPointsOfRequestedSystem() {
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).containsExactly(100L);
        assertThat(decisionService.hasPermission(1L, "ADMIN", 100L)).isTrue();
        assertThat(decisionService.hasPermission(1L, "ADMIN", 200L)).isFalse();
    }

    @Test
    void unknownSystemHasNoPermissions() {
        when(systemRepository.findBySystemCode("NONE")).thenReturn(Optional.empty());

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
        when(roleRepository.findAllById(Set.of())).thenReturn(List.of());
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();

        grant.setExpireTime(null);
        grant.setEffectiveTime(LocalDateTime.now().plusDays(1));
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void disabledRoleOrDisabledPointGrantsNothing() {
        role.setStatus("DISABLED");
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();

        role.setStatus("ACTIVE");
        IamFunctionPoint disabled = point(100L, 10L);
        disabled.setStatus("DISABLED");
        when(functionPointRepository.findAllById(any())).thenReturn(List.of(disabled));
        assertThat(decisionService.getUserFunctionPoints(1L, "ADMIN")).isEmpty();
    }

    @Test
    void batchCheckComputesEachPoint() {
        PermissionCheckResult result = decisionService.checkPermissions(1L, "ADMIN", List.of(100L, 200L));

        assertThat(result.getResults()).containsEntry(100L, true).containsEntry(200L, false);
    }
}
