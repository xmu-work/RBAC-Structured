package com.iam.platform.interfaces;

import com.iam.platform.application.account.AccountService;
import com.iam.platform.application.account.UserView;
import com.iam.platform.application.api.ApiService;
import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.auth.LoginResponse;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.application.idempotency.IdempotencyStore;
import com.iam.platform.application.log.LogService;
import com.iam.platform.application.openapi.OpenApiAuthenticator;
import com.iam.platform.application.organization.OrganizationService;
import com.iam.platform.application.permission.PermissionService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
class ApiControllerTest {

    private static final String AUTH = "Bearer good-token";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AccountService accountService;
    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private SystemService systemService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private ApiService apiService;
    @MockitoBean
    private DecisionService decisionService;
    @MockitoBean
    private OrganizationService organizationService;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private OpenApiAuthenticator openApiAuthenticator;
    @MockitoBean
    private IdempotencyStore idempotencyStore;

    @BeforeEach
    void setUp() {
        when(authService.authenticate("good-token")).thenReturn(1L);
        when(authService.authenticate("bad-token")).thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID));
        IamApi registered = new IamApi();
        registered.setId(30000L);
        registered.setPermissionId(90000L);
        registered.setApiName("测试 API");
        lenient().when(apiService.findByRequest(any(), any())).thenReturn(Optional.of(registered));
        lenient().when(decisionService.hasPermission(anyLong(), eq(AuthService.ADMIN_SYSTEM_CODE), anyLong()))
                .thenReturn(true);
    }

    @Test
    void unregisteredApiIsDeniedByDefault() throws Exception {
        when(apiService.findByRequest("/users", "POST")).thenReturn(Optional.empty());

        mvc.perform(post("/users").header("Authorization", AUTH)
                        .param("userNo", "x").param("realName", "张三").param("orgId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_FUNCTION_PERM.getCode()));
        verify(accountService, never()).createUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void loginIsPublic() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("t");
        when(authService.login(any())).thenReturn(response);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("t"));
    }

    @Test
    void tokenIsRequiredForRoleApi() throws Exception {
        mvc.perform(get("/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/roles").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredApiWithoutPermissionIs403() throws Exception {
        IamApi api = new IamApi();
        api.setId(30004L);
        api.setPermissionId(20004L);
        api.setApiName("变更账号状态");
        when(apiService.findByRequest("/users/{userId}/status", "PUT")).thenReturn(Optional.of(api));
        when(decisionService.hasPermission(1L, "ADMIN", 20004L)).thenReturn(false);

        mvc.perform(put("/users/5/status").header("Authorization", AUTH).param("targetStatus", "DISABLED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_FUNCTION_PERM.getCode()));
        verify(accountService, never()).changeStatus(any(), any(), any(), any());
    }

    @Test
    void registeredApiWithPermissionPasses() throws Exception {
        IamApi api = new IamApi();
        api.setId(30005L);
        api.setPermissionId(20005L);
        when(apiService.findByRequest("/roles", "GET")).thenReturn(Optional.of(api));
        when(roleService.listRoles()).thenReturn(List.of(new IamRole()));

        mvc.perform(get("/roles").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void userEndpoints() throws Exception {
        IamUser user = new IamUser();
        UserView view = new UserView();
        view.setUserNo("zhang");
        when(accountService.createUser("zhang", null, "张三", null, 1L, null)).thenReturn(user);
        when(accountService.getUser(5L)).thenReturn(user);
        when(accountService.changeStatus(5L, "DISABLED", null, 1L)).thenReturn(user);
        when(accountService.listUsers(any(), isNull(), isNull())).thenReturn(new PageImpl<>(List.of(user)));
        when(accountService.toView(user)).thenReturn(view);

        mvc.perform(post("/users").header("Authorization", AUTH)
                        .param("userNo", "zhang").param("realName", "张三").param("orgId", "1"))
                .andExpect(jsonPath("$.data.userNo").value("zhang"));
        mvc.perform(get("/users/5").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.userNo").value("zhang"));
        mvc.perform(get("/users").header("Authorization", AUTH).param("page", "1").param("size", "20"))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(put("/users/5/status").header("Authorization", AUTH).param("targetStatus", "DISABLED"))
                .andExpect(status().isOk());
    }

    @Test
    void idempotencyFingerprintIncludesCaller() throws Exception {
        when(authService.authenticate("second-token")).thenReturn(2L);
        when(idempotencyStore.acquire(eq("caller-key"), anyString(), anyLong()))
                .thenReturn(IdempotencyStore.Status.ACQUIRED, IdempotencyStore.Status.CONFLICT);

        mvc.perform(post("/roles/2/assignments").header("Authorization", AUTH)
                        .header("Idempotency-Key", "caller-key").param("userId", "5"))
                .andExpect(status().isOk());
        mvc.perform(post("/roles/2/assignments").header("Authorization", "Bearer second-token")
                        .header("Idempotency-Key", "caller-key").param("userId", "5"))
                .andExpect(status().isConflict());

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(idempotencyStore, times(2)).acquire(eq("caller-key"), fingerprints.capture(), anyLong());
        Assertions.assertNotEquals(fingerprints.getAllValues().get(0), fingerprints.getAllValues().get(1));
    }

    @Test
    void roleAssignmentAndPermissionEndpoints() throws Exception {
        when(roleService.grantRole(eq(1L), eq(2L), isNull(), eq(1L), isNull())).thenReturn(new IamUserRole());
        when(roleService.getRoleUsers(2L)).thenReturn(List.of(new IamUserRole()));
        when(roleService.getRolePermissions(2L)).thenReturn(List.of(new IamRolePermission()));

        mvc.perform(post("/roles/2/assignments").header("Authorization", AUTH).param("userId", "1"))
                .andExpect(status().isOk());
        mvc.perform(delete("/roles/2/assignments/1").header("Authorization", AUTH))
                .andExpect(status().isOk());
        mvc.perform(get("/roles/2/assignments").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(post("/roles/2/permissions").header("Authorization", AUTH)
                        .param("allowedAction", "VIEW").contentType(MediaType.APPLICATION_JSON)
                        .content("[10003,20001]"))
                .andExpect(status().isOk());
        mvc.perform(get("/roles/2/permissions").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(roleService).revokeRole(1L, 2L, 1L);
        verify(roleService).bindRolePermissions(2L, List.of(10003L, 20001L), "VIEW", 1L);
    }

    @Test
    void systemPermissionAndApiEndpoints() throws Exception {
        when(systemService.registerSystem("OA", "办公", null)).thenReturn(new IamSystem());
        when(systemService.listSystems()).thenReturn(List.of());
        when(permissionService.createPermission(
                1L, 10L, "USER_EXPORT", "导出用户", "BUTTON", null, "VIEW", null))
                .thenReturn(new IamPermission());
        when(permissionService.listPermissions(1L)).thenReturn(List.of(new IamPermission()));
        when(apiService.registerApi(1L, "USER_EXPORT", "导出用户", "/users/export", "POST"))
                .thenReturn(new IamApi());
        when(apiService.listApis(1L)).thenReturn(List.of(new IamApi()));

        mvc.perform(post("/systems").header("Authorization", AUTH)
                        .param("systemCode", "OA").param("systemName", "办公"))
                .andExpect(status().isOk());
        mvc.perform(get("/systems").header("Authorization", AUTH)).andExpect(status().isOk());
        mvc.perform(post("/permissions").header("Authorization", AUTH)
                        .param("systemId", "1").param("parentId", "10")
                        .param("permissionCode", "USER_EXPORT").param("permissionName", "导出用户")
                        .param("permissionType", "BUTTON").param("action", "VIEW"))
                .andExpect(status().isOk());
        mvc.perform(get("/permissions").header("Authorization", AUTH).param("systemId", "1"))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(post("/apis").header("Authorization", AUTH)
                        .param("systemId", "1").param("apiCode", "USER_EXPORT")
                        .param("apiName", "导出用户").param("apiPath", "/users/export")
                        .param("httpMethod", "POST"))
                .andExpect(status().isOk());
        mvc.perform(get("/apis").header("Authorization", AUTH).param("systemId", "1"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void organizationEndpointsUseOrganizationResource() throws Exception {
        IamOrg hq = new IamOrg();
        hq.setOrgCode("HQ1");
        when(organizationService.getOrgTree(null)).thenReturn(List.of(hq));
        when(organizationService.getChildren(1L)).thenReturn(List.of());
        when(organizationService.getOrg(1L)).thenReturn(hq);
        when(organizationService.createOrg("BR1", "分公司", "BRANCH", 2, 1L, null))
                .thenReturn(new IamOrg());
        when(organizationService.moveOrg(3L, 12L)).thenReturn(new IamOrg());

        mvc.perform(get("/organizations").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data[0].orgCode").value("HQ1"));
        mvc.perform(get("/organizations/1/children").header("Authorization", AUTH))
                .andExpect(status().isOk());
        mvc.perform(post("/organizations").header("Authorization", AUTH)
                        .param("orgCode", "BR1").param("orgName", "分公司")
                        .param("orgType", "BRANCH").param("orgLevel", "2").param("parentId", "1"))
                .andExpect(status().isOk());
        mvc.perform(put("/organizations/3/parent").header("Authorization", AUTH).param("newParentId", "12"))
                .andExpect(status().isOk());
    }

    @Test
    void unifiedLogEndpointReturnsActionAndPermissionChanges() throws Exception {
        IamLog entry = new IamLog();
        entry.setLogId(9L);
        entry.setLogType(LogService.TYPE_PERMISSION_CHANGE);
        entry.setAction("GRANT_ROLE");
        entry.setSuccess(1);
        when(logService.queryLogs(any(), isNull(), isNull(), eq(LogService.TYPE_PERMISSION_CHANGE),
                isNull(), isNull())).thenReturn(new PageImpl<>(List.of(entry)));

        mvc.perform(get("/logs").header("Authorization", AUTH)
                        .param("logType", LogService.TYPE_PERMISSION_CHANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].logType").value(LogService.TYPE_PERMISSION_CHANGE))
                .andExpect(jsonPath("$.data.list[0].action").value("GRANT_ROLE"));
    }

    @Test
    void openApiAuthorizesIndependentApiAndReturnsFrontendPermissions() throws Exception {
        IamApi api = new IamApi();
        api.setId(30001L);
        api.setPermissionId(20001L);
        when(apiService.getApi(30001L)).thenReturn(api);
        when(decisionService.hasPermission(1L, "ADMIN", 20001L)).thenReturn(true);
        when(decisionService.getUserPermissions(1L, "ADMIN")).thenReturn(Set.of(20001L));

        mvc.perform(get("/open/authorize").header("Authorization", AUTH)
                        .param("userId", "1").param("systemCode", "ADMIN").param("apiId", "30001"))
                .andExpect(jsonPath("$.data").value(true));
        mvc.perform(post("/open/authorize/batch").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"systemCode\":\"ADMIN\",\"apiIds\":[30001]}"))
                .andExpect(jsonPath("$.data.results.30001").value(true));
        mvc.perform(get("/open/permissions").header("Authorization", AUTH)
                        .param("userId", "1").param("systemCode", "ADMIN"))
                .andExpect(jsonPath("$.data[0]").value(20001));
    }

    @Test
    void dataPermissionEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/systems/data-scopes").header("Authorization", AUTH))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/open/authorize/resource").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"systemCode\":\"ADMIN\",\"apiId\":30001}"))
                .andExpect(status().isNotFound());
    }
}
