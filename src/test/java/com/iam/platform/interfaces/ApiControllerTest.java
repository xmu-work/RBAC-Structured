package com.iam.platform.interfaces;

import com.iam.platform.application.account.AccountService;
import com.iam.platform.application.account.UserView;
import com.iam.platform.application.audit.AuditService;
import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.auth.LoginResponse;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.application.grant.GrantService;
import com.iam.platform.application.organization.OrganizationService;
import com.iam.platform.application.permission.DataPermissionService;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.application.role.RoleService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private DataPermissionService dataPermissionService;
    @MockitoBean
    private DecisionService decisionService;
    @MockitoBean
    private OrganizationService organizationService;
    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        when(authService.authenticate("good-token")).thenReturn(1L);
        when(authService.authenticate("bad-token")).thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID));
        when(functionPermissionService.findByApi(any(), any())).thenReturn(Optional.empty());
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

    // ---------- 角色与授权 ----------

    @Test
    void roleAndGrantEndpoints() throws Exception {
        when(roleService.createRole("HR", "人事", "FUNC", null)).thenReturn(new IamRole());
        when(roleService.getRole(2L)).thenReturn(new IamRole());
        when(grantService.grantRole(eq(1L), eq(2L), isNull(), isNull(), isNull())).thenReturn(new IamUserRole());
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
        verify(functionPermissionService).bindRoleFunctions(2L, List.of(10L, 11L), "VIEW", null);
        verify(functionPermissionService).unbindRoleFunctions(2L, List.of(10L));
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
}
