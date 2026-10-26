import sys
from svglib import Svg

s = Svg(2400, 1180, "RBAC0 后端模块结构图（由 2 级数据流图经事务分析 / 变换分析导出）")

INFRA = dict(fill="#fff7e0", stroke="#8a6d1d")


def b(x, y, w, l1, l2="", h=64, **kw):
    s.box(x, y, w, h, l1, l2, **kw)
    return (x + w / 2, y, y + h)


def link(parent, child, couple="", dx=0):
    px, _, pb = parent
    cx, ct, _ = child
    s.call(px + dx, pb, cx, ct)
    if couple:
        s.text((px + dx + cx) / 2 + 8, (pb + ct) / 2 + 4, couple, 12, "#555", anchor="start")


# ---------- 左：事务型（P2 账号与授权管理） ----------
s.text(600, 92, "① 事务分析：P2 账号与授权管理（按请求类型分派）", 17, "#13396b", weight="bold", bg=False)
top = b(390, 120, 420, "事务中心：管理端请求分派", "UserController / RoleController / SystemController", h=70)
tx = [
    ("建立账号", "createUser"), ("变更状态/封存", "changeStatus"), ("授予角色", "grantRole"),
    ("撤销角色", "revokeRole"), ("绑定/解绑功能点", "bind/unbindRoleFunctions"), ("新建角色", "createRole"),
    ("登记系统/模块/功能点", "register*"),
]
couples = ["账号信息↓ 账号↑", "userId,状态↓", "userId,roleId↓", "userId,roleId↓", "roleId,功能点ID↓", "角色信息↓", "编码,名称↓"]
txn = []
for i, ((n, m), c) in enumerate(zip(tx, couples)):
    node = b(30 + i * 168, 300, 156, n, m, h=64, size=14)
    txn.append(node)
    link(top, node, "")
for i, node in enumerate(txn):  # 数据耦合标注放在事务框上方
    s.text(node[0], 290, couples[i], 11.5, "#555")

# changeStatus 的下层模块
c1 = b(110, 450, 220, "吊销全部令牌", "AuthService.revokeUserTokens", size=14)
c2 = b(350, 450, 220, "回收全部角色", "GrantService.revokeAllRoles", size=14)
link(txn[1], c1, "userId↓")
link(txn[1], c2, "userId↓", dx=30)

# 登录（变换型小结构）
s.text(600, 620, "② P1 登录认证", 17, "#13396b", weight="bold", bg=False)
lg = b(390, 650, 420, "登录主控", "AuthController.login → AuthService.login", h=70)
lc = [b(80, 820, 220, "核对密码", "CryptoComponent.verifyPassword", size=14),
      b(320, 820, 200, "校验账号状态", "validateAccountStatus", size=14),
      b(540, 820, 200, "签发令牌", "generateToken(JWT)", size=14),
      b(760, 820, 200, "记录令牌", "saveToken → D5", size=14)]
for node, c in zip(lc, ["密码,哈希↓ 是否匹配↑", "状态↓ 异常↑", "userId↓ 令牌↑", "令牌哈希↓"]):
    link(lg, node, "")
    s.text(node[0], 810, c, 11.5, "#555")

# ---------- 右：变换型（P3 鉴权决策） ----------
s.text(1780, 92, "③ 变换分析：P3 鉴权决策（输入 → 变换 → 输出）", 17, "#13396b", weight="bold", bg=False)
mt = b(1540, 120, 480, "鉴权主控", "AuthInterceptor.preHandle（每个请求）", h=70)
inp = b(1240, 300, 250, "输入：令牌认证", "AuthService.authenticate", size=14)
loc = b(1510, 300, 250, "变换：定位功能点", "FunctionPermissionService.findByApi", size=14)
dec = b(1780, 300, 250, "变换：功能权限判定", "DecisionService.hasPermission", size=14)
out = b(2050, 300, 250, "输出：放行 / 401 / 403", "BusinessException → 全局异常处理", size=14)
for node, c in [(inp, "令牌↓ userId↑"), (loc, "API模板,方法↓ 功能点↑"), (dec, "userId,系统,功能点↓ 是否允许↑"), (out, "判定结果↓")]:
    link(mt, node, "")
    s.text(node[0], 290, c, 11.5, "#555")

gf = b(1780, 470, 250, "计算功能点集合", "getUserFunctionPoints", size=14)
link(dec, gf, "userId,系统↓ 功能点集合↑")
steps = ["① 账号为正常状态", "② 有效的用户-角色绑定", "③ 过滤停用角色", "④ 取角色绑定的功能点", "⑤ 只保留本系统有效功能点"]
for i, st in enumerate(steps):
    node = b(1240 + i * 214, 640, 200, st, "", h=56, size=13.5)
    link(gf, node, "")

s.text(1780, 760, "开放接口 OpenApiController 直接调用 hasPermission / checkPermissions / getUserFunctionPoints，复用同一判定逻辑（FR-38）", 13, "#33507a", bg=False)

# ---------- 底部：公共下层模块 ----------
s.raw('<line x1="40" y1="960" x2="2360" y2="960" stroke="#bbb" stroke-dasharray="6,4"/>')
s.text(1200, 990, "公共下层模块（被上面多数模块调用，为减少连线未逐一画出）", 15, "#5c4710", weight="bold", bg=False)
b(120, 1010, 560, "数据访问：JPA Repository", "IamUser/Role/UserRole/RoleFunction/FunctionPoint/Module/System/Token", **INFRA)
b(700, 1010, 360, "加密脱敏：CryptoComponent", "AES-GCM 加解密 / PBKDF2 / SHA-256 / 脱敏", **INFRA)
b(1140, 1010, 360, "统一异常与响应", "BusinessException / GlobalExceptionHandler / Result", **INFRA)
b(1580, 1010, 360, "状态常量", "RoleService.ROLE_ACTIVE / GrantService.GRANT_ACTIVE", **INFRA)

s.legend(1130, [("module", "模块"), ("infra", "公共下层模块"), ("text", "箭头表示调用；灰字为模块间传递的数据（↓ 传入，↑ 返回），耦合类型分析见正文")])
s.save(sys.argv[1])
