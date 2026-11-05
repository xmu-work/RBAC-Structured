package com.iam.platform.application.system;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.IamSystemRepository;
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
class SystemServiceTest {

    @Mock
    private IamSystemRepository systemRepository;
    @InjectMocks
    private SystemService systemService;

    @Test
    void registerAndListActiveSystems() {
        when(systemRepository.findBySystemCode("OA")).thenReturn(Optional.empty());
        when(systemRepository.save(any(IamSystem.class))).thenAnswer(inv -> inv.getArgument(0));

        IamSystem created = systemService.registerSystem("OA", "协同办公", null);

        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getIsDeleted()).isZero();
    }

    @Test
    void duplicateSystemCodeIsRejected() {
        when(systemRepository.findBySystemCode("OA")).thenReturn(Optional.of(new IamSystem()));

        assertThatThrownBy(() -> systemService.registerSystem("OA", "协同办公", null))
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
        verify(systemRepository, never()).save(any());
    }

    @Test
    void listFiltersDeletedSystems() {
        IamSystem active = new IamSystem();
        active.setIsDeleted(0);
        IamSystem deleted = new IamSystem();
        deleted.setIsDeleted(1);
        when(systemRepository.findAll()).thenReturn(List.of(active, deleted));

        assertThat(systemService.listSystems()).containsExactly(active);
    }
}
