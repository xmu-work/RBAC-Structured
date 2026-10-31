package com.iam.platform.interfaces;

import com.iam.platform.application.account.AccountService;
import com.iam.platform.application.account.UserView;
import com.iam.platform.application.audit.AuditService;
import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.auth.LoginResponse;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.idempotency.IdempotencyStore;
import com.iam.platform.application.organization.OrganizationService;
import com.iam.platform.application.openapi.OpenApiAuthenticator;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.application.role.RoleService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层测试：真实的 Controller、拦截器、异常处理器，服务层全部 mock。
 * 请求路径不含 context-path（/api/v1），与 Controller 映射一致。
 */
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
    private GrantService grantService;
    @MockitoBean
    private FunctionPermissionService functionPermissionService;
    @MockitoBean
    private DecisionService decisionService;
    @MockitoBean
    private OrganizationService organizationService;
    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private OpenApiAuthenticator openApiAuthenticator;
    @MockitoBean
    private IdempotencyStore idempotencyStore;

    @BeforeEach
    void setUp() {
        when(authService.authenticate("good-token")).thenReturn(1L);
        when(authService.authenticate("bad-token")).thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID));
        // fail-closed：默认把接口视为“已登记 + 有权限”，未登记/无权限由各用例覆盖
        IamFunctionPoint registered = new IamFunctionPoint();
        registered.setId(90000L);
        registered.setPointName("测试接口");
        when(functionPermissionService.findByApi(any(), any())).thenReturn(Optional.of(registered));
        when(decisionService.hasPermission(anyLong(), eq(AuthService.ADMIN_SYSTEM_CODE), anyLong())).thenReturn(true);
    }

    @Test
    void unregisteredApiIsDeniedByDefault() throws Exception {
        // 未登记为功能点的接口默认拒绝（fail-closed），不再放行任意已登录用户
        when(functionPermissionService.findByApi("/users", "POST")).thenReturn(Optional.empty());

        mvc.perform(post("/users").header("Authorization", AUTH)
                        .param("userNo", "x").param("realName", "张三").param("orgId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_FUNCTION_PERM.getCode()));
        verify(accountService, never()).createUser(any(), any(), any(), any(), any(), any());
    }

    // ---------- 认证与鉴权 ----------

    @Test
    void loginIsPublic() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("t");
        when(authService.login(any())).thenReturn(response);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("t"));
    }

    @Test
    void requestWithoutOrWithBadTokenIs401() throws Exception {
        mvc.perform(get("/roles")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
        mvc.perform(get("/roles").header("Authorization", "Bearer bad-token")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOKEN_INVALID.getCode()));
    }

    @Test
    void registeredApiWithoutPermissionIs403() throws Exception {
        IamFunctionPoint fp = new IamFunctionPoint();
        fp.setId(20004L);
        fp.setPointName("变更账号状态");
        when(functionPermissionService.findByApi("/users/{userId}/status", "PUT")).thenReturn(Optional.of(fp));
        when(decisionService.hasPermission(1L, "ADMIN", 20004L)).thenReturn(false);

        mvc.perform(put("/users/5/status").header("Authorization", AUTH).param("targetStatus", "DISABLED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_FUNCTION_PERM.getCode()));
        verify(accountService, never()).changeStatus(any(), any(), any());
    }

    @Test
    void registeredApiWithPermissionPasses() throws Exception {
        IamFunctionPoint fp = new IamFunctionPoint();
        fp.setId(20005L);
        when(functionPermissionService.findByApi("/roles", "GET")).thenReturn(Optional.of(fp));
        when(decisionService.hasPermission(1L, "ADMIN", 20005L)).thenReturn(true);
        when(roleService.listRoles()).thenReturn(List.of(new IamRole()));

        mvc.perform(get("/roles").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void logoutAndValidateUseBearerToken() throws Exception {
        when(authService.validateToken("good-token")).thenReturn(true);

        mvc.perform(post("/auth/logout").header("Authorization", AUTH)).andExpect(status().isOk());
        mvc.perform(get("/auth/validate").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data").value(true));
        verify(authService).logout("good-token");
    }

    @Test
    void changePasswordEndpointUsesAuthenticatedUser() throws Exception {
        mvc.perform(put("/auth/password").header("Authorization", AUTH)
                        .param("oldPassword", "Old@123456").param("newPassword", "New@123456"))
                .andExpect(status().isOk());
        verify(authService).changePassword(1L, "Old@123456", "New@123456");
    }

    // ---------- 用户 ----------

    @Test
    void userEndpoints() throws Exception {
        IamUser user = new IamUser();
        UserView view = new UserView();
        view.setUserNo("zhang");
        when(accountService.createUser("zhang", null, "张三", null, 1L, null)).thenReturn(user);
        when(accountService.getUser(5L)).thenReturn(user);
        when(accountService.changeStatus(5L, "DISABLED", null)).thenReturn(user);
        when(accountService.listUsers(any(), isNull(), isNull())).thenReturn(new PageImpl<>(List.of(user)));
        when(accountService.toView(user)).thenReturn(view);

        mvc.perform(post("/users").header("Authorization", AUTH)
                        .param("userNo", "zhang").param("realName", "张三").param("orgId", "1"))
                .andExpect(jsonPath("$.data.userNo").value("zhang"));
        mvc.perform(get("/users/5").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.userNo").value("zhang"));
        mvc.perform(put("/users/5/status").header("Authorization", AUTH).param("targetStatus", "DISABLED"))
                .andExpect(status().isOk());
        mvc.perform(get("/users").header("Authorization", AUTH).param("page", "1").param("size", "20"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].userNo").value("zhang"));
    }

    @Test
    void businessAndParameterErrorsMapToHttpStatus() throws Exception {
        when(accountService.getUser(9L)).thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mvc.perform(get("/users/9").header("Authorization", AUTH)).andExpect(status().isNotFound());
        mvc.perform(post("/users").header("Authorization", AUTH).param("userNo", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
        mvc.perform(get("/users/abc").header("Authorization", AUTH)).andExpect(status().isBadRequest());
    }

    @Test
    void oversizedPageSizeIsRejected() throws Exception {
        mvc.perform(get("/users").header("Authorization", AUTH).param("page", "1").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void loginBlankCredentialsAreRejected() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void responseEchoesRequestId() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of());

        mvc.perform(get("/roles").header("Authorization", AUTH).header("X-Request-Id", "req-abc"))
                .andExpect(header().string("X-Request-Id", "req-abc"))
                .andExpect(jsonPath("$.requestId").value("req-abc"));
    }

    @Test
    void idempotencyFingerprintIncludesRequestBody() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("first");
        when(authService.login(any())).thenReturn(response);
        when(idempotencyStore.acquire(eq("body-key"), anyString(), anyLong()))
                .thenReturn(IdempotencyStore.Status.ACQUIRED, IdempotencyStore.Status.CONFLICT);

        mvc.perform(post("/auth/login").header("Idempotency-Key", "body-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"first-password\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").header("Idempotency-Key", "body-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"second-password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.IDEMPOTENCY_CONFLICT.getCode()));

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(idempotencyStore, times(2)).acquire(eq("body-key"), fingerprints.capture(), anyLong());
        Assertions.assertNotEquals(fingerprints.getAllValues().get(0), fingerprints.getAllValues().get(1));
    }

    @Test
    void unsafeIdempotencyKeyIsRejectedBeforeExecution() throws Exception {
        mvc.perform(post("/auth/login").header("Idempotency-Key", "bad key with spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

        verifyNoInteractions(idempotencyStore);
        verify(authService, never()).login(any());
    }

    @Test
    void idempotencyFingerprintIncludesCaller() throws Exception {
        when(authService.authenticate("second-token")).thenReturn(2L);
        when(idempotencyStore.acquire(eq("caller-key"), anyString(), anyLong()))
                .thenReturn(IdempotencyStore.Status.ACQUIRED, IdempotencyStore.Status.CONFLICT);

        mvc.perform(post("/roles/grants").header("Authorization", AUTH).header("Idempotency-Key", "caller-key")
                        .param("userId", "5").param("roleId", "2"))
                .andExpect(status().isOk());
        mvc.perform(post("/roles/grants").header("Authorization", "Bearer second-token")
                        .header("Idempotency-Key", "caller-key")
                        .param("userId", "5").param("roleId", "2"))
                .andExpect(status().isConflict());

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(idempotencyStore, times(2)).acquire(eq("caller-key"), fingerprints.capture(), anyLong());
        Assertions.assertNotEquals(fingerprints.getAllValues().get(0), fingerprints.getAllValues().get(1));
    }

    @Test
    void completedRequestReplaysResponseWithCurrentRequestId() throws Exception {
        when(idempotencyStore.acquire(eq("complete-key"), anyString(), anyLong()))
                .thenReturn(IdempotencyStore.Status.COMPLETED);
        when(idempotencyStore.getResponse("complete-key")).thenReturn(
                "200\n{\"code\":0,\"message\":\"success\",\"data\":{\"accessToken\":\"cached\"},"
                        + "\"requestId\":\"original-request\"}");

        mvc.perform(post("/auth/login").header("Idempotency-Key", "complete-key")
                        .header("X-Request-Id", "current-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "current-request"))
                .andExpect(jsonPath("$.data.accessToken").value("cached"))
                .andExpect(jsonPath("$.requestId").value("current-request"));
        verify(authService, never()).login(any());
    }

    @Test
    void grantingAbnormalUserIsConflictNotForbidden() throws Exception {
        // 目标用户状态异常不能返回 2005/2006（403），否则前端会把操作人强制登出
        when(grantService.grantRole(eq(5L), eq(2L), isNull(), eq(1L), isNull()))
                .thenThrow(new BusinessException(ErrorCode.TARGET_USER_ABNORMAL));

        mvc.perform(post("/roles/grants").header("Authorization", AUTH).param("userId", "5").param("roleId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(3010));
    }

    // ---------- 角色与授权 ----------

    @Test
    void roleAndGrantEndpoints() throws Exception {
        when(roleService.createRole("HR", "人事", "FUNC", null)).thenReturn(new IamRole());
        when(roleService.getRole(2L)).thenReturn(new IamRole());
        when(grantService.grantRole(eq(1L), eq(2L), isNull(), eq(1L), isNull())).thenReturn(new IamUserRole());
        when(grantService.getUserRoles(1L)).thenReturn(List.of(new IamUserRole()));

        mvc.perform(post("/roles").header("Authorization", AUTH)
                        .param("roleCode", "HR").param("roleName", "人事").param("roleType", "FUNC"))
                .andExpect(status().isOk());
        mvc.perform(get("/roles/2").header("Authorization", AUTH)).andExpect(status().isOk());
        mvc.perform(post("/roles/grants").header("Authorization", AUTH).param("userId", "1").param("roleId", "2"))
                .andExpect(status().isOk());
        mvc.perform(delete("/roles/grants").header("Authorization", AUTH).param("userId", "1").param("roleId", "2"))
                .andExpect(status().isOk());
        mvc.perform(get("/roles/grants/1").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.length()").value(1));
        verify(grantService).revokeRole(1L, 2L);
    }

    // ---------- 子系统与功能权限 ----------

    @Test
    void systemAndFunctionPermissionEndpoints() throws Exception {
        when(functionPermissionService.registerSystem("OA", "办公", null)).thenReturn(new IamSystem());
        when(functionPermissionService.listSystems()).thenReturn(List.of());
        when(functionPermissionService.registerModule(1L, "M", "模块", null)).thenReturn(new IamModule());
        when(functionPermissionService.registerFunctionPoint(1L, "C", "N", "VIEW", null, null)).thenReturn(new IamFunctionPoint());
        when(functionPermissionService.getRoleFunctions(2L)).thenReturn(List.of(new IamRoleFunction()));

        mvc.perform(post("/systems").header("Authorization", AUTH).param("systemCode", "OA").param("systemName", "办公"))
                .andExpect(status().isOk());
        mvc.perform(get("/systems").header("Authorization", AUTH)).andExpect(status().isOk());
        mvc.perform(post("/systems/modules").header("Authorization", AUTH)
                        .param("systemId", "1").param("moduleCode", "M").param("moduleName", "模块"))
                .andExpect(status().isOk());
        mvc.perform(post("/systems/function-points").header("Authorization", AUTH)
                        .param("moduleId", "1").param("pointCode", "C").param("pointName", "N").param("action", "VIEW"))
                .andExpect(status().isOk());
        mvc.perform(post("/systems/role-functions").header("Authorization", AUTH)
                        .param("roleId", "2").param("allowedAction", "VIEW")
                        .contentType(MediaType.APPLICATION_JSON).content("[10,11]"))
                .andExpect(status().isOk());
        mvc.perform(delete("/systems/role-functions").header("Authorization", AUTH)
                        .param("roleId", "2").contentType(MediaType.APPLICATION_JSON).content("[10]"))
                .andExpect(status().isOk());
        mvc.perform(get("/systems/role-functions/2").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.length()").value(1));
        verify(functionPermissionService).bindRoleFunctions(2L, List.of(10L, 11L), "VIEW", 1L);
        verify(functionPermissionService).unbindRoleFunctions(2L, List.of(10L));
    }

    // ---------- 组织机构 ----------

    @Test
    void organizationEndpoints() throws Exception {
        IamOrg hq = new IamOrg();
        hq.setOrgCode("HQ1");
        when(organizationService.getOrgTree(null)).thenReturn(List.of(hq));
        when(organizationService.getChildren(1L)).thenReturn(List.of());
        when(organizationService.getOrg(1L)).thenReturn(hq);
        when(organizationService.createOrg("BR1", "分公司", "BRANCH", 2, 1L, null)).thenReturn(new IamOrg());
        when(organizationService.moveOrg(3L, 12L)).thenReturn(new IamOrg());

        mvc.perform(get("/orgs").header("Authorization", AUTH)).andExpect(jsonPath("$.data[0].orgCode").value("HQ1"));
        mvc.perform(get("/orgs/1/children").header("Authorization", AUTH)).andExpect(status().isOk());
        mvc.perform(get("/orgs/1").header("Authorization", AUTH)).andExpect(jsonPath("$.data.orgCode").value("HQ1"));
        mvc.perform(post("/orgs").header("Authorization", AUTH).param("orgCode", "BR1").param("orgName", "分公司")
                        .param("orgType", "BRANCH").param("orgLevel", "2").param("parentId", "1"))
                .andExpect(status().isOk());
        mvc.perform(put("/orgs/3/move").header("Authorization", AUTH).param("newParentId", "12"))
                .andExpect(status().isOk());
    }

    // ---------- 开放鉴权接口 ----------

    @Test
    void openApiEndpoints() throws Exception {
        PermissionCheckResult batch = new PermissionCheckResult();
        batch.getResults().put(100L, true);
        when(decisionService.hasPermission(1L, "ADMIN", 100L)).thenReturn(true);
        when(decisionService.checkPermissions(1L, "ADMIN", List.of(100L))).thenReturn(batch);
        when(decisionService.getUserFunctionPoints(1L, "ADMIN")).thenReturn(Set.of(100L));

        mvc.perform(get("/open/authorize").header("Authorization", AUTH)
                        .param("userId", "1").param("systemCode", "ADMIN").param("functionPointId", "100"))
                .andExpect(jsonPath("$.data").value(true));
        mvc.perform(post("/open/authorize/batch").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"systemCode\":\"ADMIN\",\"functionPointIds\":[100]}"))
                .andExpect(jsonPath("$.data.results.100").value(true));
        mvc.perform(get("/open/permissions").header("Authorization", AUTH).param("userId", "1").param("systemCode", "ADMIN"))
                .andExpect(jsonPath("$.data[0]").value(100));
    }

    @Test
    void openApiUserTokenCannotQueryOthers() throws Exception {
        // 普通用户令牌只能查本人，查他人需接入方签名
        mvc.perform(get("/open/permissions").header("Authorization", AUTH)
                        .param("userId", "2").param("systemCode", "ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_FUNCTION_PERM.getCode()));
    }

    @Test
    void openApiAppSignatureAllowsQueryingOthers() throws Exception {
        when(decisionService.getUserFunctionPoints(2L, "ADMIN")).thenReturn(Set.of(100L));

        mvc.perform(get("/open/permissions")
                        .header("X-App-Id", "OA").header("X-Timestamp", "1")
                        .header("X-Nonce", "n").header("X-Signature", "s")
                        .param("userId", "2").param("systemCode", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(100));
        verify(openApiAuthenticator).verify("OA", "1", "n", "s");
    }

    @Test
    void dataPermissionEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/systems/data-scopes").header("Authorization", AUTH))
                .andExpect(status().isNotFound());
        mvc.perform(get("/open/data-scope").header("Authorization", AUTH))
                .andExpect(status().isNotFound());
        mvc.perform(post("/open/authorize/resource").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"systemCode\":\"ADMIN\",\"functionPointId\":100}"))
                .andExpect(status().isNotFound());
    }
}
