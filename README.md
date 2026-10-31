# 统一应用软件权限管理系统（IAM）

基于 RBAC 模型的企业统一身份与**功能权限**管理平台。系统集中管理账号、组织、角色、功能点与角色授权，并通过 OpenAPI 为接入系统提供功能权限查询和判定。

## 项目边界

本项目**只实现功能权限，不实现数据权限**。

- “能访问哪个菜单、页面、按钮和 API”由功能角色、功能点及 `allowed_action` 决定。
- “能查看哪个分公司、部门、项目或本人的数据”不在本项目范围内。
- 组织机构仍作为账号归属和人员管理的主数据，不承担数据权限判定。
- 角色只有功能角色一种类型，不提供数据角色或数据范围授权。

明确不包含的能力包括：数据范围、ABAC、审批流、SSO、刷新令牌、权限变更通知、审计导出。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.4.5 |
| 持久层 | Spring Data JPA、MySQL 8.0 |
| 缓存与幂等 | Redis、Spring Data Redis |
| 安全 | JWT、PBKDF2、AES-256-GCM、HMAC-SHA256 |
| 数据源 | Druid |
| 校验与工具 | Spring Validation、Lombok |
| 构建 | Maven |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus |

## 已实现能力

### 身份认证与账号

- 账号密码登录、令牌签发、登出和令牌校验。
- 自助修改密码。
- 用户创建、详情、分页查询、状态变更与离职封存。
- 封存时回收用户全部角色并吊销令牌。

### 组织与角色

- 组织机构树、详情、子节点、新建和节点移动。
- 功能角色创建、详情和列表。
- 角色继承链在权限判定时生效。
- 角色互斥配置与授予前冲突校验。

### 功能权限

- 子系统、模块和功能点登记。
- 角色与功能点批量绑定、解绑和查询。
- 功能点操作动作校验，判定时要求 `allowed_action` 覆盖功能点声明的动作。
- 系统、模块、功能点和角色状态参与判定。
- 单点、批量和权限集合查询。

### 授权与安全

- 用户角色授予、撤销和查询，支持授权有效期。
- 管理员不能授予超出自身功能权限的角色。
- 未登记的管理端 API 默认拒绝（fail-closed）。
- OpenAPI 支持接入方 HMAC 签名、时间戳、nonce 防重放；普通用户令牌只能查询本人。
- 关键请求参数校验、分页上限、批量上限、唯一约束和乐观锁。
- 写请求支持 `Idempotency-Key`，请求指纹包含调用者、参数和请求体。
- 支持 `X-Request-Id` 回写、统一响应 `requestId` 和日志关联。

### 审计与缓存

- 权限变更写操作记录审计日志，操作人取自服务端认证上下文。
- 审计日志分页查询。
- 权限缓存使用版本号失效，不使用 Redis `KEYS` 扫描。
- 分布式锁使用 owner token 与原子解锁。

## 架构

```text
┌─────────────────────────────────────────────────┐
│ web：Vue 管理后台                               │
├─────────────────────────────────────────────────┤
│ interfaces：REST Controller / Filter / Advice   │
├─────────────────────────────────────────────────┤
│ application：认证、账号、组织、角色、授权、PDP │
├─────────────────────────────────────────────────┤
│ infrastructure：JPA、Redis、Crypto、Cache       │
└─────────────────────────────────────────────────┘
```

核心目录：

```text
RBAC-Structured/
├── src/main/java/com/iam/platform/
│   ├── interfaces/        REST 接口、认证鉴权、请求追踪与幂等
│   ├── application/       业务服务与功能权限决策
│   ├── common/            统一响应、异常、枚举
│   └── infrastructure/    JPA、Redis、缓存、加解密
├── src/main/resources/db/ schema.sql、seed.sql
├── src/test/java/         后端测试
├── web/                   Vue 管理后台
└── design/                详细设计与接口设计材料
```

数据库当前包含 12 张表，核心模型为：

```text
用户 -> 用户-角色 -> 角色 -> 角色-功能点 -> 功能点 <- 模块 <- 子系统
```

组织树、角色互斥、令牌、审计日志和权限变更日志为辅助模型。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6+
- Node.js 20+、pnpm

### 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE iam_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p iam_platform < src/main/resources/db/schema.sql
mysql -u root -p iam_platform < src/main/resources/db/seed.sql
```

初始化管理员为 `admin / Admin@123`。该密码仅用于本地初始化，部署前必须修改。

MySQL 建议设置 `default-time-zone = '+08:00'`；`seed.sql` 已在会话级设置北京时间。

### 关键环境变量

| 变量 | 默认值或用途 |
| --- | --- |
| `DB_HOST` / `DB_PORT` | MySQL 地址与端口 |
| `DB_NAME` | 默认 `iam_platform` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号与密码 |
| `DB_SSL` | MySQL SSL 开关，默认 `false` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址与端口 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | JWT 密钥 |
| `AES_KEY` | 敏感字段加密密钥 |
| `HMAC_KEY` | 检索哈希与签名密钥 |

生产环境必须通过密钥或环境变量注入，不得沿用仓库中的开发默认值。

### 后端

```bash
mvn clean test
mvn clean package -DskipTests
java -jar target/iam-platform-1.0.0-SNAPSHOT.jar
```

后端默认地址：`http://localhost:8080/api/v1`。

### 前端

```bash
cd web
pnpm install
pnpm dev
pnpm typecheck
pnpm build
```

前端通过 Vite 代理访问后端；Mock 开关和代理配置见 `web/README.md`。

## 鉴权规则

- `POST /auth/login` 为公开接口。
- 管理端请求携带 `Authorization: Bearer <token>`。
- 管理端 API 必须已登记为功能点，否则默认拒绝。
- OpenAPI 可携带 `X-App-Id`、`X-Timestamp`、`X-Nonce`、`X-Signature` 进行接入方认证。
- OpenAPI 普通用户令牌只能查询本人；接入方签名可查询授权范围内的用户。
- 只有功能权限判定，不接受目标组织、项目或资源上下文作为数据权限输入。

## API 概览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/logout` | 登出 |
| GET | `/api/v1/auth/validate` | 校验令牌 |
| PUT | `/api/v1/auth/password` | 自助修改密码 |
| GET/POST | `/api/v1/users` | 用户分页查询 / 创建 |
| GET | `/api/v1/users/{userId}` | 用户详情 |
| PUT | `/api/v1/users/{userId}/status` | 变更账号状态 |
| GET/POST | `/api/v1/orgs` | 组织树 / 新建组织 |
| GET | `/api/v1/orgs/{orgId}` | 组织详情 |
| GET | `/api/v1/orgs/{orgId}/children` | 下级组织 |
| PUT | `/api/v1/orgs/{orgId}/move` | 移动组织 |
| GET/POST | `/api/v1/roles` | 角色列表 / 新建功能角色 |
| GET | `/api/v1/roles/{roleId}` | 角色详情 |
| POST/GET | `/api/v1/roles/{roleId}/exclusions` | 配置 / 查询角色互斥 |
| POST/DELETE | `/api/v1/roles/grants` | 授予 / 撤销用户角色 |
| GET | `/api/v1/roles/grants/{userId}` | 查询用户角色 |
| GET/POST | `/api/v1/systems` | 子系统列表 / 登记 |
| POST | `/api/v1/systems/modules` | 登记模块 |
| POST | `/api/v1/systems/function-points` | 登记功能点 |
| POST/DELETE | `/api/v1/systems/role-functions` | 绑定 / 解绑角色功能点 |
| GET | `/api/v1/systems/role-functions/{roleId}` | 查询角色功能点 |
| GET | `/api/v1/open/authorize` | 单点功能权限判定 |
| POST | `/api/v1/open/authorize/batch` | 批量功能权限判定 |
| GET | `/api/v1/open/permissions` | 查询用户功能点集合 |
| GET | `/api/v1/audit-logs` | 审计日志分页查询 |

## 验证状态

当前后端共 97 项测试，全部通过；前端 `pnpm typecheck`、`pnpm build` 和 `pnpm lint` 均通过。测试覆盖认证、账号、组织、角色继承与互斥、功能权限判定、OpenAPI、审计、输入校验、并发冲突、请求追踪和幂等。

`design/` 目录保留课程详细设计材料；功能范围以本 README 和当前代码为准。
