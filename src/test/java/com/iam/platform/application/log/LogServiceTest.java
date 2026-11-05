package com.iam.platform.application.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.infrastructure.persistence.entity.IamLog;
import com.iam.platform.infrastructure.persistence.repository.IamLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private IamLogRepository logRepository;
    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogService(logRepository, new ObjectMapper());
    }

    @Test
    void actionAndPermissionChangeUseSameTableButDifferentTypes() {
        logService.record(1L, "admin", "ADMIN", "POST /roles", "/roles",
                "roleCode=HR", "127.0.0.1", true, null, "req-1");
        logService.recordPermissionChange(1L, "ADMIN", "GRANT_ROLE", "user:2/role:3",
                null, Map.of("roleId", 3, "status", "ACTIVE"), "127.0.0.1", "req-2");

        ArgumentCaptor<IamLog> logs = ArgumentCaptor.forClass(IamLog.class);
        verify(logRepository, org.mockito.Mockito.times(2)).save(logs.capture());
        assertThat(logs.getAllValues().get(0).getLogType()).isEqualTo(LogService.TYPE_ACTION);
        assertThat(logs.getAllValues().get(1).getLogType()).isEqualTo(LogService.TYPE_PERMISSION_CHANGE);
        assertThat(logs.getAllValues().get(1).getBeforeSnapshot()).isNull();
        assertThat(logs.getAllValues().get(1).getAfterSnapshot()).contains("\"roleId\":3");
        assertThat(logs.getAllValues().get(1).getRequestId()).isEqualTo("req-2");
    }
}
