# 统一应用软件权限管理系统 API 设计说明书

| 项目 | 内容 |
|---|---|
| 文档名称 | 统一应用软件权限管理系统 API 设计说明书 |
| 项目全称 | 统一应用软件权限管理系统（RBAC 统一身份与权限平台） |
| 文档版本 | V1.0 |
| 编制单位 | 信息中心 / 系统架构组 |
| 上游依据 | 《统一应用软件权限管理系统需求规格说明书（SRS）》V1.0、《统一应用软件权限管理系统 概要/结构化设计说明书》V1.0 |
| 适用阶段 | 详细设计阶段（课程期末大作业交付物） |
| 密级 | 内部 |

---

## 1. 设计约定

### 1.1 基础约定

| 项 | 约定 |
|---|---|
| Base URL | `/api/v1` |
| 协议 | HTTPS（生产强制 TLS 1.2+） |
| 数据格式 | 请求/响应统一 `application/json; charset=utf-8` |
| 时间格式 | ISO8601，含时区，如 `2025-06-01T09:30:00+08:00` |
| 字段命名 | 小写下划线（`user_id`、`system_code`、`api_path`） |
| 分页参数 | 请求 `page`（从 1 开始）、`page_size`（默认 20，最大 200）；响应 `total`、`list` |
| 排序参数 | `sort_by`、`sort_order`（`asc`/`desc`） |
| 请求追踪 | 所有请求头携带 `X-Request-Id`（UUID），响应原样回传，用于日志关联 |
| 幂等 | 写接口支持请求头 `Idempotency-Key`（UUID），同 key 在 24h 内重复提交返回首次结果 |
| 版本演进 | 破坏性变更升 `/api/v2`；非破坏性字段新增不升版本 |

### 1.2 认证方式

| 场景 | 方式 | 说明 |
|---|---|---|
| 管理后台（Web） | Bearer Token | 登录后签发 JWT 访问令牌（`access_token`，默认 2h）+ 刷新令牌（`refresh_token`，默认 7d） |
| 对外 OpenAPI（18 套业务系统） | appKey/appSecret + 签名 | 请求头 `X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Signature`；签名算法 `HMAC-SHA256(appSecret, method + path + timestamp + nonce + body_sha256)` |
| 高安全场景（可选） | 双向 TLS | 业务系统客户端证书校验，与签名二选一或叠加 |

**防重放**：`X-Timestamp` 与服务端时间偏差超过 300s 拒绝；`X-Nonce` 在有效窗口内唯一（Redis 记录，TTL=300s），重复即拒绝。所有对外接口响应携带 `X-Request-Id` 与 `X-Server-Time`。

### 1.3 统一响应包

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "request_id": "b1e2c3d4-...",
  "timestamp": "2025-06-01T09:30:00+08:00"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务码，0 表示成功 |
| message | string | 人类可读信息 |
| data | object/array/null | 业务数据 |
| request_id | string | 请求追踪 ID |
| timestamp | string | 服务端时间（ISO8601） |

分页响应 `data` 结构：

```json
{ "total": 10000, "page": 1, "page_size": 20, "list": [] }
```

### 1.4 统一错误码表

| 分段 | 含义 | 错误码 | 名称 | HTTP | 说明 |
|---|---|---|---|---|---|
| 0 | 成功 | 0 | SUCCESS | 200 | 成功 |
| 1xxx | 参数/校验 | 1001 | PARAM_INVALID | 400 | 参数校验失败（附 `data.errors`） |
| | | 1002 | PARAM_MISSING | 400 | 必填参数缺失 |
| | | 1003 | PARAM_FORMAT_ERROR | 400 | 格式错误（时间/枚举/JSON） |
| | | 1004 | IDEMPOTENCY_CONFLICT | 409 | 幂等键冲突且请求体不一致 |
| 2xxx | 认证授权 | 2001 | UNAUTHENTICATED | 401 | 未登录/未携带令牌 |
| | | 2002 | TOKEN_INVALID | 401 | 令牌无效（签名错误/格式错误） |
| | | 2003 | TOKEN_EXPIRED | 401 | 令牌过期 |
| | | 2004 | TOKEN_REVOKED | 401 | 令牌已吊销 |
| | | 2005 | ACCOUNT_SEALED | 403 | 账号已封存（离职） |
| | | 2006 | ACCOUNT_DISABLED | 403 | 账号停用/锁定 |
| | | 2007 | NO_FUNCTION_PERM | 403 | 无功能权限 |
| | | 2008 | OUT_OF_DATA_SCOPE | 403 | 越数据范围 |
| | | 2009 | SIGNATURE_INVALID | 401 | 对外接口签名校验失败 |
| | | 2010 | REPLAY_DETECTED | 401 | 防重放命中（timestamp/nonce） |
| | | 2011 | APP_DISABLED | 403 | 业务系统接入已停用 |
| 3xxx | 业务规则 | 3001 | ROLE_MUTEX_CONFLICT | 409 | 角色互斥冲突 |
| | | 3002 | ROLE_INHERIT_CYCLE | 409 | 角色继承成环 |
| | | 3003 | ORG_MOVE_INVALID | 409 | 组织节点移动非法（成环/跨级） |
| | | 3004 | USER_ALREADY_SEALED | 409 | 用户已封存 |
| | | 3005 | GRANT_DUPLICATE | 409 | 重复授权 |
| | | 3006 | APPROVAL_REQUIRED | 409 | 需审批流（未走审批） |
| | | 3007 | DATA_SCOPE_EXPIRED | 409 | 临时授权已过期 |
| | | 3008 | LOCK_ACQUIRE_FAILED | 409 | 分布式锁获取失败（并发冲突） |
| 4xxx | 限流 | 4001 | RATE_LIMITED | 429 | 触发限流（附 `Retry-After`） |
| 5xxx | 服务器 | 5000 | INTERNAL_ERROR | 500 | 服务器内部错误 |
| | | 5001 | SERVICE_UNAVAILABLE | 503 | 依赖服务不可用（可降级） |
| | | 5002 | DEGRADED | 200 | 决策服务降级（业务侧按短缓存处理） |

### 1.5 幂等与并发

- **幂等**：所有 `POST/PUT/PATCH/DELETE` 写接口支持 `Idempotency-Key`；服务端以 `idem:{app_key}:{key}` 存储首次响应（TTL 24h），重复请求体一致返回首次结果，不一致返回 `1004`。
- **分布式锁**：授权/回收/封存/角色互斥校验等写操作以 `lock:auth:{user_id}`、`lock:role:{role_id}`、`lock:org:{org_id}` 为键加 Redis 分布式锁（超时 10s，看门狗续期）；获取失败返回 `3008`。
- **乐观锁**：授权关系表、角色表带 `version` 字段，更新时 `WHERE version = ?`，冲突返回 `3008` 并提示重试。
- **事务性消息**：授权写库与变更事件发布采用发件箱（outbox）模式，保证数据与事件最终一致。

---

## 2. 认证接口（M01）

### 2.1 POST /auth/login

管理后台账号密码登录。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 登录名（工号或手机号） |
| password | string | 是 | 密码（前端 RSA 公钥加密传输） |
| captcha_id | string | 否 | 验证码 ID（连续失败 3 次后必填） |
| captcha_code | string | 否 | 验证码 |
| client_ip | string | 否 | 客户端 IP（服务端亦可从连接获取） |

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| access_token | string | 访问令牌（JWT，2h） |
| refresh_token | string | 刷新令牌（7d） |
| token_type | string | 固定 `Bearer` |
| expires_in | int | 访问令牌有效期（秒） |
| user_id | string | 用户 ID |
| display_name | string | 脱敏展示名（如 `张*`） |
| account_status | string | `normal`/`disabled`/`sealed`/`locked` |

### 2.2 POST /auth/logout

**请求头**：`Authorization: Bearer {access_token}`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| refresh_token | string | 否 | 一并吊销刷新令牌 |
| all_devices | boolean | 否 | 是否吊销该用户全部会话，默认 false |

**响应体 data**：`{ "revoked": true }`

### 2.3 POST /auth/refresh

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| refresh_token | string | 是 | 刷新令牌 |

**响应体 data**：同登录（新 `access_token` + 轮换后的 `refresh_token`）。

### 2.4 GET /auth/profile

**请求头**：`Authorization: Bearer {access_token}`

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | string | 用户 ID |
| username | string | 登录名 |
| display_name | string | 脱敏姓名 |
| phone_masked | string | 脱敏电话（如 `138****5678`） |
| org_id | string | 所属组织 ID |
| org_path | array | 组织路径（总公司→分公司→部门→项目经理部） |
| roles | array | `[{role_id, role_name, role_type}]` |
| account_status | string | 账号状态 |
| permission_version | int | 权限版本号（前端据此判断是否需刷新） |

### 2.5 SSO 接口

**GET /sso/authorize**

| 参数 | 位置 | 必填 | 说明 |
|---|---|---|---|
| system_code | query | 是 | 业务系统编码（如 `PMIS`） |
| redirect_uri | query | 是 | 回跳地址（须与登记一致） |
| state | query | 是 | 防 CSRF 随机串，原样回传 |

**响应**：302 重定向至 `redirect_uri?code={一次性授权码}&state={state}`；授权码有效期 60s，一次性使用。

**POST /sso/token**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| grant_type | string | 是 | 固定 `authorization_code` |
| code | string | 是 | 授权码 |
| system_code | string | 是 | 业务系统编码 |
| redirect_uri | string | 是 | 与授权请求一致 |

**响应体 data**：`{ access_token, token_type, expires_in, user_id, permission_version }`（业务系统据此建立本地会话，后续调用 `/open/*` 鉴权）。

---

## 3. 对外鉴权 OpenAPI（供 18 套业务系统调用，M08/M12）

> 所有 `/open/*` 接口须携带 `X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Signature`，并满足防重放要求（见 1.2）。

### 3.1 POST /open/token/verify

校验令牌有效性（对应结构化设计 4.2(1)）。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | string | 是 | 待校验令牌 |
| system_code | string | 是 | 业务系统编码 |
| client_ip | string | 否 | 客户端 IP（用于风控） |
| request_id | string | 是 | 业务侧请求 ID（防重放关联） |

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| valid | boolean | 是否有效 |
| user_id | string | 用户 ID（有效时返回） |
| expire_at | string | 令牌过期时间（ISO8601） |
| reason | string | 无效原因：`invalid`/`expired`/`revoked`/`account_sealed`/`account_disabled` |
| permission_version | int | 权限版本号（业务侧缓存比对用） |

### 3.2 POST /open/authorize

鉴权决策（对应结构化设计 4.2(3)、第 6 章 PDP 流程）。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| user_id | string | 是 | 用户 ID |
| system_code | string | 是 | 业务系统编码 |
| api_path | string | 是 | 被访问 API 路径（如 `/pmis/project/123`） |
| http_method | string | 是 | `GET`/`POST`/`PUT`/`DELETE` 等 |
| target_data | object | 否 | 数据目标：`{org_id, project_id, owner_id}` |
| token | string | 是 | 用户令牌 |
| request_id | string | 是 | 业务侧请求 ID |

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| allow | boolean | 是否允许 |
| deny_reason | string | 拒绝原因：`token_invalid`/`account_sealed`/`no_function_perm`/`out_of_data_scope`/`degraded` |
| decision_id | string | 决策 ID（用于审计关联与追溯） |
| matched_function_point_id | string | 命中的功能点 ID |
| required_operation | string | 所需操作（12 种取值之一） |
| permission_version | int | 权限版本号 |

### 3.3 GET /open/permissions

查询用户在某系统的功能权限 + 数据权限（前端菜单/按钮渲染用，对应结构化设计 4.2(2)）。

**查询参数**

| 参数 | 必填 | 说明 |
|---|---|---|
| user_id | 是 | 用户 ID |
| system_code | 是 | 业务系统编码 |
| module_code | 否 | 仅查某模块 |

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | string | 用户 ID |
| system_code | string | 系统编码 |
| account_status | string | 账号状态 |
| permission_version | int | 权限版本号 |
| function_permissions | array | `[{function_point_id, module_code, operation}]`，operation 取 12 种取值之一 |
| data_permissions | array | `[{scope_type, org_id, project_id, expire_at}]`，scope_type ∈ `all_company`/`branch`/`department`/`project`/`self` |

### 3.4 POST /open/event/callback

权限变更回调（平台 → 业务系统，订阅制，对应结构化设计 4.2(4)）。

**订阅方式**：业务系统在 `/open/apps` 登记时配置 `callback_url` 与 `subscribe_events`（如 `["grant_changed","revoked","sealed","role_changed"]`）。

**请求体（平台推送）**

| 字段 | 类型 | 说明 |
|---|---|---|
| event_id | string | 事件 ID（消费方幂等去重） |
| event_type | string | `grant_changed`/`revoked`/`sealed`/`role_changed` |
| affected_user_ids | array | 受影响用户 ID 列表 |
| affected_system_codes | array | 受影响系统编码列表 |
| change_snapshot | object | 变更前后快照（对应 FR-42） |
| occurred_at | string | 发生时间（ISO8601） |
| signature | string | 平台签名（业务系统验签） |

**响应体 data**：`{ "ack": true, "event_id": "..." }`；业务系统须在 5s 内返回，超时平台按指数退避重试（最多 5 次），消费方按 `event_id` 幂等。

### 3.5 POST /open/apps

业务系统接入登记（对应结构化设计 4.2(6)、M12）。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| system_code | string | 是 | 系统编码（唯一，如 `PMIS`） |
| system_name | string | 是 | 系统名称 |
| callback_url | string | 是 | 变更回调地址 |
| subscribe_events | array | 是 | 订阅事件类型列表 |
| public_key | string | 否 | 业务系统公钥（双向 TLS 或验签用） |
| contact | string | 否 | 联系人 |
| description | string | 否 | 描述 |

**响应体 data**

| 字段 | 类型 | 说明 |
|---|---|---|
| app_key | string | 平台分配的应用 Key |
| app_secret | string | 平台分配的应用密钥（仅首次返回，须安全保存） |
| system_code | string | 系统编码 |
| status | string | `active` |

---

## 4. 管理端接口（管理后台，M02~M07/M09/M12）

> 所有 `/admin/*` 接口须携带 `Authorization: Bearer {access_token}`；写接口均落审计日志（见第 6 章）。

### 4.1 组织机构（M03）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/orgs | 组织列表（分页） | `page`、`page_size`、`org_type`、`parent_id` |
| GET | /admin/orgs/tree | 组织树查询 | `root_id`、`depth`、`include_member_count` |
| POST | /admin/orgs | 新建组织节点 | `org_name`、`org_type`、`parent_id`、`leader_user_id` |
| GET | /admin/orgs/{org_id} | 组织详情 | — |
| PUT | /admin/orgs/{org_id} | 更新组织 | `org_name`、`leader_user_id`、`version` |
| DELETE | /admin/orgs/{org_id} | 删除组织（须无子节点与成员） | `version` |
| POST | /admin/orgs/{org_id}/move | 移动节点 | `new_parent_id`、`version`（校验成环/跨级） |

### 4.2 用户账号（M02）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/users | 用户列表（姓名/电话脱敏返回） | `page`、`page_size`、`org_id`、`account_status`、`keyword` |
| POST | /admin/users | 新建用户 | `username`、`real_name`、`phone`、`org_id`、`position` |
| GET | /admin/users/{user_id} | 用户详情（脱敏） | — |
| PUT | /admin/users/{user_id} | 更新用户 | `real_name`、`phone`、`org_id`、`version` |
| PATCH | /admin/users/{user_id}/status | 状态变更（停用/封存/锁定/启用） | `target_status`、`reason`、`version` |
| POST | /admin/users/import | 批量导入（Excel/CSV） | `file`、`dry_run` |
| GET | /admin/users/{user_id}/permission-overview | 用户权限总览（角色+功能+数据范围，姓名电话脱敏） | `system_code`（可选） |

### 4.3 角色（M04）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/roles | 角色列表 | `page`、`page_size`、`role_type`、`status` |
| POST | /admin/roles | 新建角色 | `role_name`、`role_type`（`function`/`data`）、`description` |
| GET | /admin/roles/{role_id} | 角色详情 | — |
| PUT | /admin/roles/{role_id} | 更新角色 | `role_name`、`description`、`version` |
| DELETE | /admin/roles/{role_id} | 删除角色（须无绑定） | `version` |
| GET | /admin/roles/tree | 角色继承树 | `root_id` |
| POST | /admin/roles/{role_id}/inherit | 配置继承关系 | `parent_role_id`（校验成环） |
| POST | /admin/roles/mutex | 配置角色互斥 | `role_id_a`、`role_id_b`、`reason` |

### 4.4 功能权限（M05/M12）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/systems | 业务系统列表 | `page`、`page_size`、`status` |
| POST | /admin/systems | 系统登记 | `system_code`、`system_name`、`callback_url` |
| GET | /admin/modules | 模块列表 | `system_code`、`page`、`page_size` |
| POST | /admin/modules | 新建模块 | `system_code`、`module_code`、`module_name`、`parent_id` |
| GET | /admin/function-points | 功能点列表 | `system_code`、`module_code`、`page`、`page_size` |
| POST | /admin/function-points | 新建功能点 | `module_code`、`function_point_code`、`function_point_name`、`api_path`、`http_method`、`operation_set` |
| PUT | /admin/function-points/{id} | 更新功能点（含 API 映射） | `api_path`、`http_method`、`operation_set`、`version` |
| GET | /admin/permission-matrix | 权限矩阵查询 | `role_id`、`system_code`、`module_code` |
| GET | /admin/permission-matrix/export | 权限矩阵导出 | 同上，返回 Excel |

### 4.5 数据权限（M06）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/data-scopes | 数据范围字典 | `scope_type` |
| POST | /admin/role-data-scopes | 角色-数据范围授权 | `role_id`、`scope_type`、`org_id`、`project_id`、`expire_at`（临时授权有效期） |
| GET | /admin/role-data-scopes | 查询角色数据范围授权 | `role_id`、`include_expired` |
| DELETE | /admin/role-data-scopes/{id} | 回收数据范围授权 | `version` |

### 4.6 授权管理（M07）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| POST | /admin/user-roles | 用户-角色授予（走审批流） | `user_id`、`role_ids`、`reason`、`effective_at`、`expire_at` |
| DELETE | /admin/user-roles | 用户-角色回收 | `user_id`、`role_ids`、`reason` |
| GET | /admin/user-roles | 查询用户角色绑定 | `user_id`、`include_history` |
| GET | /admin/grant-snapshots | 授权变更快照查询 | `user_id`、`start_time`、`end_time`、`change_type` |
| POST | /admin/grants/{grant_id}/approve | 审批通过 | `approver_id`、`comment` |
| POST | /admin/grants/{grant_id}/reject | 审批驳回 | `approver_id`、`comment` |

### 4.7 审计日志（M09）

| 方法 | 路径 | 说明 | 关键参数 |
|---|---|---|---|
| GET | /admin/audit-logs | 日志查询 | `start_time`、`end_time`、`username`、`operation_type`、`success`、`page`、`page_size` |
| GET | /admin/audit-logs/export | 日志导出（导出操作自身落审计） | 同上 + `format`（`csv`/`xlsx`） |

---

## 5. 典型接口示例

### 5.1 登录

**请求**

```http
POST /api/v1/auth/login
Content-Type: application/json
X-Request-Id: 7f3a1c2e-9b4d-4e8a-a1b2-c3d4e5f60718
Idempotency-Key: 0a1b2c3d-4e5f-6789-abcd-ef0123456789

{
  "username": "zhangsan",
  "password": "RSA_ENCRYPTED_BASE64...",
  "client_ip": "10.20.30.40"
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "rt_9f8e7d6c5b4a...",
    "token_type": "Bearer",
    "expires_in": 7200,
    "user_id": "U10086",
    "display_name": "张*",
    "account_status": "normal"
  },
  "request_id": "7f3a1c2e-9b4d-4e8a-a1b2-c3d4e5f60718",
  "timestamp": "2025-06-01T09:30:00+08:00"
}
```

### 5.2 对外鉴权决策

**请求**

```http
POST /api/v1/open/authorize
Content-Type: application/json
X-App-Key: PMIS_APP_KEY
X-Timestamp: 1748731800
X-Nonce: 5f2e1d0c-9b8a-7f6e-5d4c-3b2a1f0e9d8c
X-Signature: HMAC_SHA256_HEX...
X-Request-Id: 2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f

{
  "user_id": "U10086",
  "system_code": "PMIS",
  "api_path": "/pmis/project/123/approve",
  "http_method": "POST",
  "target_data": { "org_id": "ORG_BRIDGE_01", "project_id": "P2025001", "owner_id": "U10086" },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "request_id": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f"
}
```

**响应（允许）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "allow": true,
    "deny_reason": null,
    "decision_id": "DEC_20250601_0001",
    "matched_function_point_id": "FP_PMIS_PROJECT_APPROVE",
    "required_operation": "查看/审批",
    "permission_version": 1024
  },
  "request_id": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "timestamp": "2025-06-01T09:31:00+08:00"
}
```

**响应（拒绝-越数据范围）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "allow": false,
    "deny_reason": "out_of_data_scope",
    "decision_id": "DEC_20250601_0002",
    "matched_function_point_id": "FP_PMIS_PROJECT_APPROVE",
    "required_operation": "查看/审批",
    "permission_version": 1024
  },
  "request_id": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "timestamp": "2025-06-01T09:31:00+08:00"
}
```

### 5.3 用户授权角色

**请求**

```http
POST /api/v1/admin/user-roles
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json
Idempotency-Key: 1a2b3c4d-5e6f-7890-abcd-ef1234567890

{
  "user_id": "U10086",
  "role_ids": ["R_SAFETY_OFFICER", "R_PROJECT_MANAGER"],
  "reason": "调岗至桥梁工程分公司第一项目经理部任项目经理",
  "effective_at": "2025-06-01T00:00:00+08:00",
  "expire_at": null
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "grant_id": "G_20250601_0001",
    "user_id": "U10086",
    "role_ids": ["R_SAFETY_OFFICER", "R_PROJECT_MANAGER"],
    "status": "pending_approval",
    "approval_flow_id": "AF_20250601_0001",
    "permission_version": 1025
  },
  "request_id": "3d4e5f6a-7b8c-9d0e-1f2a-3b4c5d6e7f80",
  "timestamp": "2025-06-01T10:00:00+08:00"
}
```

**响应（角色互斥冲突）**

```json
{
  "code": 3001,
  "message": "角色互斥冲突：R_SAFETY_OFFICER 与 R_QUALITY_INSPECTOR 不可同时授予",
  "data": { "conflict_role_ids": ["R_SAFETY_OFFICER", "R_QUALITY_INSPECTOR"] },
  "request_id": "3d4e5f6a-7b8c-9d0e-1f2a-3b4c5d6e7f80",
  "timestamp": "2025-06-01T10:00:00+08:00"
}
```

### 5.4 离职封存

**请求**

```http
PATCH /api/v1/admin/users/U10086/status
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json
Idempotency-Key: 4e5f6a7b-8c9d-0e1f-2a3b-4c5d6e7f8091

{
  "target_status": "sealed",
  "reason": "员工离职（HR 事件 HR20250601-001）",
  "version": 7
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "user_id": "U10086",
    "account_status": "sealed",
    "sealed_at": "2025-06-01T11:00:00+08:00",
    "revoked_tokens": 3,
    "unbound_role_ids": ["R_SAFETY_OFFICER", "R_PROJECT_MANAGER"],
    "unbound_data_scope_ids": ["DS_20250101_0001"],
    "permission_version": 1026,
    "audit_log_id": "AL_20250601_0001"
  },
  "request_id": "5f6a7b8c-9d0e-1f2a-3b4c-5d6e7f809102",
  "timestamp": "2025-06-01T11:00:00+08:00"
}
```

> 说明：封存保留账号与日志，不做物理删除；解绑业务角色与数据权限；吊销全部令牌；发布 `sealed` 变更事件。

### 5.5 查询审计日志

**请求**

```http
GET /api/v1/admin/audit-logs?start_time=2025-06-01T00:00:00%2B08:00&end_time=2025-06-01T23:59:59%2B08:00&username=zhangsan&operation_type=grant&success=true&page=1&page_size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "page_size": 20,
    "list": [
      {
        "log_id": "AL_20250601_0001",
        "operation_time": "2025-06-01T10:00:00+08:00",
        "operation_content": "授予用户 U10086 角色 R_SAFETY_OFFICER、R_PROJECT_MANAGER",
        "username": "zhangsan",
        "success": true,
        "client_ip": "10.20.30.40",
        "request_id": "3d4e5f6a-7b8c-9d0e-1f2a-3b4c5d6e7f80",
        "detail": { "grant_id": "G_20250601_0001", "role_ids": ["R_SAFETY_OFFICER", "R_PROJECT_MANAGER"] }
      },
      {
        "log_id": "AL_20250601_0002",
        "operation_time": "2025-06-01T10:05:12+08:00",
        "operation_content": "查询审计日志（关键词=权限变更，时间范围=2025-06-01）",
        "username": "lisi",
        "success": true,
        "client_ip": "10.20.31.45",
        "request_id": "7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
        "detail": { "keyword": "权限变更" }
      }
    ]
  }
}
```

---

## 第6章 安全与限流

### 6.1 接口鉴权处理顺序

统一权限平台作为公司级 IAM，对 18 套业务系统的每一次受保护 API 调用，均在后端执行**串行、短路**的四级校验。任一级失败立即返回错误并终止后续校验，不泄露后续校验信息。

| 顺序 | 校验环节 | 校验内容 | 失败返回 |
|---|---|---|---|
| ① | 令牌校验 | SSO 令牌/接口鉴权令牌的签名、有效期、签发方、是否在吊销名单 | 401 未认证 |
| ② | 账号状态 | 账号是否存在、是否被封存（离职禁用）、是否锁定 | 403 账号不可用 |
| ③ | 功能权限 | 该账号对目标系统/菜单/按钮/API 是否具备功能权限（RBAC0 角色—权限映射） | 403 无功能权限 |
| ④ | 数据范围 | 请求中的数据范围（分公司/部门/项目/本人）是否落在该账号被授予的数据权限内 | 403 越权访问 |

设计要点：

- **短路拒绝**：①失败不再校验②③④，避免对无效令牌做多余查询，也避免通过错误码差异探测系统内部结构。
- **统一错误码**：对外仅暴露 401/403 两类语义，详细原因仅写入审计日志，不返回给调用方。
- **校验前置**：鉴权在业务逻辑执行前完成，业务代码不得自行绕过；平台以统一网关/拦截器方式强制注入，业务系统无"跳过鉴权"开关。
- **数据范围二次校验**：功能权限通过后，仍需对请求参数中的组织/项目标识做数据范围比对，防止"有功能权限但越范围取数"。

### 6.2 垂直/水平越权防护

**垂直越权（低权限角色调用高权限接口）防护：**

- 功能权限以"系统—菜单—按钮/API"三级资源为粒度授予，接口与权限点一一绑定，不做"角色名硬编码判断"。
- 所有写接口与敏感查询接口在服务端强制校验权限点，前端菜单/按钮的 enable/disable 仅作体验优化，不作为安全边界。
- 系统管理员权限与业务权限分离，系统管理员不默认拥有业务数据查看权，避免"运维即全权"。
- 高危操作（授权变更、角色调整、账号封存）单独设权限点，并强制二次确认与审计。

**水平越权（同角色访问他人/他部门数据）防护：**

- 数据权限维度包括：分公司、部门、项目经理部、项目、本人。请求中的组织/项目标识必须与账号被授予的数据范围求交，越界即拒绝。
- "本人"类权限（查看本人、查看/编辑本人）在服务端以当前登录账号身份回填过滤条件，**不接受前端传入的用户标识**，防止伪造他人 ID。
- 列表查询与详情查询使用同一套数据范围过滤逻辑，避免"列表过滤、详情漏过滤"的常见漏洞。
- 安全员、预算员、质检员等岗位的数据可见范围差异，通过数据权限配置实现，不写死在代码中。

### 6.3 限流策略

平台按三个维度限流，超限返回 429 并记录审计日志。阈值建议如下（可按实际压测调整）：

| 维度 | 限流对象 | 建议阈值 | 说明 |
|---|---|---|---|
| 按用户 | 单账号 | 查询类 20 次/秒；写操作 5 次/秒 | 防止单账号脚本刷接口 |
| 按 IP | 单一来源 IP | 100 次/秒 | 防止单点异常流量；NAT 出口可适当放宽 |
| 按调用方业务系统 | 单个业务系统（client_id） | 500 次/秒（可配置） | 保护平台自身，防止某套系统异常拖垮全局 |

补充规则：

- 令牌申请（登录/换取令牌）接口单独限流，按 IP + 账号双维度，防止暴力破解。
- 限流采用令牌桶/滑动窗口，阈值支持配置化，无需重启生效。
- 触发限流时返回统一错误码与 `Retry-After` 提示，不暴露内部限流实现细节。

### 6.4 审计要求

**每个写接口（新增、修改、删除、授权、封存、解绑等）必须落审计日志，且失败操作同样记录。** 审计日志永久保存，不提供删除入口。

审计日志必备字段：

| 字段 | 说明 |
|---|---|
| log_id | 日志唯一标识 |
| oper_time | 操作时间（精确到毫秒，统一时区） |
| oper_user | 操作账号 |
| oper_user_name | 操作人姓名（脱敏展示） |
| oper_action | 操作动作（新增/修改/删除/授权/查询/登录等） |
| oper_content | 操作内容描述（含关键参数，敏感字段脱敏） |
| target_system | 目标业务系统 |
| target_resource | 目标资源/接口 |
| result | 成功与否（SUCCESS/FAIL） |
| fail_reason | 失败原因（仅失败时记录，含鉴权失败级别） |
| client_ip | 来源 IP |
| duration_ms | 处理耗时 |
| trace_id | 链路追踪标识，便于跨系统关联 |

要求：

- 审计写入与业务操作在同一事务边界内保证"操作发生即留痕"，审计失败不得导致业务静默成功。
- 日志中的姓名、电话号码等敏感个人信息按脱敏规则存储与展示，原始值加密存储。
- 审计日志支持按时间、账号、系统、结果等条件查询，查询行为本身亦记录审计。

### 6.5 结语

本章明确了统一权限平台"先认证、再鉴权、后取数"的串行短路校验模型，从垂直与水平两个方向封堵越权风险，并以多维度限流与全量审计构成纵深防御。结合前文的 RBAC0 建模、双维度权限与人员生命周期管理，平台可在不重写各接入业务系统的前提下，为其提供一致、可审计、可追溯的统一身份与权限底座，满足万级人员规模下的安全合规与运维要求。
