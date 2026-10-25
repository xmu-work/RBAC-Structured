package com.iam.platform.application.role;

import com.iam.platform.TestFixtures;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamRole;
import com.iam.platform.infrastructure.persistence.repository.IamRoleExclusionRepository;
import com.iam.platform.infrastructure.persistence.repository.IamRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private IamRoleRepository roleRepository;
    @Mock
    private IamRoleExclusionRepository exclusionRepository;
    @InjectMocks
    private RoleService roleService;

    @Test
    void createRoleSavesActiveRole() {
        when(roleRepository.findByRoleCode("HR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(IamRole.class))).thenAnswer(inv -> inv.getArgument(0));

        IamRole role = roleService.createRole("HR", "人事", "FUNC", "人事专员");

        assertThat(role.getStatus()).isEqualTo("ACTIVE");
        assertThat(role.getIsDeleted()).isZero();
        assertThat(role.getRoleType()).isEqualTo("FUNC");
    }

    @Test
    void createRoleRejectsDuplicateCodeAndUnknownType() {
        when(roleRepository.findByRoleCode("HR")).thenReturn(Optional.of(new IamRole()));

        assertThatThrownBy(() -> roleService.createRole("HR", "人事", "FUNC", null))
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
        assertThatThrownBy(() -> roleService.createRole("X", "x", "BAD", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void getRoleThrowsNotFound() {
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRole(9L))
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void getAndListExcludeDeletedRoles() {
        IamRole active = TestFixtures.role(1L, "ACTIVE");
        IamRole deleted = TestFixtures.role(2L, "ACTIVE");
        deleted.setIsDeleted(1);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(active));
        when(roleRepository.findAll()).thenReturn(List.of(active, deleted));

        assertThat(roleService.getRole(1L)).isSameAs(active);
        assertThat(roleService.listRoles()).containsExactly(active);
    }
}
