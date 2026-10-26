# RBAC 后端 API 使用手册

**版本：1.1 · 代码基准：8d78558 · 编写与检查日期：2026-10-26（北京时间）**

> 1.1 修订：组织机构 A24～A28 已实现，按实际行为重写；初始化接口功能点扩展为 20001～20022；`seed.sql` 新增两个组织节点并设置会话时区。其余接口内容沿用 1.0。

本手册用于实际构造请求、处理响应、对接前端或接入业务系统。它记录当前代码真正提供的行为，不将设计目标、模拟接口或尚未实现的方法写成可用能力。

| 项目 | 基准 |
|---|---|
| 仓库 | xmu-work/RBAC-Structured |
| 源码位置 | 当前仓库根目录；本手册与 README.md 并列 |
| 分支与完整提交 | `main` · `8d78558`（Merge pull request #9） |
| 业务入口覆盖 | 7 个控制器、34 个显式接口：28 个已实现、6 个待实现 |
| 认证方式 | 访问令牌；当前无刷新令牌接口、无对外应用签名校验 |
| 功能范围 | 基础用户—角色—功能点模型与组织树；尚无角色继承、互斥及数据范围决策 |
| 资料优先级 | 当前方法实现与测试 > 当前方法签名 > 设计说明；发生冲突时本手册解释实际行为 |

**适用边界：**“已实现”指存在业务实现，仍依赖正确的数据库、初始化数据与运行配置。6 个“待实现”接口只保留调用签名以便识别，当前不能作为可用功能接入。本文不承诺 Spring Boot 自动提供的健康检查、错误处理等框架入口的业务契约。

下方提供目录和每个接口的稳定编号 A01～A34。浏览器版本可搜索路径、参数名或错误码；Markdown 版本可作为后续维护源文件。

可按第 4 章的 A01～A34 索引跳转到具体接口，也可使用文档页面的大纲导航。

## 1. 接入前准备与快速开始

### 1.1 地址与运行前提

默认地址：

```text
http://localhost:8080/api/v1
```

`server.port=8080`，`server.servlet.context-path=/api/v1`。后文 `/users` 等是相对路径，完整地址是 `/api/v1/users`。没有额外的 `/admin` 段，也不要拼成 `/api/v1/api/v1/users`。

在执行示例之前，应由项目维护者启动后端并准备好 `iam_platform` 数据库，先执行 `schema.sql`，再执行 `seed.sql`。本手册不会自动执行建表或初始化。数据库初始化脚本会创建 ADMIN 系统、超级管理员、菜单与当前管理接口权限，以及“总公司”（主键 1，根节点）和其下的“信息中心”（主键 2）两个组织节点；它没有导入完整组织树或所有业务系统。脚本开头设置会话时区 `+08:00`，即使 MySQL 服务器为 UTC，初始化数据的时间字段也是北京时间。

`seed.sql` 中的初始管理员是 `admin / Admin@123`；仅适用于按该脚本初始化且尚未变更账号密码的环境。初始姓名密文与当前加密密钥配套，不能在更换密钥后直接假定原有密文仍可解密。

默认后端没有配置浏览器跨域放行。仓库前端开发服务通过代理转发 `/api/v1` 到后端；其他网页客户端应采用已配置的同源或代理方案。命令行调用不受浏览器跨域机制约束。

### 1.2 获取并使用访问令牌

以下命令使用 bash/zsh 与 curl。所有示例均为**供调用者按需执行**的文本，本次编写没有对真实业务数据执行这些写请求。

```bash
BASE_URL='http://localhost:8080/api/v1'

curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  --data '{"username":"admin","password":"Admin@123"}'
```

登录成功后，取 `data.accessToken` 设置到变量；不要把尖括号或整个 JSON 响应一起复制到令牌中。

```bash
ACCESS_TOKEN='替换为登录响应中的实际访问令牌'

curl -sS "$BASE_URL/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

后文示例中的标识也需替换为实际值：

```bash
TARGET_USER_ID=2
ROLE_ID=2
SYSTEM_ID=2
MODULE_ID=3
FUNCTION_POINT_ID=5001
```

这些数字仅用来展示参数位置，不代表数据库一定存在这些记录。创建返回的 `id` 是后续绑定、授权和查询的依据；系统数字 `id` 与文本 `systemCode` 不能混用。

### 1.3 三种必须区分的请求形态

| 类别 | 对应入口 | 正确传法 |
|---|---|---|
| JSON 对象请求体 | 登录；批量功能鉴权 | `Content-Type: application/json`，在请求体提供对象 |
| 查询参数请求 | 大部分用户、角色、系统、模块、功能点及授权写操作；各查询入口 | 参数放 URL 查询串；不能把所有参数统一包成 JSON |
| 查询参数 + JSON 裸数组 | 角色功能点绑定与解绑 | roleId 等放 query，主键数组放 JSON body，例如 `[5001,5002]` |

本手册用 `curl -G -X POST/PUT/DELETE` 加 `--data-urlencode` 将参数安全编码到查询串，`-X` 保留目标方法。不要只保留 `-G` 导致写请求变为 GET。POST 的标准表单参数也可能被框架读取，但统一使用 query 可避免 PUT/DELETE 表单解析差异。

## 2. 通用请求、响应与类型约定

### 2.1 请求规则

- 路径参数替换为实际主键，例如 `/users/2`，不发送字面量 `{userId}`。
- 中文、空格、日期、原因等查询值应做 URL 编码；`--data-urlencode` 已处理编码。
- 必需参数是现有签名或业务代码的要求。多数入口没有统一的非空、格式、长度与正数校验，**必需不等于自动满足所有业务规则**。
- 未明确支持的字段不会因被发送就自动生效，例如创建角色的 `parentId`、回收角色的 `reason`、用户列表的 `keyword`。
- 枚举码使用本手册列出的精确大写形式。服务调用枚举校验时不自动去空格或转换大小写。
- 请求标识 `X-Request-Id`、重复提交标识 `Idempotency-Key` 当前没有后端完整处理流程；不能依赖它们做关联回传或重复请求去重。
- 对写接口超时后，不应假定服务端没有成功；可先查询记录确认，再根据各接口的重复调用规则决定是否重试。

### 2.2 统一响应外层

正常返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": true,
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

| 字段 | 类型 | 当前规则 |
|---|---|---|
| code | int32 | 0 表示成功；非 0 表示业务或系统错误。应结合 HTTP 状态判断，不要仅凭 HTTP 200 判断所有业务成功 |
| message | string | 成功通常为 success；错误可能由实际代码补充对象标识等信息，不宜依赖完整文案做逻辑分支 |
| data | 任意结构，可缺失 | 各接口的实际业务结果；无数据成功和失败响应通常省略，不保证总有 `data:null` |
| requestId | string，可缺失 | 字段已定义，但当前没有赋值流程，通常被省略；不存在 `request_id` 的自动改名 |
| timestamp | 本地日期时间字符串 | 由 `LocalDateTime.now()` 产生，带 T，可能带小数秒，不带时区后缀 |

没有业务数据的成功响应仍然是 HTTP 200 + JSON：

```json
{
  "code": 0,
  "message": "success",
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

典型失败响应：

```json
{
  "code": 2007,
  "message": "无功能权限: 查询用户列表",
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

**字段命名为 camelCase。** 用户分页是 `pageSize`，请求标识字段是 `requestId`；不是旧文档或代码注释里的下划线形式。最外层的空值省略不会自动作用于嵌套对象，例如 `refreshToken:null`、`phoneMasked:null` 仍可能出现在 data 里。

### 2.3 数字、时间与空值

| 内容 | 实际约定 |
|---|---|
| 主键和外键 | 多数为 int64 的 JSON 数字；登录返回的 `userId` 是唯一已明确使用字符串的用户标识返回字段 |
| 0/1 标志 | `isDeleted`、审计声明中的 `success` 等是数字，不是布尔值 |
| 功能判定 | 返回真正的 boolean；与 0/1 标志区分 |
| 时间响应 | `2026-10-26T14:00:00` 或带小数秒；不假定固定 3 位或 6 位小数 |
| 时间参数 | 推荐 `2027-12-31T23:59:59`；现有验证确认 T 格式可用，空格分隔格式会返回 400/1001；未承诺所有带时区写法的转换行为 |
| 服务器时钟 | 当前配置意图为 Asia/Shanghai，但 `LocalDateTime.now()` 依赖部署进程的时区；调用方与运维需保持约定一致，不从无偏移字符串推断其他时区 |
| 可选时间 | 省略表示未指定；例如不传授权 expireTime 表示长期，不发送字符串 `null` |
| 返回集合 | Set 会序列化为数组并去重，但顺序不保证；对象数组也没有统一排序保证 |

数据库字段长度属于存储约束，并非每个入口都主动验证。超长输入或唯一约束冲突可能落到 5000，不能把所有这类情况都假定成 400 或 3009。姓名和电话存储为密文，不能简单把密文列长度当作明文允许长度。

### 2.4 错误发生的先后顺序

对于已存在、由控制器处理的业务路径，通常依次为：

```text
令牌认证 → 已登记接口的功能权限检查 → 参数转换／请求体绑定 → 业务处理 → 返回封装
```

因此，同一请求即使存在缺参数或“待实现”问题，也可能先得到 401 或 403。本文写的某个业务错误，均以请求自身已经通过前置认证和权限检查为前提。

## 3. 认证、功能权限与调用边界

### 3.1 Bearer 令牌

除 `POST /auth/login` 外，34 个业务入口中的其余接口均经过认证，包括 `/auth/validate`、`/auth/logout` 和全部 `/open/*`。

```text
Authorization: Bearer <accessToken>
```

当前解析精确要求 `Bearer ` 前缀及非空令牌；`bearer`、单独 token 查询参数、Cookie 或未加前缀的令牌都不是已实现的替代传法。

令牌认证验证签名、签发者、令牌自身过期时间、数据库中的令牌摘要记录及其 revoked 标志。当前不是只验签就能通过的无状态令牌：即使签名合法，数据库没有相应记录仍为 2002。

账号状态 API 在目标状态为 DISABLED、LOCKED 或 SEALED 时，会主动吊销目标用户的全部有效令牌；目标为 NORMAL 时不会吊销。认证过程本身没有每次重新读取用户状态。过期或吊销之后没有刷新端点，调用方需重新登录。登出之后继续用旧令牌调用不会成功。

### 3.2 菜单权限与接口权限

当前拦截器用**控制器路径模板 + 请求方法**查 `iam_function_point`，例如 `GET /users/{userId}`，再判断操作人在 ADMIN 系统中是否拥有这个功能点。

- 有映射：无该功能点时返回 403/2007。
- 没有映射：当前实现仅要求令牌有效，不执行对应的功能权限判定。此规则是现状，不是“该入口永远不需要权限”的承诺。
- 路径模板不包含 `/api/v1`；路径占位符名称要与控制器映射一致，`/users/{id}` 与当前 `/users/{userId}` 不能随意替换。
- 管理后台始终按 ADMIN 权限判定。接口映射登记在其他系统下也不会自动让拦截器改查那个系统；不要用其他系统登记去替代 ADMIN 自身的接口授权。
- 初始超级管理员通过角色绑定获得权限，没有代码硬编码的“管理员跳过鉴权”特权。

`seed.sql` 的菜单功能点为 10001～10008，接口功能点为 20001～20022（其中 20018～20022 为组织接口）。拥有“用户管理”菜单 10003，并不自动拥有“创建用户”接口 20002；需要分别授权。完整接口标识见第 4 章。

### 3.3 权限算法与调用边界

`DecisionService` 当前按以下过程汇总目标用户在某系统中的功能点：

1. 用户存在、isDeleted=0 且 accountStatus=NORMAL。
2. 用户角色关系 status=ACTIVE，effectiveTime 为空或不晚于当前时间，expireTime 为空或严格晚于当前时间。
3. 关系对应的角色存在、isDeleted=0 且 status=ACTIVE。
4. 收集这些角色的功能点关系。
5. 依据 systemCode 找系统，收集属于该系统的模块，再保留相应模块下 isDeleted=0 且 status=ACTIVE 的功能点。

当前实现**没有**额外按系统／模块的 status 或 isDeleted 过滤，也没有解释角色类型、父角色继承、功能点 action 或绑定 allowedAction 的细分动作语义。仅把功能点绑定为 `NONE` 不会自动产生拒绝；需要回收用户角色、解绑角色功能点或修改实际状态。

对外鉴权接口的 `userId` 是待判定用户，不强制等于调用令牌的用户，也没有当前应用专属密钥、签名、时间戳或防重放验证。它返回的是“该目标用户是否有某功能点”，并不代替业务系统验证请求人身份或数据对象归属。`grantBy`、`createdBy` 同样是请求参数，不会自动绑定为当前操作人。

当前权限查询实时访问数据库，权限缓存工具没有接入这条决策流程；角色回收影响下一次判定。到期只影响计算结果，不会自动把关系的数据库状态从 ACTIVE 改成 EXPIRED。数据权限、组织范围和项目数据过滤仍未实现。

## 4. 完整接口索引

以下路径均相对于 `$BASE_URL`。权限标识来自当前 `seed.sql`；“仅令牌”表示初始化数据未给该入口登记功能点，不表示代码强制其永远免功能权限。所有已实现接口正常成功均为 HTTP 200，不使用 201 或 204。

| 编号 | 方法与路径 | 用途 | 当前状态 | 初始化接口权限 |
| --- | --- | --- | --- | --- |
| [A01](#a01) | `POST /auth/login` | 账号密码登录 | 已实现 | 免令牌 |
| [A02](#a02) | `POST /auth/logout` | 登出并吊销当前令牌 | 已实现 | 仅令牌 |
| [A03](#a03) | `GET /auth/validate` | 校验当前令牌 | 已实现 | 仅令牌 |
| [A04](#a04) | `POST /users` | 创建用户 | 已实现 | 20002 |
| [A05](#a05) | `GET /users` | 分页查询用户 | 已实现 | 20001 |
| [A06](#a06) | `GET /users/{userId}` | 查询用户详情 | 已实现 | 20003 |
| [A07](#a07) | `PUT /users/{userId}/status` | 变更账号状态 | 已实现 | 20004 |
| [A08](#a08) | `POST /roles` | 创建角色 | 已实现 | 20006 |
| [A09](#a09) | `GET /roles` | 查询角色列表 | 已实现 | 20005 |
| [A10](#a10) | `GET /roles/{roleId}` | 查询角色详情 | 已实现 | 20007 |
| [A11](#a11) | `POST /roles/grants` | 向用户授予单个角色 | 已实现 | 20008 |
| [A12](#a12) | `DELETE /roles/grants` | 回收用户的单个角色 | 已实现 | 20009 |
| [A13](#a13) | `GET /roles/grants/{userId}` | 查询用户角色关系记录 | 已实现 | 20010 |
| [A14](#a14) | `POST /systems` | 登记接入系统 | 已实现 | 20012 |
| [A15](#a15) | `GET /systems` | 查询接入系统列表 | 已实现 | 20011 |
| [A16](#a16) | `POST /systems/modules` | 登记模块 | 已实现 | 20013 |
| [A17](#a17) | `POST /systems/function-points` | 登记功能点 | 已实现 | 20014 |
| [A18](#a18) | `POST /systems/role-functions` | 绑定角色功能点 | 已实现 | 20015 |
| [A19](#a19) | `DELETE /systems/role-functions` | 解绑角色功能点 | 已实现 | 20016 |
| [A20](#a20) | `GET /systems/role-functions/{roleId}` | 查询角色功能点绑定记录 | 已实现 | 20017 |
| [A21](#a21) | `GET /open/authorize` | 判定单个功能点权限 | 已实现 | 仅令牌 |
| [A22](#a22) | `POST /open/authorize/batch` | 批量判定功能点权限 | 已实现 | 仅令牌 |
| [A23](#a23) | `GET /open/permissions` | 查询用户有效功能点主键集合 | 已实现 | 仅令牌 |
| [A24](#a24) | `POST /orgs` | 创建组织节点 | 已实现 | 20019 |
| [A25](#a25) | `GET /orgs` | 查询组织树 | 已实现 | 20018 |
| [A26](#a26) | `GET /orgs/{orgId}` | 查询组织详情 | 已实现 | 20020 |
| [A27](#a27) | `GET /orgs/{orgId}/children` | 查询组织子节点 | 已实现 | 20021 |
| [A28](#a28) | `PUT /orgs/{orgId}/move` | 移动组织节点 | 已实现 | 20022 |
| [A29](#a29) | `POST /roles/{roleId}/exclusions` | 设置角色互斥 | 待实现 | 仅令牌 |
| [A30](#a30) | `GET /roles/{roleId}/exclusions` | 查询角色互斥记录 | 待实现 | 仅令牌 |
| [A31](#a31) | `POST /systems/data-scopes` | 创建数据范围字典 | 待实现 | 仅令牌 |
| [A32](#a32) | `GET /systems/data-scopes` | 查询数据范围字典 | 待实现 | 仅令牌 |
| [A33](#a33) | `POST /systems/role-data-scopes` | 绑定角色数据范围 | 待实现 | 仅令牌 |
| [A34](#a34) | `GET /audit-logs` | 分页查询审计日志 | 待实现 | 仅令牌 |

## 5. 接口逐项使用说明

每节的 data 结构统一见第 6 章；错误码表见第 8 章。所有 JSON 返回样例均为说明结构而构造，标识、名称、时间和令牌需以你的服务器实际响应为准，不是线上抓包。

### 认证

<a id="a01"></a>

#### A01 · 账号密码登录

**`POST /api/v1/auth/login`** · **已实现**

认证：免登录令牌。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `username` | JSON | string | 是（业务校验） | — | 登录账号，对应 userNo；null、空串、纯空白均拒绝。 |
| `password` | JSON | string | 是（业务校验） | — | 原始密码；null、空串、纯空白均拒绝。 |
| `captchaId` | JSON | string | 否 | — | 已预留但当前不校验。 |
| `captchaCode` | JSON | string | 否 | — | 已预留但当前不校验。 |
| `clientIp` | JSON | string | 否 | — | 调用者提供的来源地址，写入令牌记录；不是控制器自动获取的真实来源地址，数据库长度 64。 |

**实际行为**

- 请求体必须是 JSON 对象。登录接口免令牌，不必发送 Authorization。
- 账号不存在、已删除或密码错误统一返回 2001；账号密码正确后才检查账号是否封存、停用或锁定。
- 成功签发访问令牌、保存其摘要并更新 lastLoginAt；同一用户再次登录不会自动吊销旧令牌。
- expiresIn 来自 jwt.access-token-expire，当前配置为 7200 秒；refreshToken 当前为 null，没有刷新端点。

**返回 data**：`LoginResponse`。

**失败与边界**：400/1002：账号或密码为空；401/2001：账号或密码错误；403/2005：账号封存；403/2006：停用或锁定；请求体缺失或无法解析通常 400/1001。

**请求示例**

```bash
curl -sS -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  --data '{"username":"admin","password":"Admin@123"}'
```

**成功响应结构示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "<服务端实际签发的访问令牌>",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": "1",
    "displayName": "系统管理员",
    "accountStatus": "NORMAL"
  },
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

<a id="a02"></a>

#### A02 · 登出并吊销当前令牌

**`POST /api/v1/auth/logout`** · **已实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

业务参数：无；认证头仍按上方要求发送。

**实际行为**

- 只吊销请求头中的这枚令牌，不是吊销该用户全部令牌。
- 服务层“未知令牌视为已登出”的逻辑不能绕过入口拦截；普通无效令牌、已吊销令牌在进入控制器前就失败。
- 使用同一令牌第二次登出，通常返回 401/2004；成功首次登出返回 200/code=0，且省略 data。

**返回 data**：`无 data`。

**失败与边界**：通用认证错误 401/2001～2004；若此路径另行登记为 ADMIN 功能点，也受相应 403/2007 限制。

**请求示例**

```bash
curl -sS -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a03"></a>

#### A03 · 校验当前令牌

**`GET /api/v1/auth/validate`** · **已实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

业务参数：无；认证头仍按上方要求发送。

**实际行为**

- 必须携带 Authorization: Bearer 令牌，没有 token 查询参数。
- 正常有效令牌返回 data=true；无效、过期或已吊销令牌通常先被认证拦截器拒绝，返回 401，而不是 200/data=false。
- 服务层内部虽可返回 false，但不能据此把该 HTTP 接口当作所有输入均返回布尔值的探测接口。

**返回 data**：`boolean`。

**失败与边界**：通用认证错误 401/2001～2004。

**请求示例**

```bash
curl -sS "$BASE_URL/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**成功响应结构示例**

```json
{
  "code": 0,
  "message": "success",
  "data": true,
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

### 用户

<a id="a04"></a>

#### A04 · 创建用户

**`POST /api/v1/users`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20002`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userNo` | query | string | 是 | — | 账号；数据库列长度 32，唯一，已删除账号也占用编码。 |
| `password` | query | string | 否 | — | 不传、空串或纯空白时使用 iam.default-password；未配置时为 Iam@123456。 |
| `realName` | query | string | 是 | — | 真实姓名，加密存储；返回时为完整明文姓名，当前没有统一非空或长度校验。 |
| `phone` | query | string | 否 | — | 为空或纯空白时不存；否则加密存储并在返回视图中脱敏，服务未做 11 位手机号规则校验。 |
| `orgId` | query | int64 | 是 | — | 所属组织标识；当前仅存值，不校验组织存在或状态。 |
| `post` | query | string | 否 | — | 岗位；数据库列长度 64。 |

**实际行为**

- 参数放 query；不是 JSON 对象。
- 新用户为 NORMAL、isDeleted=0；自动生成用户主键并设置创建和更新时间。
- 只创建账号，不自动授予角色；需要继续调用 POST /roles/grants。
- 姓名、电话和密码相关密文／摘要不在该接口返回。

**返回 data**：`UserView`。

**失败与边界**：409/3009：账号已存在；必需 query 参数缺失或数值无法转换：400/1001；存储长度等数据库异常未细分，可能 500/5000。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/users" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "userNo=DOC_USER_001" \
  --data-urlencode "password=DocPass@123" \
  --data-urlencode "realName=接口示例用户" \
  --data-urlencode "phone=13800138000" \
  --data-urlencode "orgId=1" \
  --data-urlencode "post=联调人员"
```

<a id="a05"></a>

#### A05 · 分页查询用户

**`GET /api/v1/users`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20001`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `page` | query | int32 | 否 | 1 | 对实际查询使用 max(page,1)，但响应回显原始 page；建议只传 >=1。 |
| `size` | query | int32 | 否 | 20 | 必须 >0，否则 400/1001；未设置业务级最大值。 |
| `orgId` | query | int64 | 否 | — | 精确匹配该组织，不自动包含下级组织。 |
| `accountStatus` | query | string | 否 | — | 精确筛选状态；此查询没有调用枚举校验，未知码通常得到空结果。 |

**实际行为**

- 仅返回 isDeleted=0 的用户；不保证排序，不支持指定排序参数。
- 响应是 data.list，不是 data.content，也不是 page_size。
- 没有姓名、账号关键词、电话或岗位查询参数；发送额外参数不代表后端会使用。

**返回 data**：`PageResult<UserView>`。

**失败与边界**：400/1001：page/size/orgId 类型错误或 size<=0；另有通用认证鉴权错误。

**请求示例**

```bash
curl -sS -G -X GET "$BASE_URL/users" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "page=1" \
  --data-urlencode "size=20" \
  --data-urlencode "orgId=1" \
  --data-urlencode "accountStatus=NORMAL"
```

<a id="a06"></a>

#### A06 · 查询用户详情

**`GET /api/v1/users/{userId}`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20003`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | path | int64 | 是 | — | 路径中的用户主键。 |

**实际行为**

- 用户不存在或 isDeleted!=0 时返回 1005。
- 已停用、锁定和封存用户仍可被具有权限的操作人查询；目标用户状态不会自动使详情请求变为 403。

**返回 data**：`UserView`。

**失败与边界**：404/1005：用户不存在或已删除；400/1001：路径标识不能转换。

**请求示例**

```bash
curl -sS "$BASE_URL/users/$TARGET_USER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a07"></a>

#### A07 · 变更账号状态

**`PUT /api/v1/users/{userId}/status`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20004`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | path | int64 | 是 | — | 路径中的目标用户主键。 |
| `targetStatus` | query | string | 是 | — | 严格使用 NORMAL、DISABLED、SEALED 或 LOCKED。 |
| `reason` | query | string | 否 | — | 变更原因，当前仅写应用日志；没有写入审计表。 |

**实际行为**

- NORMAL 为启用；DISABLED/LOCKED/SEALED 均会吊销目标用户全部 ACTIVE 令牌。
- SEALED 还设置 leaveTime，并把该用户全部 ACTIVE 角色关系改为 REVOKED；账号本身保留。
- 先校验 targetStatus 枚举；对已经 SEALED 的用户，传入任何合法目标状态均返回 3004，当前不能用此接口解封。非法目标状态仍先返回 1001。
- DISABLED/LOCKED 不回收角色；恢复 NORMAL 不恢复已吊销的旧令牌，用户必须重新登录。
- 非 SEALED 状态重复设置为相同状态仍会执行相应处理，没有“状态未变就跳过”的规则。

**返回 data**：`UserView`。

**失败与边界**：400/1001：非法 targetStatus；404/1005：用户不存在；409/3004：目标已封存。

**请求示例**

```bash
curl -sS -G -X PUT "$BASE_URL/users/$TARGET_USER_ID/status" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "targetStatus=DISABLED" \
  --data-urlencode "reason=联调状态测试"
```

### 角色

<a id="a08"></a>

#### A08 · 创建角色

**`POST /api/v1/roles`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20006`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleCode` | query | string | 是 | — | 角色编码，数据库长度 64，全局唯一，已删除记录也占用。 |
| `roleName` | query | string | 是 | — | 角色名称，数据库长度 128。 |
| `roleType` | query | string | 是 | — | FUNC 或 DATA；仅类型合法不意味着数据权限能力已实现。 |
| `description` | query | string | 否 | — | 描述，数据库长度 500。 |

**实际行为**

- 先检查 roleType 合法，再检查编码重复。
- 新角色 status=ACTIVE、isDeleted=0；不接受 parentId、createdBy、status 等额外参数作为角色创建字段。
- 没有角色修改、删除、停用、复制和配置继承的已实现接口。

**返回 data**：`IamRole`。

**失败与边界**：400/1001：角色类型非法；409/3009：角色编码已存在。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/roles" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "roleCode=DOC_VIEWER_001" \
  --data-urlencode "roleName=文档示例查看角色" \
  --data-urlencode "roleType=FUNC" \
  --data-urlencode "description=接口手册联调示例"
```

<a id="a09"></a>

#### A09 · 查询角色列表

**`GET /api/v1/roles`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20005`。

业务参数：无；认证头仍按上方要求发送。

**实际行为**

- 返回全部 isDeleted=0 的角色，不分页、不按角色状态或类型过滤，也不保证顺序。
- 不是仅返回 ACTIVE 的角色；调用方如需可授权角色，还应自行根据 status 筛选。

**返回 data**：`IamRole[]`。

**失败与边界**：通用认证鉴权错误。

**请求示例**

```bash
curl -sS "$BASE_URL/roles" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a10"></a>

#### A10 · 查询角色详情

**`GET /api/v1/roles/{roleId}`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20007`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | path | int64 | 是 | — | 角色主键。 |

**实际行为**

- 只排除已删除角色；停用角色如果存在仍可查询。

**返回 data**：`IamRole`。

**失败与边界**：404/1005：角色不存在或已删除；400/1001：路径标识格式错误。

**请求示例**

```bash
curl -sS "$BASE_URL/roles/$ROLE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 用户角色授权

<a id="a11"></a>

#### A11 · 向用户授予单个角色

**`POST /api/v1/roles/grants`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20008`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | query | int64 | 是 | — | 目标用户主键，不是当前操作人的固定字段。 |
| `roleId` | query | int64 | 是 | — | 目标角色主键。 |
| `expireTime` | query | 本地日期时间 | 否 | — | 必须严格晚于服务器当前时间；省略表示长期。示例 2027-12-31T23:59:59，不带 Z 或时区。 |
| `grantBy` | query | int64 | 否 | — | 调用方填写的授权人标识；当前不会自动取登录用户，也不验证与其相等。 |
| `grantReason` | query | string | 否 | — | 授权原因，数据库长度 255。 |

**实际行为**

- 一次请求只授予一个 roleId，不存在 HTTP 批量 roleIds 参数；GrantService.grantRoles 是内部方法。
- 校验顺序：目标用户存在且未删除 → 用户为 NORMAL → 角色存在且未删除 → 角色为 ACTIVE → 到期时间 → 重复授权。
- 新授权 effectiveTime=服务器当前时间、status=ACTIVE；不接受调用方设置未来生效时间。
- 若同一用户角色已有 ACTIVE 记录，返回 3005，即使该记录 expireTime 已过去也不会自动视为可再次授予。
- 已回收等非 ACTIVE 的同一关系可再次授予，复用原记录主键并更新有效期、原因和授权人。
- 没有角色互斥检查、继承计算、审批或审计快照写入。

**返回 data**：`IamUserRole`。

**失败与边界**：404/1005：用户或角色不存在／已删除；409/3010：目标用户不是 NORMAL；400/1001：角色停用或 expireTime 不在未来／格式错误；409/3005：已有 ACTIVE 关系。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/roles/grants" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "roleId=$ROLE_ID" \
  --data-urlencode "grantReason=示例功能授权"
```

<a id="a12"></a>

#### A12 · 回收用户的单个角色

**`DELETE /api/v1/roles/grants`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20009`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | query | int64 | 是 | — | 目标用户主键。 |
| `roleId` | query | int64 | 是 | — | 目标角色主键。 |

**实际行为**

- 查找同一用户与角色的 ACTIVE 关系，把 status 改为 REVOKED 并更新时间；不物理删除。
- 当前不接收回收原因参数，也不验证用户或角色当前状态；到期但仍是 ACTIVE 的记录也能回收。
- 重复回收或关系不存在返回 1005；不是无条件成功的幂等操作。
- 回收后下一次服务端功能权限判定不再计入该关系，不需要等缓存失效；前端菜单可能仍按其轮询周期更新。

**返回 data**：`无 data`。

**失败与边界**：404/1005：不存在 ACTIVE 关系；另有通用认证鉴权错误。

**请求示例**

```bash
curl -sS -G -X DELETE "$BASE_URL/roles/grants" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "roleId=$ROLE_ID"
```

<a id="a13"></a>

#### A13 · 查询用户角色关系记录

**`GET /api/v1/roles/grants/{userId}`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20010`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | path | int64 | 是 | — | 目标用户主键。 |

**实际行为**

- 返回该用户全部关系记录，包括 ACTIVE、REVOKED 和数据库中已有的其他状态，不仅是当前有效角色。
- 不会自动把已经超过 expireTime 的 ACTIVE 记录改为 EXPIRED；真实权限需同时判断状态和时间。
- 不保证排序，不分页，不联表返回 roleName；角色名需另查角色列表。
- 未对目标用户做存在性检查，通常没有关系就返回 []，不能用空数组断定用户一定存在。

**返回 data**：`IamUserRole[]`。

**失败与边界**：通用认证鉴权错误及路径类型错误。

**请求示例**

```bash
curl -sS "$BASE_URL/roles/grants/$TARGET_USER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 系统与功能权限

<a id="a14"></a>

#### A14 · 登记接入系统

**`POST /api/v1/systems`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20012`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `systemCode` | query | string | 是 | — | 系统编码，数据库长度 64，全局唯一；后续权限查询使用此编码。 |
| `systemName` | query | string | 是 | — | 系统名称，数据库长度 128。 |
| `description` | query | string | 否 | — | 描述，数据库长度 500。 |

**实际行为**

- 创建为 ACTIVE、isDeleted=0；该接口不创建模块、角色或授权。
- callbackUrl 和 publicKey 虽然在返回实体中存在，但当前登记接口不接受它们，也没有密钥签发流程。
- 已删除的相同 systemCode 也被重复检查拦截。

**返回 data**：`IamSystem`。

**失败与边界**：409/3009：系统编码已存在；另有通用参数和存储错误。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/systems" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "systemCode=DOC_APP_001" \
  --data-urlencode "systemName=文档联调系统" \
  --data-urlencode "description=实际调用示例"
```

<a id="a15"></a>

#### A15 · 查询接入系统列表

**`GET /api/v1/systems`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20011`。

业务参数：无；认证头仍按上方要求发送。

**实际行为**

- 返回所有 isDeleted=0 的系统，不分页、不筛选 status，也不保证顺序。

**返回 data**：`IamSystem[]`。

**失败与边界**：通用认证鉴权错误。

**请求示例**

```bash
curl -sS "$BASE_URL/systems" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a16"></a>

#### A16 · 登记模块

**`POST /api/v1/systems/modules`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20013`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `systemId` | query | int64 | 是 | — | 父系统主键，注意不是 systemCode。 |
| `moduleCode` | query | string | 是 | — | 模块编码，数据库长度 64，同一系统内唯一。 |
| `moduleName` | query | string | 是 | — | 模块名称，数据库长度 128。 |
| `sortNo` | query | int32 | 否 | 0 | 排序号，省略时服务写 0；未做非负校验。 |

**实际行为**

- 仅检查 systemId 对应记录存在，不检查该系统是否 ACTIVE 或 isDeleted=0。
- 新模块 ACTIVE、isDeleted=0；服务未预查重复 moduleCode，违反数据库唯一约束通常返回 5000，不是稳定的 3009。
- 没有已实现的模块列表或模块详情查询入口；保存好创建返回的 id。

**返回 data**：`IamModule`。

**失败与边界**：404/1005：系统不存在；重复系统内模块编码等存储异常通常 500/5000。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/systems/modules" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "systemId=$SYSTEM_ID" \
  --data-urlencode "moduleCode=DOCUMENTS" \
  --data-urlencode "moduleName=文档模块" \
  --data-urlencode "sortNo=1"
```

<a id="a17"></a>

#### A17 · 登记功能点

**`POST /api/v1/systems/function-points`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20014`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `moduleId` | query | int64 | 是 | — | 父模块主键，必须存在。 |
| `pointCode` | query | string | 是 | — | 功能点编码，数据库长度 64，同一模块内唯一。 |
| `pointName` | query | string | 是 | — | 功能点名称，数据库长度 128。 |
| `action` | query | string | 是 | — | 严格取本手册列出的 12 个操作码之一。 |
| `apiPath` | query | string | 否 | — | 接口映射模板，长度 255；如 /users/{userId}，不包含 /api/v1、主机或查询串；模板占位符须按实际映射一致。 |
| `apiMethod` | query | string | 否 | — | 请求方法，数据库长度 16；非 null 时转大写。服务未限制为固定方法集合。 |

**实际行为**

- 校验模块存在、action 合法；apiPath 和 apiMethod 都非 null 时预查该组合是否已登记。组合在全库唯一，不是每个系统分别唯一。
- apiPath 不做去空格、前缀删除或路径模板归一；建议这两个映射字段一起提供或一起省略。
- 同模块 pointCode 的重复由数据库约束处理，服务未预查，因此不保证返回 3009。
- 登记一条其他业务系统的路径，不会自动在该业务系统部署拦截器；业务系统需要实际接入权限判定。
- 当前没有已实现的功能点列表入口；保存创建返回的 id。

**返回 data**：`IamFunctionPoint`。

**失败与边界**：404/1005：模块不存在；400/1001：action 非法；409/3009：接口模板与方法组合重复；编码唯一约束等存储错误可能 500/5000。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/systems/function-points" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "moduleId=$MODULE_ID" \
  --data-urlencode "pointCode=DOCUMENT_VIEW" \
  --data-urlencode "pointName=查看文档" \
  --data-urlencode "action=VIEW" \
  --data-urlencode "apiPath=/documents/{documentId}" \
  --data-urlencode "apiMethod=GET"
```

<a id="a18"></a>

#### A18 · 绑定角色功能点

**`POST /api/v1/systems/role-functions`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20015`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | query | int64 | 是 | — | 角色主键。 |
| `allowedAction` | query | string | 是 | — | 12 个合法操作码之一，不是任意动作数组。 |
| `createdBy` | query | int64 | 否 | — | 调用方提供的创建人标识，不自动取当前登录人。 |
| `请求体` | JSON body | int64[] | 是 | — | 裸数组，例如 [5001,5002]；不是 {"functionPointIds":[...]}。 |

**实际行为**

- 混合传参：roleId、allowedAction、createdBy 在 query，功能点主键数组在 JSON 请求体。
- 角色必须存在、未删除且 ACTIVE，否则 1005；仅检查功能点存在，不检查其 status、isDeleted 或所属系统。
- 数组内重复标识去重；已经绑定的功能点直接跳过，不覆盖原 allowedAction 或 createdBy。空数组 [] 不新增关系。
- 任一功能点不存在使事务回滚，不能认为前面几个绑定已永久成功。
- 当前决策只按功能点关系判断是否拥有，没有进一步解释 allowedAction；例如 NONE 并不自动使判定返回 false。需要撤销时应调用解绑接口。

**返回 data**：`无 data`。

**失败与边界**：404/1005：角色不存在／停用／已删除或功能点不存在；400/1001：非法操作码、缺少必填 query、请求体缺失或形状错误；并发唯一冲突等可能 500/5000。

**请求示例**

```bash
curl -sS -X POST "$BASE_URL/systems/role-functions?roleId=$ROLE_ID&allowedAction=VIEW" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  --data "[$FUNCTION_POINT_ID]"
```

<a id="a19"></a>

#### A19 · 解绑角色功能点

**`DELETE /api/v1/systems/role-functions`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20016`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | query | int64 | 是 | — | 目标角色主键。 |
| `请求体` | JSON body | int64[] | 是 | — | 要解绑的功能点主键裸数组。 |

**实际行为**

- roleId 放 query，数组放 JSON 请求体；没有 allowedAction 参数。
- 只删除该角色已绑定且在数组中的记录，调用数据库物理删除；不存在的绑定忽略。
- 没有角色存在性校验；空数组或没有匹配关系仍成功，可重复调用。

**返回 data**：`无 data`。

**失败与边界**：通用认证、参数绑定和请求体错误；存储失败可能 500/5000。

**请求示例**

```bash
curl -sS -X DELETE "$BASE_URL/systems/role-functions?roleId=$ROLE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  --data "[$FUNCTION_POINT_ID]"
```

<a id="a20"></a>

#### A20 · 查询角色功能点绑定记录

**`GET /api/v1/systems/role-functions/{roleId}`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20017`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | path | int64 | 是 | — | 目标角色主键。 |

**实际行为**

- 返回绑定关系数组，不是纯功能点主键数组，也不联表返回功能点名称。
- 未校验角色存在、状态及功能点状态；无关系通常返回 []，不分页、不保证顺序。

**返回 data**：`IamRoleFunction[]`。

**失败与边界**：通用认证鉴权错误及路径类型错误。

**请求示例**

```bash
curl -sS "$BASE_URL/systems/role-functions/$ROLE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 对外功能鉴权

<a id="a21"></a>

#### A21 · 判定单个功能点权限

**`GET /api/v1/open/authorize`** · **已实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | query | int64 | 是 | — | 待判定用户主键，与调用令牌所属用户是两个概念。 |
| `systemCode` | query | string | 是 | — | 目标系统编码，如 ADMIN；不是系统数字主键。 |
| `functionPointId` | query | int64 | 是 | — | 待判定功能点主键。 |

**实际行为**

- 返回 data=true 或 false；“无权限”是业务判定成功，不是 403。403/2007 只可能来自请求自身被入口权限拦截。
- 目标用户不存在／非 NORMAL、无有效授权、系统编码不存在或功能点不属于该系统等情况，通常返回 false。
- 当前只判定功能点，不接收数据对象、组织或项目条件；没有数据范围判定。
- 完整判定过滤规则见“权限算法与调用边界”。

**返回 data**：`boolean`。

**失败与边界**：通用认证、参数错误；正常无权限为 200/code=0/data=false。

**请求示例**

```bash
curl -sS -G -X GET "$BASE_URL/open/authorize" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "systemCode=DOC_APP_001" \
  --data-urlencode "functionPointId=$FUNCTION_POINT_ID"
```

**成功响应结构示例**

```json
{
  "code": 0,
  "message": "success",
  "data": true,
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

<a id="a22"></a>

#### A22 · 批量判定功能点权限

**`POST /api/v1/open/authorize/batch`** · **已实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | JSON | int64 | 业务必需；无字段注解校验 | — | 目标用户主键。 |
| `systemCode` | JSON | string | 业务必需；无字段注解校验 | — | 目标系统编码。 |
| `functionPointIds` | JSON | int64[] | 业务必需；无字段注解校验 | — | 待判定数组；允许 []，不要提交 null 或空元素。 |

**实际行为**

- 请求体是对象，不是裸数组；userId、systemCode、functionPointIds 都放 JSON。
- 只计算一次用户权限集合，再逐个对比；重复功能点主键合并为同一个结果键。
- data.results 是对象，序列化后主键成为字符串；不保证键顺序。空数组对应 results={}。
- 请求体类没有字段级校验注解；缺字段或 null 不保证统一参数错误，部分路径会变为 5000，调用方应提交完整结构。

**返回 data**：`PermissionCheckResult`。

**失败与边界**：通用认证错误；缺失／不可解析请求体 400/1001；字段 null 等未覆盖输入可能 400/1001 或 500/5000；单项无权限仍为成功返回 false。

**请求示例**

```bash
curl -sS -X POST "$BASE_URL/open/authorize/batch" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  --data "{\"userId\":$TARGET_USER_ID,\"systemCode\":\"DOC_APP_001\",\"functionPointIds\":[$FUNCTION_POINT_ID,999999]}"
```

**成功响应结构示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "results": {
      "5001": true,
      "999999": false
    }
  },
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

<a id="a23"></a>

#### A23 · 查询用户有效功能点主键集合

**`GET /api/v1/open/permissions`** · **已实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `userId` | query | int64 | 是 | — | 待查询用户主键。 |
| `systemCode` | query | string | 是 | — | 目标系统编码。 |

**实际行为**

- 返回集合序列化的数字数组，不是权限对象，不含角色名称、动作、菜单树、版本号或数据范围。
- 去重但不保证顺序；未知用户、非正常用户、未知系统或没有有效授权时通常是 []。
- 前端 ADMIN 菜单控制消费的是这一接口的结果，但获得菜单功能点不自动等于获得对应接口功能点。

**返回 data**：`int64[]`。

**失败与边界**：通用认证、参数错误；正常没有权限时为 200/code=0/data=[]。

**请求示例**

```bash
curl -sS -G -X GET "$BASE_URL/open/permissions" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "systemCode=DOC_APP_001"
```

**成功响应结构示例**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    5001
  ],
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

### 组织机构

组织为四级树：HQ（总公司，层级 1）→ BRANCH（分公司，2）→ DEPT（部门，3）→ PROJECT（项目部，4）。`ancestors` 存从根到父节点的主键路径，根节点为 `"0"`，例如信息中心为 `"0,1"`。以下查询只过滤 `isDeleted=1` 的节点，不按 `status` 过滤；当前没有修改、停用、删除组织的接口。

<a id="a24"></a>

#### A24 · 创建组织节点

**`POST /api/v1/orgs`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20019`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `orgCode` | query | string | 是 | — | 组织编码，全局唯一。 |
| `orgName` | query | string | 是 | — | 组织名称。 |
| `orgType` | query | string | 是 | — | HQ / BRANCH / DEPT / PROJECT。 |
| `orgLevel` | query | int32 | 是 | — | 必须等于类型对应层级：HQ=1、BRANCH=2、DEPT=3、PROJECT=4。 |
| `parentId` | query | int64 | 否 | — | 父组织主键；不传则创建根节点。 |
| `sortNo` | query | int32 | 否 | 0 | 同级排序号。 |

**实际行为**

- 先校验类型合法、层级与类型一致，再按 `orgCode` 预查重复，最后校验父节点。
- 传 `parentId` 时，父节点必须存在且未删除，且父节点层级数值必须小于本节点（只要求更高，不要求相邻，例如 DEPT 可直接挂在 HQ 下）。
- `ancestors` 由服务计算：根节点为 `"0"`，否则为父节点 `ancestors` + `,` + 父节点主键。
- 新节点 `status=ACTIVE`、`isDeleted=0`；`leaderUserId`、`createdBy`、`updatedBy` 不写入，保持 null。

**返回 data**：`IamOrg`。

**失败与边界**：400/1001：orgType 非法、orgLevel 与类型不一致、父节点层级不高于本节点、缺少必需 query；404/1005：父节点不存在或已删除；409/3009：orgCode 已存在。并发重复提交或字段超长可能落到数据库约束，返回 500/5000。

**请求示例**

```bash
curl -sS -G -X POST "$BASE_URL/orgs" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "orgCode=DOC_BRANCH" \
  --data-urlencode "orgName=示例分公司" \
  --data-urlencode "orgType=BRANCH" \
  --data-urlencode "orgLevel=2" \
  --data-urlencode "parentId=1"
```

<a id="a25"></a>

#### A25 · 查询组织树

**`GET /api/v1/orgs`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20018`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `rootId` | query | int64 | 否 | — | 子树根节点主键。 |

**实际行为**

- 不传 `rootId`：返回所有 `parentId` 为 null 的未删除节点（顶层），按 `sortNo`、主键升序；下级通过 A27 逐层查询。
- 传 `rootId`：返回该节点及其全部未删除下级的**扁平数组**，按 `orgLevel`、`sortNo`、主键升序。下级判定为 `ancestors` 等于"根节点路径"或以"根节点路径,"开头，`0,1` 不会误匹配 `0,12`。
- 返回的 `IamOrg` 没有 `children` 字段；需要嵌套树时由调用方按 `parentId` 组装。

**返回 data**：`IamOrg[]`。

**失败与边界**：404/1005：rootId 不存在或已删除；rootId 不是整数时 400/1001。

**请求示例**

```bash
curl -sS "$BASE_URL/orgs" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a26"></a>

#### A26 · 查询组织详情

**`GET /api/v1/orgs/{orgId}`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20020`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `orgId` | path | int64 | 是 | — | 组织主键。 |

**实际行为**

- 返回单个组织；已删除节点视为不存在。

**返回 data**：`IamOrg`。

**失败与边界**：404/1005：组织不存在或已删除；路径类型错误 400/1001。

**请求示例**

```bash
curl -sS "$BASE_URL/orgs/1" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a27"></a>

#### A27 · 查询组织子节点

**`GET /api/v1/orgs/{orgId}/children`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20021`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `orgId` | path | int64 | 是 | — | 父组织主键。 |

**实际行为**

- 先校验父组织存在且未删除，再返回其直接下级（不含更深层级），按 `sortNo`、主键升序；没有下级时返回 []。

**返回 data**：`IamOrg[]`。

**失败与边界**：404/1005：父组织不存在或已删除；路径类型错误 400/1001。

**请求示例**

```bash
curl -sS "$BASE_URL/orgs/1/children" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a28"></a>

#### A28 · 移动组织节点

**`PUT /api/v1/orgs/{orgId}/move`** · **已实现**

认证：必需 Bearer 令牌；初始化接口功能点 `20022`。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `orgId` | path | int64 | 是 | — | 被移动组织主键。 |
| `newParentId` | query | int64 | 是 | — | 目标父组织主键；没有 reason 或 version 参数。 |

**实际行为**

- 被移动节点和目标父节点都必须存在且未删除。
- 目标父节点不能是自身，也不能位于被移动节点的子树中（成环）；目标父节点层级数值必须小于被移动节点（跨级）。不能用于把节点移成根节点。
- 更新本节点的 `parentId`、`ancestors`、`updatedAt`，并在同一事务中把全部下级 `ancestors` 中的旧路径前缀替换为新前缀；`orgLevel`、`orgType` 不变。

**返回 data**：`IamOrg`（移动后的节点）。

**失败与边界**：404/1005：节点或目标父节点不存在或已删除；409/3003：移动到自身或自身下级、目标父节点层级不高于本节点；缺少 newParentId 400/1001。

**请求示例**

```bash
curl -sS -G -X PUT "$BASE_URL/orgs/$ORG_ID/move" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "newParentId=1"
```

### 已定义但未实现

<a id="a29"></a>

#### A29 · 设置角色互斥

**`POST /api/v1/roles/{roleId}/exclusions`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | path | int64 | 是 | — | 当前角色主键。 |
| `excludedRoleId` | query | int64 | 是 | — | 互斥角色主键。 |
| `reason` | query | string | 否 | — | 互斥原因。 |

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。

**返回 data**：`声明无 data；当前无成功响应`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS -G -X POST "$BASE_URL/roles/$ROLE_ID/exclusions" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "excludedRoleId=2" \
  --data-urlencode "reason=职责分离"
```

<a id="a30"></a>

#### A30 · 查询角色互斥记录

**`GET /api/v1/roles/{roleId}/exclusions`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | path | int64 | 是 | — | 角色主键。 |

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。

**返回 data**：`声明 IamRoleExclusion[]；当前无成功数据`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS "$BASE_URL/roles/$ROLE_ID/exclusions" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a31"></a>

#### A31 · 创建数据范围字典

**`POST /api/v1/systems/data-scopes`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `scopeCode` | query | string | 是 | — | 范围编码。 |
| `scopeName` | query | string | 是 | — | 范围名称。 |
| `description` | query | string | 否 | — | 描述。 |
| `sortNo` | query | int32 | 否 | — | 无控制器默认值。 |

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。

**返回 data**：`声明 IamDataScope；当前无成功数据`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS -G -X POST "$BASE_URL/systems/data-scopes" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "scopeCode=DOC_SCOPE" \
  --data-urlencode "scopeName=示例范围"
```

<a id="a32"></a>

#### A32 · 查询数据范围字典

**`GET /api/v1/systems/data-scopes`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

业务参数：无；认证头仍按上方要求发送。

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。

**返回 data**：`声明 IamDataScope[]；当前无成功数据`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS "$BASE_URL/systems/data-scopes" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

<a id="a33"></a>

#### A33 · 绑定角色数据范围

**`POST /api/v1/systems/role-data-scopes`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `roleId` | query | int64 | 是 | — | 角色主键。 |
| `scopeType` | query | string | 是 | — | 规划值 ALL/BRANCH/DEPT/PROJECT/SELF，当前方法体未校验。 |
| `orgId` | query | int64 | 否 | — | 关联组织。 |
| `projectId` | query | int64 | 否 | — | 关联项目。 |
| `createdBy` | query | int64 | 否 | — | 调用方提供的创建人；无 effectiveTime 或 expireTime 参数。 |

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。

**返回 data**：`声明无 data；当前无成功响应`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS -G -X POST "$BASE_URL/systems/role-data-scopes" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "roleId=$ROLE_ID" \
  --data-urlencode "scopeType=DEPT" \
  --data-urlencode "orgId=1"
```

<a id="a34"></a>

#### A34 · 分页查询审计日志

**`GET /api/v1/audit-logs`** · **待实现**

认证：必需 Bearer 令牌；初始化未登记接口功能点。

| 参数 | 位置 | 类型 | 必需 | 默认 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `page` | query | int32 | 否 | 1 | 当前尚未执行分页逻辑。 |
| `size` | query | int32 | 否 | 20 | 当前尚未执行分页逻辑。 |
| `userId` | query | int64 | 否 | — | 操作用户。 |
| `systemCode` | query | string | 否 | — | 系统编码。 |
| `startTime` | query | 本地日期时间 | 否 | — | 签名使用 ISO 日期时间绑定；不是后端强制必填。 |
| `endTime` | query | 本地日期时间 | 否 | — | 签名使用 ISO 日期时间绑定；不是后端强制必填。 |

**实际行为**

- 当前控制器直接抛出 UnsupportedOperationException("待实现")，没有可用成功响应。
- 以下是现有方法签名，不是已运行的业务契约；认证和参数绑定先执行，因此可能先返回 401、403 或 400。
- 不能把审计返回写成已实现的 PageResult：当前方法声明是 Spring Page，尚未落实实际成功结构。前端期望与后端声明的差异仍待后续实现时统一。

**返回 data**：`声明 Spring Page<IamAuditLog>；当前无成功数据`。

**失败与边界**：合法请求通过认证与参数绑定后：500/5000，message="服务器内部错误"；并非 501。

**请求示例（当前仅可验证失败行为）**

```bash
curl -sS -G -X GET "$BASE_URL/audit-logs" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "page=1" \
  --data-urlencode "size=20" \
  --data-urlencode "startTime=2026-10-26T00:00:00" \
  --data-urlencode "endTime=2026-10-26T23:59:59"
```

## 6. 返回数据字典

### 6.1 阅读规则

- 本章列出全部已实现接口使用的数据字段，以及未实现接口的声明类型。它们是当前代码字段，不按旧设计文档重命名。
- `int64` 是 Java Long 序列化的 JSON 数字；`int32` 是 Integer/int；`string` 是文本；本地日期时间使用带 `T` 的字符串。
- `Result` 的非空字段过滤只作用于最外层；下列普通数据对象没有统一省略 null 的注解。不要把 `phoneMasked:null`、`refreshToken:null` 等字段当作“不可能出现”。
- 数据库非空约束不等于 HTTP 输入验证，也不等于新建实体的所有返回字段已由服务显式填充。下列“说明”只在代码确实写入时说明默认值。

### 6.2 LoginResponse

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | string | 访问令牌，后续通过 Authorization 请求头使用。 |
| `refreshToken` | string | 当前未生成，正常为 null；没有可用刷新端点。 |
| `tokenType` | string | 当前固定 Bearer。 |
| `expiresIn` | int64 | 访问令牌有效期，单位秒；当前配置 7200。 |
| `userId` | string | 字符串用户主键，例如 "1"，不同于其他接口常见的数字主键。 |
| `displayName` | string | 解密后的完整姓名，当前没有姓名脱敏。 |
| `accountStatus` | string | 成功登录时为 NORMAL。 |

### 6.3 UserView

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `userNo` | string | 登录账号。 |
| `realName` | string | 解密后的完整姓名。 |
| `phoneMasked` | string | 无电话时为 null；长度 >=7 时为前三位 + **** + 后四位，长度 <7 时按原值返回。 |
| `orgId` | int64 | 关联组织主键；当前用户创建只存入，不验证组织存在。 |
| `post` | string | 岗位，可为 null。 |
| `accountStatus` | string | NORMAL / DISABLED / SEALED / LOCKED。 |
| `leaveTime` | string / 本地日期时间 | 封存时设置的离职时间；此前可为 null。 |
| `lastLoginAt` | string / 本地日期时间 | 最近成功登录时间；未登录可为 null。 |
| `isDeleted` | int32 | 当前用户视图查询和创建正常返回为 0。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |

### 6.4 IamRole

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `roleCode` | string | 角色编码。 |
| `roleName` | string | 角色名称。 |
| `roleType` | string | FUNC / DATA。 |
| `parentId` | int64 | 父角色主键，可为 null；字段存在但当前不计算继承。 |
| `status` | string | 资源状态，当前创建写 ACTIVE；列表是否筛选状态以各接口说明为准。 |
| `description` | string | 描述，可为 null。 |
| `isDeleted` | int32 | 删除标志，数字 0/1；当前相关创建写 0。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |
| `createdBy` | int64 | 创建人主键，可为 null；不能假定服务自动填写为当前登录人。 |
| `updatedBy` | int64 | 更新人主键，可为 null；当前创建流程不填写。 |

### 6.5 IamUserRole

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `userId` | int64 | 关联用户主键。 |
| `roleId` | int64 | 关联角色主键。 |
| `effectiveTime` | string / 本地日期时间 | 生效时间，可为 null；当前用户角色授予写服务器当前时间。 |
| `expireTime` | string / 本地日期时间 | 失效时间，可为 null，null 表示不设截止；等于当前时刻即不再计入有效权限。 |
| `grantBy` | int64 | 授权人主键，可为 null；由调用方传入，服务不自动取登录人。 |
| `grantReason` | string | 授权原因，可为 null；回收操作不覆盖它。 |
| `status` | string | ACTIVE / REVOKED，数据库还约定 EXPIRED；到期不会自动修改本字段。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |

### 6.6 IamSystem

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `systemCode` | string | 系统编码，鉴权请求使用此值。 |
| `systemName` | string | 系统名称。 |
| `callbackUrl` | string | 回调地址，可为 null；当前登记入口不接受该字段。 |
| `publicKey` | string | 公钥文本，可为 null；当前没有完整密钥管理接口。 |
| `status` | string | 资源状态，当前创建写 ACTIVE；列表是否筛选状态以各接口说明为准。 |
| `description` | string | 描述，可为 null。 |
| `isDeleted` | int32 | 删除标志，数字 0/1；当前相关创建写 0。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |

### 6.7 IamModule

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `systemId` | int64 | 所属接入系统的数字主键，不是 systemCode。 |
| `moduleCode` | string | 模块编码。 |
| `moduleName` | string | 模块名称。 |
| `sortNo` | int32 | 排序号，当前模块创建省略时写 0；仅有该字段不表示列表已经排序。 |
| `status` | string | 资源状态，当前创建写 ACTIVE；列表是否筛选状态以各接口说明为准。 |
| `isDeleted` | int32 | 删除标志，数字 0/1；当前相关创建写 0。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |

### 6.8 IamFunctionPoint

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `moduleId` | int64 | 所属模块主键。 |
| `pointCode` | string | 功能点编码。 |
| `pointName` | string | 功能点名称。 |
| `action` | string | 12 个操作码之一；当前决策不进一步展开动作语义。 |
| `apiPath` | string | 映射模板，可为 null；按请求原样保存。正确登记应省略 /api/v1 前缀，代码不会自动删除错误前缀。 |
| `apiMethod` | string | 映射请求方法，可为 null；当前登记时对非 null 值转大写。 |
| `status` | string | 资源状态，当前创建写 ACTIVE；列表是否筛选状态以各接口说明为准。 |
| `isDeleted` | int32 | 删除标志，数字 0/1；当前相关创建写 0。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间。 |

### 6.9 IamRoleFunction

已实现接口返回。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `roleId` | int64 | 关联角色主键。 |
| `functionPointId` | int64 | 关联功能点主键。 |
| `allowedAction` | string | 绑定记录中的操作码；当前决策不解释其语义，NONE 也不等于自动拒绝。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `createdBy` | int64 | 创建人主键，可为 null；不能假定服务自动填写为当前登录人。 |

### 6.10 IamOrg

A24～A28 的返回类型。实体没有 `children` 字段。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `parentId` | int64 | 父节点主键；根节点为 null。 |
| `ancestors` | string | 从根到父节点的主键路径，逗号分隔；根节点为 `"0"`。 |
| `orgCode` | string | 组织编码，全局唯一。 |
| `orgName` | string | 组织名称。 |
| `orgType` | string | HQ / BRANCH / DEPT / PROJECT。 |
| `orgLevel` | int32 | 1～4，与 orgType 对应。 |
| `leaderUserId` | int64 | 负责人主键；当前没有写入接口，通常为 null。 |
| `sortNo` | int32 | 同级排序号，创建时默认 0。 |
| `status` | string | 创建时为 ACTIVE；当前查询不按此字段过滤，也没有停用接口。 |
| `isDeleted` | int32 | 删除标志；创建为 0，当前没有删除接口。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `updatedAt` | string / 本地日期时间 | 最近更新时间；移动节点或其上级被移动时更新。 |
| `createdBy` | int64 | 创建人主键；当前创建流程不写入，为 null。 |
| `updatedBy` | int64 | 更新人主键；当前流程不写入，为 null。 |

### 6.11 IamRoleExclusion

仅未实现接口的声明类型，当前无成功响应。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `roleId` | int64 | 关联角色主键。 |
| `excludedRoleId` | int64 | 被配置为互斥的另一角色主键。 |
| `description` | string | 互斥原因文本，可为 null；请求参数名是 reason，实体字段名是 description。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |
| `createdBy` | int64 | 创建人主键，可为 null；不能假定服务自动填写为当前登录人。 |

### 6.12 IamDataScope

仅未实现接口的声明类型，当前无成功响应。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `id` | int64 | 当前对象主键，由数据库生成。 |
| `scopeCode` | string | 数据范围编码。 |
| `scopeName` | string | 数据范围名称。 |
| `description` | string | 描述，可为 null。 |
| `sortNo` | int32 | 排序号。 |
| `status` | string | 数据范围字典状态，当前没有成功业务响应。 |
| `createdAt` | string / 本地日期时间 | 创建时间。 |

### 6.13 IamAuditLog

仅未实现接口的声明类型，当前无成功响应。

| 字段 | JSON 类型 | 说明 |
| --- | --- | --- |
| `logId` | int64 | 审计记录主键。 |
| `userId` | int64 | 关联操作用户主键，可为 null，例如系统事件。 |
| `username` | string | 操作用户名快照，可为 null。 |
| `action` | string | 操作动作文本。 |
| `operateTime` | string / 本地日期时间 | 操作时间。 |
| `success` | int32 | 数字 1 成功、0 失败，不是 boolean。 |
| `target` | string | 操作对象文本，可为 null。 |
| `ip` | string | 来源地址文本，可为 null。 |
| `systemCode` | string | 所属系统编码，可为 null。 |
| `detail` | string | 详情文本，可为 null；Java 类型为 String，即使数据库列为 JSON，也不能据此声明实际 HTTP 返回为对象。 |
| `decisionId` | string | 决策关联标识，可为 null。 |

### 6.14 分页、数组和批量结果

`PageResult<UserView>` 仅用于当前用户分页接口：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 2,
        "userNo": "DOC_USER_001",
        "realName": "接口示例用户",
        "phoneMasked": "138****8000",
        "orgId": 1,
        "post": "联调人员",
        "accountStatus": "NORMAL",
        "leaveTime": null,
        "lastLoginAt": null,
        "isDeleted": 0,
        "createdAt": "2026-10-26T14:00:00",
        "updatedAt": "2026-10-26T14:00:00"
      }
    ]
  },
  "timestamp": "2026-10-26T14:00:00.123456"
}
```

| 类型 | 实际形状与约束 |
| --- | --- |
| PageResult<T> | total：int64；page：int32；pageSize：int32；list：T[]。超出末页时 list=[]，total 仍是匹配总数。 |
| IamRole[] / IamSystem[] / IamUserRole[] / IamRoleFunction[] | data 是对象数组。通常无数据为 []，不是 null；没有分页包装或顺序保证。 |
| boolean | data 是 true/false，不是字符串 "true"。拒绝业务目标与入口 403 是两个不同结果。 |
| int64[] | data 是去重数字数组；集合的输出顺序不保证。 |
| PermissionCheckResult | data={"results":{"5001":true,"5002":false}}；键是字符串，值是 boolean；空输入数组为 results={}。 |
| 无 data | 成功外层 code=0，省略 data 字段；响应仍有 JSON 内容且 HTTP 为 200。 |
| Spring Page<IamAuditLog> | 仅审计待实现方法的声明；当前没有可依赖的成功分页形状，不能套用用户 PageResult。 |

## 7. 全部业务枚举与状态码值

### 7.1 已实现入口使用的枚举

| 字段 | 合法码 | 说明 |
|---|---|---|
| targetStatus / accountStatus | NORMAL、DISABLED、SEALED、LOCKED | 正常、停用、离职封存、锁定；状态变更会校验，用户列表筛选不做同样的枚举校验 |
| roleType | FUNC、DATA | 功能角色、岗位型数据角色；DATA 只是合法类型，当前数据权限业务未实现 |
| action / allowedAction | NONE、VIEW、ALL、VIEW_EDIT、VIEW_ADD、VIEW_APPROVE、VIEW_EXPORT、VIEW_EDIT_APPROVE、VIEW_ADD_EDIT、VIEW_EDIT_EXPORT、VIEW_SELF、VIEW_EDIT_SELF | 分别为无、查看、全部、查看/编辑、查看/新增、查看/审批、查看/导出、查看/编辑/审批、查看/新增/编辑、查看/编辑/导出、查看本人、查看/编辑本人 |
| 一般资源 status | ACTIVE、DISABLED | 角色、系统、模块、功能点等状态约定；当前创建服务写 ACTIVE，但不代表已提供停用接口 |
| 用户角色关系 status | ACTIVE、EXPIRED、REVOKED | 关系可存储的状态约定；当前创建写 ACTIVE，回收写 REVOKED，到期不会自动写 EXPIRED |
| orgType | HQ、BRANCH、DEPT、PROJECT | 总公司、分公司、部门、项目部，对应层级 1、2、3、4；创建组织时校验类型与层级一致 |

### 7.2 后续能力预留的枚举

| 字段 | 码值 | 当前边界 |
|---|---|---|
| scopeType / DataScopeType | ALL、BRANCH、DEPT、PROJECT、SELF | 全公司、本分公司、本部门、本项目、本人；数据范围接口和判定待实现 |
| ChangeType | GRANT、REVOKE、SEAL、ROLE_UPDATE、SCOPE_UPDATE | 授权、回收、封存、角色变更、范围变更；尚未写入完整变更快照流程 |

## 8. 完整错误码与实际触发范围

以下 31 项与 `ErrorCode.java` 逐项对应。“仅预留”表示枚举已定义，但当前业务未实现相应触发流程，不能据此假定有该能力。HTTP 列是枚举规定的映射；仅预留项尚无可依赖的实际触发接口。

| 业务码 | 名称 | HTTP | 默认 message | 当前触发与边界 |
| --- | --- | --- | --- | --- |
| 0 | SUCCESS | 200 | success | 所有正常成功响应。 |
| 1001 | PARAM_INVALID | 400 | 参数校验失败 | 当前触发：枚举非法、缺少 query、类型转换或请求体绑定失败、size<=0、角色停用时授予、授权到期时间不在未来、组织层级与类型不一致或父组织层级不高于本节点等。 |
| 1002 | PARAM_MISSING | 400 | 必填参数缺失 | 当前触发：登录账号／密码为 null、空串或纯空白；不是通用 query 缺参码。 |
| 1003 | PARAM_FORMAT_ERROR | 400 | 格式错误 | 仅预留；没有独立格式错误触发路径，当前常见格式问题使用 1001。 |
| 1004 | IDEMPOTENCY_CONFLICT | 409 | 幂等键冲突且请求体不一致 | 仅预留；没有重复提交键比对实现。 |
| 1005 | RESOURCE_NOT_FOUND | 404 | 资源不存在 | 当前触发：所需用户／角色／系统／模块／功能点／组织不存在或不满足相应规则；回收时找不到 ACTIVE 关系；部分未找到资源情况。 |
| 2001 | UNAUTHENTICATED | 401 | 未登录或未携带令牌 | 当前触发：缺失或不符合 Bearer 格式的头；登录账号不存在／已删除或密码错误。 |
| 2002 | TOKEN_INVALID | 401 | 令牌无效 | 当前触发：令牌签名、签发者、结构等非法，或数据库没有相应令牌摘要记录。 |
| 2003 | TOKEN_EXPIRED | 401 | 令牌过期 | 当前触发：解析令牌时访问令牌自身已过期。 |
| 2004 | TOKEN_REVOKED | 401 | 令牌已吊销 | 当前触发：合法令牌对应记录 revoked=1。 |
| 2005 | ACCOUNT_SEALED | 403 | 账号已封存（离职） | 当前触发：密码正确后登录用户为 SEALED。 |
| 2006 | ACCOUNT_DISABLED | 403 | 账号停用或锁定 | 当前触发：密码正确后登录用户非 NORMAL 且非 SEALED，包括 DISABLED/LOCKED。 |
| 2007 | NO_FUNCTION_PERM | 403 | 无功能权限 | 当前触发：已登记的接口功能点在 ADMIN 中不属于当前操作人。 |
| 2008 | OUT_OF_DATA_SCOPE | 403 | 越数据范围 | 仅预留；数据范围决策未实现。 |
| 2009 | SIGNATURE_INVALID | 401 | 对外接口签名校验失败 | 仅预留；对外接口签名校验未实现。 |
| 2010 | REPLAY_DETECTED | 401 | 防重放命中 | 仅预留；防重放未实现。 |
| 2011 | APP_DISABLED | 403 | 业务系统接入已停用 | 仅预留；当前决策不按系统停用状态抛该码。 |
| 3001 | ROLE_MUTEX_CONFLICT | 409 | 角色互斥冲突 | 仅预留；角色互斥检查未实现。 |
| 3002 | ROLE_INHERIT_CYCLE | 409 | 角色继承成环 | 仅预留；角色继承检查未实现。 |
| 3003 | ORG_MOVE_INVALID | 409 | 组织节点移动非法 | 当前触发：移动组织到自身或自身下级（成环），或目标父组织层级不高于被移动组织（跨级）。 |
| 3004 | USER_ALREADY_SEALED | 409 | 用户已封存 | 当前触发：targetStatus 枚举合法后，对已 SEALED 的用户调用状态变更，包括试图解封或再次封存；非法枚举先返回 1001。 |
| 3005 | GRANT_DUPLICATE | 409 | 重复授权 | 当前触发：同一用户／角色已有 status=ACTIVE 的关系，时间已过期也仍可能触发。 |
| 3006 | APPROVAL_REQUIRED | 409 | 需审批流 | 仅预留；审批未实现。 |
| 3007 | DATA_SCOPE_EXPIRED | 409 | 临时授权已过期 | 仅预留；实际到期通过权限集合过滤，不抛此码。 |
| 3008 | LOCK_ACQUIRE_FAILED | 409 | 分布式锁获取失败 | 仅预留；业务没有接入分布式锁获取失败流程。 |
| 3009 | RESOURCE_DUPLICATE | 409 | 资源已存在 | 当前触发：创建用户、角色、系统、组织编码重复；登记的非 null 接口模板与方法组合重复。非所有唯一约束错误都会转为该码。 |
| 3010 | TARGET_USER_ABNORMAL | 409 | 目标用户非正常状态 | 当前触发：授予角色时目标用户不是 NORMAL；不是当前操作人令牌失效。 |
| 4001 | RATE_LIMITED | 429 | 触发限流 | 仅预留；没有当前限流触发流程。 |
| 5000 | INTERNAL_ERROR | 500 | 服务器内部错误 | 当前触发：通用异常，包括待实现方法、未专门处理的存储异常、空值缺陷等。 |
| 5001 | SERVICE_UNAVAILABLE | 503 | 依赖服务不可用 | 仅预留；当前依赖异常没有统一映射到此码。 |
| 5002 | DEGRADED | 200 | 决策服务降级 | 仅预留；决策降级未实现。HTTP 定义虽为 200，业务码仍不为 0。 |



### 8.1 实际调用中容易误判的错误

- **缺 query 参数通常是 1001，非 1002。** 1002 当前由登录业务针对空账号或空密码主动抛出。
- **3010 是目标用户异常，不应将操作人登出。** 授予角色给停用、封存或锁定用户，使用 409/3010；2005/2006 主要用于登录账号自身状态。
- **3005 不只代表时间仍有效。** 同一用户角色关系 status 还是 ACTIVE，就会被重复授权检查拦截，哪怕 expireTime 已过去。
- **功能判定 false 是成功结果。** `code=0,data=false` 与请求入口没有权限的 403/2007 是两件事。
- **待实现入口不是 501。** 当前通用异常处理将其包装为 500/5000；“待实现”字样不会作为对外 message 返回。
- **不是所有存储异常都有细分业务码。** 模块或功能点编码唯一冲突、并发重复写、字段超长等若未主动捕获，可能是 500/5000。
- **不要默认错误方法必定得到 405 或错误内容类型必定得到 415。** 当前全局异常处理没有为这两类异常专门分支，可能被统一成 5000；以实际返回为准，不将常见框架默认值写成此实现保证。

## 9. 一条完整的实际调用链示例

下面脚本演示“管理员登录 → 建立系统、模块、功能点、角色和用户 → 绑定功能点 → 给用户授角色 → 验证 → 回收 → 再验证 → 管理员登出”。脚本会新增演示数据，使用时间后缀减少编码重复。**本次编写只做语法与结构检查，没有对真实数据库执行它。**

前提：服务和初始化数据已准备，具备 bash、curl、Python 3；初始管理员密码与环境一致。`DOC_ORG_ID=1` 对应 seed.sql 创建的“总公司”；如使用其他组织，替换成实际主键即可。当前用户创建服务仍不校验组织是否存在。

```bash
set -euo pipefail

BASE_URL='http://localhost:8080/api/v1'
ADMIN_USERNAME='admin'
ADMIN_PASSWORD='Admin@123'
DOC_ORG_ID=1
DOC_SUFFIX="$(date +%s)"
DOC_SYSTEM_CODE="DOC_APP_$DOC_SUFFIX"
DOC_USER_NO="DOC_U_$DOC_SUFFIX"

# 从统一响应中读取 data 的一个字段；失败就停止，不继续用错误响应拼请求。
json_field() {
  python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("code")==0, (d.get("code"),d.get("message")); print(d["data"][sys.argv[1]])' "$1"
}

# 按当前环境的账号密码构造 JSON，避免在请求体拼接中破坏引号。
LOGIN_BODY="$(python3 -c 'import json,sys; print(json.dumps({"username":sys.argv[1],"password":sys.argv[2]}))' "$ADMIN_USERNAME" "$ADMIN_PASSWORD")"
ACCESS_TOKEN="$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' --data "$LOGIN_BODY" | json_field accessToken)"

call_api() {
  local verb="$1"
  local resource="$2"
  shift 2
  local raw_response
  raw_response="$(curl -sS -X "$verb" "$BASE_URL$resource" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    --write-out '\n%{http_code}' "$@")" || return
  printf '%s' "$raw_response" | python3 -c 'import json,sys; raw=sys.stdin.read(); body,status=raw.rsplit("\n",1); d=json.loads(body); assert status=="200", ("HTTP",status,d.get("code"),d.get("message")); assert d.get("code")==0, (d.get("code"),d.get("message")); print(json.dumps(d,ensure_ascii=False))'
}

assert_decision() {
  python3 -c 'import json,sys; d=json.load(sys.stdin); expected=sys.argv[1]=="true"; assert d.get("code")==0 and d.get("data") is expected, ("权限判定不符合预期",d.get("code"),d.get("data")); print(json.dumps(d,ensure_ascii=False))' "$1"
}

SYSTEM_ID="$(call_api POST /systems -G \
  --data-urlencode "systemCode=$DOC_SYSTEM_CODE" \
  --data-urlencode 'systemName=手册演示系统' | json_field id)"

MODULE_ID="$(call_api POST /systems/modules -G \
  --data-urlencode "systemId=$SYSTEM_ID" \
  --data-urlencode 'moduleCode=DOCS' \
  --data-urlencode 'moduleName=演示模块' | json_field id)"

# 这里只测试功能授权，不登记真实管理端接口路径。
FUNCTION_POINT_ID="$(call_api POST /systems/function-points -G \
  --data-urlencode "moduleId=$MODULE_ID" \
  --data-urlencode 'pointCode=DOC_VIEW' \
  --data-urlencode 'pointName=查看示例文档' \
  --data-urlencode 'action=VIEW' | json_field id)"

ROLE_ID="$(call_api POST /roles -G \
  --data-urlencode "roleCode=DOC_ROLE_$DOC_SUFFIX" \
  --data-urlencode 'roleName=手册演示角色' \
  --data-urlencode 'roleType=FUNC' | json_field id)"

TARGET_USER_ID="$(call_api POST /users -G \
  --data-urlencode "userNo=$DOC_USER_NO" \
  --data-urlencode 'password=DocPass@123' \
  --data-urlencode 'realName=手册演示用户' \
  --data-urlencode "orgId=$DOC_ORG_ID" | json_field id)"

call_api POST "/systems/role-functions?roleId=$ROLE_ID&allowedAction=VIEW" \
  -H 'Content-Type: application/json' --data "[$FUNCTION_POINT_ID]"

call_api POST /roles/grants -G \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "roleId=$ROLE_ID" \
  --data-urlencode 'grantReason=手册联调'

# 预期统一成功响应的 data=true。
call_api GET /open/authorize -G \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "systemCode=$DOC_SYSTEM_CODE" \
  --data-urlencode "functionPointId=$FUNCTION_POINT_ID" | assert_decision true

call_api DELETE /roles/grants -G \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "roleId=$ROLE_ID"

# 回收后 data=false，HTTP 仍是 200、code 仍是 0。
call_api GET /open/authorize -G \
  --data-urlencode "userId=$TARGET_USER_ID" \
  --data-urlencode "systemCode=$DOC_SYSTEM_CODE" \
  --data-urlencode "functionPointId=$FUNCTION_POINT_ID" | assert_decision false

# 吊销的是管理员当前访问令牌；演示账号和其他资源仍保留。
call_api POST /auth/logout
unset ACCESS_TOKEN ADMIN_PASSWORD LOGIN_BODY
```

各写接口都有自己的重复调用行为，不能把完整脚本当作可随意重放的数据初始化工具。当前没有覆盖所有演示资源的删除接口，执行后会保留账号、角色、系统、模块和功能点。若要证明目标用户登录后的 ADMIN 页面访问，也需单独授予对应的 ADMIN 菜单和接口功能点；上例仅验证自建系统下的功能判定。

## 10. 当前不存在或不能按设计直接调用的能力

以下不属于本版本已实现的业务入口，不应从旧设计文档、字段或前端模拟文件推导为可用接口：

- `/auth/refresh`、`/auth/profile`、`/auth/captcha` 等认证扩展。
- 用户批量导入、调岗、改密码／重置密码的完整接口。
- 角色修改、停用、复制、删除、继承配置及真正的互斥授予检查。
- `GET /systems/modules` 和 `GET /systems/function-points` 等模块、功能点列表；当前同路径仅有登记 POST。
- 组织修改、停用、删除、负责人设置和人数统计；角色数据范围业务、数据对象范围鉴权、审计写入与导出。
- 对外应用签名／密钥认证、请求防重放、审批、后端通知推送、重复提交识别和限流。
- 内部 `GrantService.grantRoles` 不对应任何 HTTP 批量授权接口。
- 前端 `/demo/ping` 只存在于模拟目录，真实后端没有这个业务控制器。

某条路径使用了错误方法，不一定落到普通“未知路径”；其异常状态也不应按想象填入接口文档。这里列的是不存在的业务能力，不承诺一个统一的探测返回值。

## 11. 排障速查

| 现象 | 先检查 |
|---|---|
| 所有受保护请求 401 | Authorization 是否精确为 Bearer + 空格 + 实际令牌；是否过期／吊销；数据库是否有令牌记录 |
| 登录成功，查用户却 403/2007 | 操作人是否获得对应接口功能点，而不只是用户管理菜单；是否导入 seed.sql 与管理员授权 |
| 明明传了参数仍提示缺失 | 该入口是 query 参数还是 JSON body；尤其用户、角色、系统创建不是统一 JSON 对象 |
| 角色功能绑定请求 400 | 请求体是否为裸数组，roleId 与 allowedAction 是否放在 query，Content-Type 是否匹配 JSON |
| 授权到期时间 400 | 使用带 T 的本地日期时间并正确 URL 编码；是否严格晚于服务端当前时间 |
| 已过期角色再次授予返回 3005 | 该关系数据库 status 可能仍为 ACTIVE；先按业务需要回收，再重新授予 |
| 权限回收后列表仍能看到关系 | 关系查询包含回收记录；检查 status 与有效期，或直接调用功能判定接口 |
| 功能点已登记但没有权限 | 是否完成角色功能绑定、用户角色绑定；角色／用户／功能点状态和授权有效期；systemCode 与模块所属系统是否一致 |
| 列表顺序不稳定 | 当前多数查询没有显式排序，调用方不能依赖集合或数据库默认顺序 |
| 互斥／数据范围／审计 500 | 当前仍待实现；不是修改请求参数就能启用 |
| 创建组织 400 | orgLevel 是否与 orgType 对应（HQ=1…PROJECT=4）；父组织层级是否高于本节点 |
| 移动组织 409/3003 | 目标父组织是否为自身或自身下级；目标父组织层级是否高于被移动组织 |
| 创建模块或功能点出现 500 | 同系统模块编码、同模块功能点编码是否重复；字段长度和数据库日志；并非所有冲突被映射为 3009 |
| requestId 没返回 | 当前没有赋值／回传流程，最外层空值会被省略 |

## 12. 两遍检查与验证记录

本版已完成两遍独立检查，并在修订后再次确认所有发现已解决：

| 检查 | 范围 | 结果 |
|---|---|---|
| 第一遍：逐接口对照源码 | A01～A34 的方法、完整路径、参数位置／必需性／默认值、返回声明、初始化权限和可用状态 | 34/34 对应；修正封存状态错误的前置条件、组织未实现字段的措辞及请求示例后，无未解决项 |
| 第二遍：按调用者视角检查 | 全文、12 类数据对象的 116 字段、31 码、业务规则、默认值、时间、空值、事务、重复请求和完整调用链 | 字段和码值无遗漏；收紧实际行为说明，补齐脚本成功断言和失败停止；修订后无未解决项 |
| 原有后端测试 | 仓库外副本，8 个测试类 | 65/65 通过，0 失败、错误或跳过 |
| 新增接口验证测试 | 仓库外新增 5 个 MockMvc 测试，覆盖 26 次模拟请求 | 5/5 通过；确认日期、序列化、参数错误和全部 11 个待实现映射的实际返回 |
| 文档示例静态与本地执行检查 | 38 个 shell 代码块、9 个 JSON 块；捕获 34 个接口命令和完整链路 12 次调用展开后的真实参数 | 语法、JSON、方法／路径／query／body 均匹配；替代 curl 运行，没有向真实服务发送请求 |
| 示例失败路径 | 模拟绑定 HTTP 500、传输失败、权限结果不符三类情况 | 均立即停止，没有继续后续写操作；不代表真实环境端到端测试 |
| 网页结构 | 115 个内部链接、全部接口锚点、表格与代码块 | 内部链接有效，无重复锚点；网页由同一份 Markdown 生成 |
| 验证时的源码状态 | git status 与验证前后源码摘要 | 验证期间未改动业务源码，文档单独整理提交 |

详细检查过程和原始验证资料保留在本地，未纳入本次文档提交；本章已汇总检查结果和验证边界。

**1.1 修订的验证**（只覆盖组织机构与初始化数据的变更部分）：

| 检查 | 范围 | 结果 |
|---|---|---|
| 逐接口对照源码 | A24～A28 与 `OrganizationService`、`OrgController`、`seed.sql` | 参数、校验顺序、错误码、排序、功能点编号一致 |
| 后端单元与接口测试 | 仓库内全部测试，含 `OrganizationServiceTest` 与接口层组织测试 | 72/72 通过 |
| 真实环境 | MySQL 8.0（服务器时区 UTC）导入 `schema.sql`、`seed.sql`，后端以 `ddl-auto=validate` 启动 | A25、A26、A27 经前端页面真实调用正常；初始化数据时间为北京时间。A24、A28 只经单元与接口测试验证，未在真实数据库上调用 |


验证使用仓库外独立副本，数据库、SQL 初始化及相关连接自动配置被排除，业务服务或数据库访问使用测试替代。测试结果可证明已覆盖的服务／接口行为，**不证明真实 MySQL 持久化、完整应用启动、网络部署或所有客户端端到端流程已经通过**。本手册中的完整写操作示例没有被用于修改真实数据库。

## 13. 源码追溯与后续维护

| 手册内容 | 事实来源 |
|---|---|
| 方法、路径、参数位置和返回声明 | `interfaces/admin/{Auth,User,Role,System,Org,Audit}Controller.java`、`interfaces/openapi/OpenApiController.java` |
| 认证顺序、令牌格式、路径映射 | `interfaces/AuthInterceptor.java`、`interfaces/WebConfig.java`、`application/auth/AuthService.java` |
| 用户、角色、授权和功能点规则 | `application/account/AccountService.java`、`role/RoleService.java`、`grant/GrantService.java`、`permission/FunctionPermissionService.java` |
| 权限过滤和集合返回 | `application/decision/DecisionService.java`、`PermissionCheckResult.java` |
| 响应字段和空值规则 | `common/result/Result.java`、`PageResult.java`、`application/auth/LoginResponse.java`、`application/account/UserView.java`、各返回实体 |
| 全部错误与状态映射 | `common/enums/ErrorCode.java`、`common/exception/GlobalExceptionHandler.java` |
| 字段存储长度、唯一约束和初始权限 | `src/main/resources/db/schema.sql`、`seed.sql` |
| 端口、前缀、令牌有效期与时间配置 | `src/main/resources/application.yml` |

以上 Java 路径均相对于 `src/main/java/com/iam/platform/`。本手册代码基准已固定；后续接口发生变更时，应同时更新参数、返回字段、错误码、权限登记、请求示例和验证记录，而不能只改索引表。
