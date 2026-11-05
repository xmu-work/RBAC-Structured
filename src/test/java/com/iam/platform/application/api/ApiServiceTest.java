package com.iam.platform.application.api;

import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.IamApiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiServiceTest {

    @Mock
    private IamApiRepository apiRepository;
    @Mock
    private SystemService systemService;
    @InjectMocks
    private ApiService apiService;

    @Test
    void registerNormalizesMethodAndLeavesPermissionUnmapped() {
        when(systemService.getSystem(1L)).thenReturn(new IamSystem());
        when(apiRepository.findBySystemIdAndApiCode(1L, "USER_LIST")).thenReturn(Optional.empty());
        when(apiRepository.findByApiPathAndHttpMethod("/users", "GET")).thenReturn(Optional.empty());
        when(apiRepository.save(any(IamApi.class))).thenAnswer(inv -> inv.getArgument(0));

        IamApi api = apiService.registerApi(1L, "USER_LIST", "查询用户", "/users", "get");

        assertThat(api.getHttpMethod()).isEqualTo("GET");
        assertThat(api.getPermissionId()).isNull();
    }

    @Test
    void duplicateApiCodeOrRequestIsRejected() {
        when(systemService.getSystem(1L)).thenReturn(new IamSystem());
        when(apiRepository.findBySystemIdAndApiCode(1L, "USER_LIST")).thenReturn(Optional.of(new IamApi()));

        assertThatThrownBy(() -> apiService.registerApi(1L, "USER_LIST", "查询用户", "/users", "GET"))
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.RESOURCE_DUPLICATE.getCode()));
    }

    @Test
    void findRequestUsesPathAndMethod() {
        IamApi api = new IamApi();
        api.setIsDeleted(0);
        api.setStatus("ACTIVE");
        when(apiRepository.findByApiPathAndHttpMethod("/users/{userId}", "GET"))
                .thenReturn(Optional.of(api));

        assertThat(apiService.findByRequest("/users/{userId}", "get")).contains(api);
        verify(apiRepository, never()).save(any());
    }
}
