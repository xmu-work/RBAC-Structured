package com.iam.platform.application.permission;

import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.IamApiRepository;
import com.iam.platform.infrastructure.persistence.repository.IamPermissionRepository;
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
class PermissionServiceTest {

    @Mock
    private IamPermissionRepository permissionRepository;
    @Mock
    private IamApiRepository apiRepository;
    @Mock
    private SystemService systemService;
    @InjectMocks
    private PermissionService permissionService;

    @Test
    void createsMenuButtonTreeNode() {
        when(systemService.getSystem(1L)).thenReturn(new IamSystem());
        when(permissionRepository.findBySystemIdAndPermissionCode(1L, "USER_EXPORT"))
                .thenReturn(Optional.empty());
        when(permissionRepository.save(any(IamPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        IamPermission permission = permissionService.createPermission(
                1L, null, "USER_EXPORT", "导出用户", "button", null, "VIEW", null);

        assertThat(permission.getPermissionType()).isEqualTo("BUTTON");
        assertThat(permission.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void invalidTypeIsRejected() {
        when(systemService.getSystem(1L)).thenReturn(new IamSystem());

        assertThatThrownBy(() -> permissionService.createPermission(
                1L, null, "X", "X", "PAGE", null, "VIEW", null))
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void mapsMultipleApisToSamePermission() {
        IamPermission permission = permission(10L, 1L);
        IamApi first = api(100L, 1L);
        IamApi second = api(101L, 1L);
        when(permissionRepository.findById(10L)).thenReturn(Optional.of(permission));
        when(apiRepository.findById(100L)).thenReturn(Optional.of(first));
        when(apiRepository.findById(101L)).thenReturn(Optional.of(second));
        when(apiRepository.save(any(IamApi.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(permissionService.bindApi(10L, 100L).getPermissionId()).isEqualTo(10L);
        assertThat(permissionService.bindApi(10L, 101L).getPermissionId()).isEqualTo(10L);
    }

    @Test
    void crossSystemMappingIsRejected() {
        when(permissionRepository.findById(10L)).thenReturn(Optional.of(permission(10L, 1L)));
        when(apiRepository.findById(100L)).thenReturn(Optional.of(api(100L, 2L)));

        assertThatThrownBy(() -> permissionService.bindApi(10L, 100L))
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_INVALID.getCode()));
    }

    private static IamPermission permission(Long id, Long systemId) {
        IamPermission permission = new IamPermission();
        permission.setId(id);
        permission.setSystemId(systemId);
        permission.setStatus("ACTIVE");
        permission.setIsDeleted(0);
        return permission;
    }

    private static IamApi api(Long id, Long systemId) {
        IamApi api = new IamApi();
        api.setId(id);
        api.setSystemId(systemId);
        api.setStatus("ACTIVE");
        api.setIsDeleted(0);
        return api;
    }
}
