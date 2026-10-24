# 统一权限平台 · 管理后台前端

依据《前端工程契约与模块设计说明书》V2.1（`design/06_前端契约与模块设计.md`）完成前端框架搭建。

## 技术栈

Vue 3（仅 `<script setup>`）+ TypeScript(strict) + Vite 5 + Pinia + Vue Router 4 + Element Plus + Axios + pnpm

## 常用命令

```bash
pnpm install      # 安装依赖（lock 文件入库）
pnpm dev          # 启动开发服务（默认 5173，Mock 已开启）
pnpm build        # vue-tsc 类型检查 + 生产构建
pnpm typecheck    # 仅类型检查
pnpm lint         # ESLint + Stylelint
```

## 已完成的框架能力

- 登录、会话恢复、登出与未授权跳转
- 按权限过滤的动态路由、侧边菜单、`v-perm` 元素级权限指令与 60 秒权限同步
- 主布局、顶栏、面包屑、多页签、403/404/500 页面
- Pinia 状态层、Axios 契约封装、完整 API/类型目录、分页组合式函数与字典 Store
- 登录、权限与数据范围字典的契约级 Mock

当前业务模块使用占位页接入路由，领域页面与接口联调不在本次框架搭建范围内。

## Mock / 联调切换

- `web/.env.development` 中 `VITE_USE_MOCK=true`：vite-plugin-mock 按契约附录 B 签名拦截 `/api/v1/*` 并返回契约级数据。
- Mock 模式下，登录页输入任意非空账号和密码即可进入管理后台。
- 后端就绪后置 `false`：请求经 Vite proxy 至 `http://localhost:8080`（含契约 D1 双前缀的临时 rewrite 兜底，后端修复后移除）。

## 目录契约（契约 2.1）

`api / types / stores / router / directives / composables / layout / views / utils / constants / styles`

## UI 实现三原则（契约 9.1）

1. 布局容器（grid/flex/间距）与视觉组件（颜色/圆角/阴影/字体）分离
2. 设计令牌先行：`src/styles/tokens.css` 为唯一视觉来源，禁止硬编码色值与 px
3. 一个组件只渲染一个独立视觉区块

## 待办

- husky + lint-staged 提交即检（契约第 12 章，后续接入）
- 后端修复 D1 双前缀后移除 vite.config.ts 中的临时 rewrite
- 后端登记 `ADMIN` 系统及菜单功能点后，将 `src/constants/permissions.ts` 中的占位 ID 替换为真实主键
