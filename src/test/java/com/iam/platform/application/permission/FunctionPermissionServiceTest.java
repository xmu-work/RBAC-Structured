package com.iam.platform.application.permission;

import com.iam.platform.TestFixtures;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import com.iam.platform.infrastructure.persistence.entity.IamModule;
import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FunctionPermissionServiceTest {

    @Mock
    private IamSystemRepository systemRepository;
    @Mock
    private IamModuleRepository moduleRepository;
    @Mock
    private IamFunctionPointRepository functionPointRepository;
    @Mock
    private IamRoleFunctionRepository roleFunctionRepository;
    @Mock
    private IamRoleRepository roleRepository;
    @InjectMocks
    private FunctionPermissionService service;

    private static int code(Throwable e) {
        return ((BusinessException) e).getCode();
    }

    private static IamRoleFunction binding(Long roleId, Long pointId) {
        IamRoleFunction rf = new IamRoleFunction();
        rf.setRoleId(roleId);
        rf.setFunctionPointId(pointId);
        return rf;
    }

    @Test
    void registerSystemSavesOrRejectsDuplicate() {
        when(systemRepository.findBySystemCode("OA")).thenReturn(Optional.empty());
        when(systemRepository.findBySystemCode("HR")).thenReturn(Optional.of(new IamSystem()));
        when(systemRepository.save(any(IamSystem.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.registerSystem("OA", "办公", null).getStatus()).isEqualTo("ACTIVE");
        assertThatThrownBy(() -> service.registerSystem("HR", "人事", null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
    }

    @Test
    void registerModuleRequiresExistingSystem() {
        when(systemRepository.existsById(1L)).thenReturn(true);
        when(systemRepository.existsById(2L)).thenReturn(false);
        when(moduleRepository.save(any(IamModule.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.registerModule(1L, "M", "模块", null).getSortNo()).isZero();
        assertThatThrownBy(() -> service.registerModule(2L, "M", "模块", 1))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void registerFunctionPointNormalizesMethod() {
        when(moduleRepository.existsById(1L)).thenReturn(true);
        when(functionPointRepository.findByApiPathAndApiMethod("/users", "GET")).thenReturn(Optional.empty());
        when(functionPointRepository.save(any(IamFunctionPoint.class))).thenAnswer(inv -> inv.getArgument(0));

        IamFunctionPoint point = service.registerFunctionPoint(1L, "USER_LIST", "查询用户", "VIEW", "/users", "get");

        assertThat(point.getApiMethod()).isEqualTo("GET");
        assertThat(point.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void registerFunctionPointValidatesModuleActionAndApiUniqueness() {
        when(moduleRepository.existsById(1L)).thenReturn(true);
        when(moduleRepository.existsById(2L)).thenReturn(false);
        when(functionPointRepository.findByApiPathAndApiMethod("/users", "GET")).thenReturn(Optional.of(new IamFunctionPoint()));

        assertThatThrownBy(() -> service.registerFunctionPoint(2L, "c", "n", "VIEW", null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        assertThatThrownBy(() -> service.registerFunctionPoint(1L, "c", "n", "FLY", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registerFunctionPoint(1L, "c", "n", "VIEW", "/users", "GET"))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
    }

    @Test
    void bindRoleFunctionsSkipsAlreadyBoundPoints() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.role(1L, "ACTIVE")));
        when(roleFunctionRepository.findByRoleId(1L)).thenReturn(List.of(binding(1L, 10L)));
        when(functionPointRepository.existsById(any())).thenReturn(true);

        service.bindRoleFunctions(1L, List.of(10L, 11L, 11L), "VIEW", 9L);

        ArgumentCaptor<IamRoleFunction> saved = ArgumentCaptor.forClass(IamRoleFunction.class);
        verify(roleFunctionRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getFunctionPointId()).isEqualTo(11L);
        assertThat(saved.getValue().getAllowedAction()).isEqualTo("VIEW");
    }

    @Test
    void bindRoleFunctionsRejectsDisabledRoleAndMissingPoint() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.role(1L, "DISABLED")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.role(2L, "ACTIVE")));
        when(roleFunctionRepository.findByRoleId(2L)).thenReturn(List.of());
        when(functionPointRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.bindRoleFunctions(1L, List.of(10L), "VIEW", null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        assertThatThrownBy(() -> service.bindRoleFunctions(2L, List.of(99L), "VIEW", null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
        verify(roleFunctionRepository, never()).save(any());
    }

    @Test
    void unbindDeletesOnlyListedPoints() {
        IamRoleFunction keep = binding(1L, 10L);
        IamRoleFunction drop = binding(1L, 11L);
        when(roleFunctionRepository.findByRoleId(1L)).thenReturn(List.of(keep, drop));

        service.unbindRoleFunctions(1L, List.of(11L, 12L));

        verify(roleFunctionRepository).delete(drop);
        verify(roleFunctionRepository, never()).delete(keep);
    }

    @Test
    void queriesDelegateToRepositories() {
        IamSystem active = new IamSystem();
        active.setIsDeleted(0);
        IamSystem deleted = new IamSystem();
        deleted.setIsDeleted(1);
        when(systemRepository.findAll()).thenReturn(List.of(active, deleted));
        when(roleFunctionRepository.findByRoleId(1L)).thenReturn(List.of());
        when(functionPointRepository.findByApiPathAndApiMethod("/x", "GET")).thenReturn(Optional.empty());

        assertThat(service.listSystems()).containsExactly(active);
        assertThat(service.getRoleFunctions(1L)).isEmpty();
        assertThat(service.findByApi("/x", "GET")).isEmpty();
    }
}
