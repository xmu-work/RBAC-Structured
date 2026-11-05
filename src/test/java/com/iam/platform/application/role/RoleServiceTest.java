package com.iam.platform.application.role;

import com.iam.platform.TestFixtures;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.log.LogService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.entity.IamRolePermission;
import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import com.iam.platform.infrastructure.persistence.repository.IamPermissionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRolePermissionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private IamRoleRepository roleRepository;
    @Mock
    private IamUserRepository userRepository;
    @Mock
    private IamUserRoleRepository userRoleRepository;
    @Mock
    private IamPermissionRepository permissionRepository;
    @Mock
    private IamRolePermissionRepository rolePermissionRepository;
    @Mock
    private DecisionService decisionService;
    @Mock
    private LogService logService;
    @InjectMocks
    private RoleService roleService;

    private static int code(Throwable e) {
        return ((BusinessException) e).getCode();
    }

    @Test
    void createAndQueryRoles() {
        when(roleRepository.findByRoleCode("HR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(IamRole.class))).thenAnswer(inv -> inv.getArgument(0));

        IamRole created = roleService.createRole("HR", "人事", "FUNC", "人事专员");

        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getRoleType()).isEqualTo("FUNC");
    }

    @Test
    void grantRoleCreatesActiveAssignment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                TestFixtures.user(1L, "NORMAL", TestFixtures.crypto(), "p")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.role(2L, "ACTIVE")));
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.empty());
        when(userRoleRepository.save(any(IamUserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        IamUserRole grant = roleService.grantRole(1L, 2L, null, null, "入职");

        assertThat(grant.getStatus()).isEqualTo("ACTIVE");
        assertThat(grant.getRoleId()).isEqualTo(2L);
        assertThat(grant.getGrantReason()).isEqualTo("入职");
    }

    @Test
    void grantRejectsDuplicateAssignment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                TestFixtures.user(1L, "NORMAL", TestFixtures.crypto(), "p")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.role(2L, "ACTIVE")));
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L))
                .thenReturn(Optional.of(TestFixtures.grant(1L, 2L, "ACTIVE")));

        assertThatThrownBy(() -> roleService.grantRole(1L, 2L, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.GRANT_DUPLICATE.getCode()));
    }

    @Test
    void revokeMarksAssignmentRevoked() {
        IamUserRole grant = TestFixtures.grant(1L, 2L, "ACTIVE");
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.of(grant));
        when(userRoleRepository.save(grant)).thenReturn(grant);

        roleService.revokeRole(1L, 2L, 9L);

        assertThat(grant.getStatus()).isEqualTo("REVOKED");
        verify(userRoleRepository).save(grant);
    }

    @Test
    void bindPermissionsSkipsAlreadyBoundAndSavesNew() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.role(1L, "ACTIVE")));
        IamRolePermission existing = binding(1L, 10L);
        when(rolePermissionRepository.findByRoleId(1L)).thenReturn(List.of(existing));
        when(permissionRepository.findById(any())).thenAnswer(inv -> {
            IamPermission permission = new IamPermission();
            permission.setId(inv.getArgument(0));
            permission.setStatus("ACTIVE");
            permission.setIsDeleted(0);
            return Optional.of(permission);
        });

        roleService.bindRolePermissions(1L, List.of(10L, 11L, 11L), "VIEW", 9L);

        ArgumentCaptor<IamRolePermission> saved = ArgumentCaptor.forClass(IamRolePermission.class);
        verify(rolePermissionRepository).save(saved.capture());
        assertThat(saved.getValue().getPermissionId()).isEqualTo(11L);
        assertThat(saved.getValue().getAllowedAction()).isEqualTo("VIEW");
    }

    @Test
    void grantRejectsRoleBeyondOperatorPermissions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                TestFixtures.user(1L, "NORMAL", TestFixtures.crypto(), "p")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.role(2L, "ACTIVE")));
        when(userRoleRepository.findByUserIdAndRoleId(1L, 2L)).thenReturn(Optional.empty());
        when(roleRepository.findAllById(any())).thenReturn(List.of(TestFixtures.role(2L, "ACTIVE")));
        when(rolePermissionRepository.findByRoleId(2L)).thenReturn(List.of(binding(2L, 50L)));
        when(decisionService.getUserPermissions(9L, "ADMIN")).thenReturn(Set.of());

        assertThatThrownBy(() -> roleService.grantRole(1L, 2L, null, 9L, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.NO_FUNCTION_PERM.getCode()));
    }

    private static IamRolePermission binding(Long roleId, Long permissionId) {
        IamRolePermission binding = new IamRolePermission();
        binding.setRoleId(roleId);
        binding.setPermissionId(permissionId);
        return binding;
    }
}
