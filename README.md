# 统一应用软件权限管理系统（IAM）

基于 RBAC 模型的统一身份与权限管理平台，为接入的各业务系统提供集中式的账号管理、角色授权、功能权限判定与数据权限控制能力。

## 项目背景

企业存在多个业务子系统（OA、项目管理系统、财务系统等），各系统独立维护账号与权限，导致账号冗余、权限分散、审计困难。本系统作为**统一权限中心**，集中管理用户身份、组织机构、角色与权限策略，并通过 OpenAPI 为各接入子系统提供鉴权与权限判定服务。适用于多系统、多组织的统一权限治理场景。

## 技术栈

| 类别       | 技术                         |
| ---------- | ---------------------------- |
| 语言       | Java 17                      |
| 框架       | Spring Boot 3.4.5            |
| 持久层     | Spring Data JPA + MySQL 8.0  |
| 安全       | JWT (jjwt 0.12)              |
| 工具       | Lombok                       |
| 校验       | Spring Validation            |
| 构建       | Maven                        |

## 架构设计

项目采用**结构化编程**思想，按"输入→处理→输出"的线性流程组织代码，强调过程内聚与数据流清晰。整体分为四层：

```
┌─────────────────────────────────────────────────┐
│  interfaces（接口层）                            │
│  REST 控制器：admin / openapi / callback         │
├─────────────────────────────────────────────────┤
│  application（应用层）                           │
│  用例编排：M01认证 M02账号 M03组织 ... M12接入   │
├─────────────────────────────────────────────────┤
│  domain（领域层）                                │
│  领域规则与不变量                                 │
├─────────────────────────────────────────────────┤
│  infrastructure（基础设施层）                    │
│  JPA实体/仓库、缓存、加解密                       │
└─────────────────────────────────────────────────┘
```

### 功能模块

| 模块   | 名称           | 说明                                   |
| ------ | -------------- | -------------------------------------- |
| M01    | 统一认证       | 登录、令牌签发/续期/吊销、防重放        |
| M02    | 账号管理       | 账号建立、状态管理、批量导入           |
| M03    | 组织机构       | 多级树、节点移动、组织-数据范围映射     |
| M04    | 角色管理       | 角色定义、继承、互斥、复制、停用       |
| M05    | 功能权限       | 子系统/模块/功能点注册、角色-功能绑定   |
| M06    | 数据权限       | 数据范围定义、角色-数据范围绑定        |
| M07    | 授权管理       | 用户-角色绑定/解绑、授权前校验         |
| M08    | 策略决策(PDP)  | 功能权限判定 + 数据范围计算            |
| M09    | 审计日志       | 操作日志记录与查询                     |
| M10    | 通知           | 权限变更通知                           |
| M11    | 加解密         | 敏感字段加密存储与脱敏展示             |
| M12    | 接入管理       | 子系统注册、密钥管理                   |

## 目录结构

```
RBAC-Structured/
├── pom.xml                          # Maven 构建配置
├── src/main/
│   ├── java/com/iam/platform/
│   │   ├── IamApplication.java      # 启动类
│   │   ├── common/                  # 通用组件
│   │   │   ├── result/              # 统一响应封装
│   │   │   ├── enums/               # 枚举定义
│   │   │   └── exception/           # 异常处理
│   │   ├── application/             # 应用服务层（RBAC0 已实现，其余待实现）
│   │   │   ├── auth/                # M01 认证
│   │   │   ├── account/             # M02 账号
│   │   │   ├── organization/        # M03 组织
│   │   │   ├── role/                # M04 角色
│   │   │   ├── permission/          # M05/M06 功能/数据权限
│   │   │   ├── grant/               # M07 授权
│   │   │   ├── decision/            # M08 策略决策
│   │   │   └── audit/               # M09 审计
│   │   ├── interfaces/              # 接口层 + 认证鉴权拦截器
│   │   │   ├── admin/               # 管理端控制器
│   │   │   └── openapi/             # 开放API控制器
│   │   └── infrastructure/          # 基础设施层
│   │       ├── persistence/
│   │       │   ├── entity/          # JPA 实体（14张表）
│   │       │   └── repository/      # JPA 仓库
│   │       ├── crypto/              # 加解密组件
│   │       └── cache/               # 权限缓存
│   └── resources/
│       ├── application.yml          # 应用配置
│       └── db/schema.sql            # 建表脚本
│       └── db/seed.sql              # RBAC0 初始化数据（admin 账号、ADMIN 系统功能点）
└── design/                          # 设计文档
```

> **实现进度（RBAC0）**：登录/登出（M01）、账号（M02）、角色（M04，不含互斥/继承）、功能权限（M05）、用户授权（M07）、权限判定（M08）已实现，并有单元测试。
> 组织机构、数据权限、角色互斥、审计等仍为 `throw new UnsupportedOperationException("待实现")`，留待 RBAC1/RBAC3 阶段。
>
> **鉴权规则**：除 `POST /auth/login` 外所有接口需携带 `Authorization: Bearer <token>`；若该 API（Controller 映射模板 + HTTP 方法）已在 `iam_function_point` 中登记为 ADMIN 系统的功能点，还需当前用户拥有该功能点，否则返回 403。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 数据库准备

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE iam_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;"

# 2. 执行建表脚本
mysql -u root -p iam_platform < src/main/resources/db/schema.sql

# 3. 导入初始化数据（默认管理员 admin / Admin@123，上线后请修改）
mysql -u root -p iam_platform < src/main/resources/db/seed.sql
```

### 配置修改

编辑 `src/main/resources/application.yml`，修改数据库连接信息与 JWT 密钥：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/iam?useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

jwt:
  secret: your-256-bit-secret-key
```

### 构建与运行

```bash
# 编译（需 JDK 17 或 21；JDK 23+ 下 Lombok 会编译失败）
mvn clean compile

# 单元测试 + Jacoco 覆盖率报告（target/site/jacoco/index.html）
mvn clean test

# 打包
mvn clean package -DskipTests

# 运行
java -jar target/iam-platform-1.0.0-SNAPSHOT.jar
```

### 访问

- 管理端 API：`http://localhost:8080/api/v1/`
- 开放 API：`http://localhost:8080/api/v1/open/`

## API 概览

| 方法 | 路径                              | 说明             |
| ---- | --------------------------------- | ---------------- |
| POST | `/api/v1/auth/login`              | 登录             |
| POST | `/api/v1/auth/logout`             | 登出             |
| GET  | `/api/v1/users`                   | 用户列表         |
| POST | `/api/v1/users`                   | 创建用户         |
| PUT  | `/api/v1/users/{id}/status`       | 变更账号状态     |
| GET  | `/api/v1/orgs`                    | 组织树           |
| POST | `/api/v1/orgs`                    | 新建组织         |
| GET  | `/api/v1/roles`                   | 角色列表         |
| POST | `/api/v1/roles`                   | 新建角色         |
| POST | `/api/v1/roles/grants`            | 授予角色         |
| GET  | `/api/v1/open/authorize`          | 权限判定         |
| POST | `/api/v1/open/authorize/batch`    | 批量权限判定     |
| GET  | `/api/v1/open/permissions`        | 查询用户权限     |
| GET  | `/api/v1/audit-logs`              | 审计日志查询     |

## 设计文档

详细设计文档位于 `design/` 目录：

- `01_需求规格说明书.md` — 需求与用例
- `02_结构化设计.md` — 数据流图与模块分解
- `03_数据库设计.md` — 表结构与索引
- `04_API设计说明书.md` — REST API 规范
- `05_ER图实体说明.md` — ER 图与实体说明

## 开发约定

- **结构化编程**：方法内按线性流程编排，避免深层嵌套与多态分发
- **命名规范**：实体以 `Iam` 前缀，服务以 `Service` 后缀
- **加密存储**：姓名、手机号等敏感字段加密存储，哈希用于检索
- **软删除**：所有业务表使用 `is_deleted` 字段，不做物理删除
- **审计日志**：所有权限变更操作记录审计日志
