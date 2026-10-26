import sys
from svglib import Svg

ROW, TOP = 270, 90
s = Svg(2320, TOP + 5 * ROW + 90, "P2 账号与授权管理 — 2级数据流图")

# 左侧通栏：E2；右侧通栏：父图中的 P5、P4（虚线框表示本图边界外的加工）
s.entity(40, TOP + 40, 170, 5 * ROW - 60, "E2 管理员", "")
s.process(1830, TOP + 40 + ROW, 200, 4 * ROW - 60, "P5", "变更通知", dashed=True)
s.process(2080, TOP + 40, 200, 5 * ROW - 60, "P4", "日志审计", dashed=True)

rows = [
    ("P2.1 建立账号", "AccountService.createUser", "建账号请求", False),
    ("P2.2 变更账号状态/封存", "AccountService.changeStatus", "状态变更请求", False),
    ("P2.3 授予/撤销用户角色", "GrantService.grantRole/revokeRole", "授予/撤销请求", False),
    ("P2.4 绑定/解绑角色功能点", "FunctionPermissionService.bind/unbind", "绑定/解绑请求", False),
    ("P2.5 数据范围授权（RBAC3）", "DataPermissionService（待实现）", "数据范围授权请求", True),
]
log_labels = ["账号日志", "封存日志", "授权日志", "绑定日志", "授权日志"]

for i, (name, impl, req, later) in enumerate(rows):
    y0 = TOP + i * ROW
    px, py, pw, ph = 600, y0 + 80, 320, 110
    s.process(px, py, pw, ph, name, impl, faded=later, dashed=later)
    dashed = later
    s.flow([(210, py + 30), (px, py + 30)], req, 400, py + 22, dashed=dashed)
    s.flow([(px, py + 75), (210, py + 75)], "操作结果", 400, py + 93, dashed=dashed)
    # 右侧：日志与变更事件
    s.flow([(px + pw, py + 40), (2080, py + 40)], log_labels[i], 1990, py + 32, dashed=dashed)
    if i > 0:
        s.flow([(px + pw, py + 75), (1830, py + 75)], "变更事件", 1740, py + 67, dashed=dashed)

def st(x, y, label, w=250):
    s.store(x, y, w, label, dup=True)

# 行 1：P2.1 建立账号
y0 = TOP
s.entity(330, y0 + 0, 170, 60, "E4 HR系统", "", dup=True)
s.flow([(470, y0 + 60), (630, y0 + 80)], "入职事件（扩展）", 560, y0 + 58, dashed=True)
st(1000, y0 + 0, "D1 账号库")
s.flow([(1030, y0 + 48), (920, y0 + 90)], "账号是否已存在", 1010, y0 + 66)
s.flow([(920, y0 + 108), (1220, y0 + 48)], "账号记录", 1250, y0 + 92)

# 行 2：P2.2 变更账号状态/封存
y0 = TOP + ROW
s.entity(330, y0 + 0, 170, 60, "E4 HR系统", "", dup=True)
s.flow([(470, y0 + 60), (630, y0 + 80)], "离职/调岗事件（扩展）", 560, y0 + 58, dashed=True)
st(1000, y0 + 0, "D1 账号库")
s.flow([(1030, y0 + 48), (920, y0 + 90)], "当前账号状态", 1000, y0 + 66)
s.flow([(920, y0 + 108), (1220, y0 + 48)], "新账号状态", 1250, y0 + 92)
st(1450, y0 + 0, "D5 令牌库")
s.flow([(920, y0 + 84), (1460, y0 + 48)], "吊销令牌", 1500, y0 + 72)
st(1000, y0 + 222, "D3 角色权限库")
s.flow([(860, y0 + 190), (1040, y0 + 222)], "回收用户角色（封存时）", 1180, y0 + 212)

# 行 3：P2.3 授予/撤销用户角色
y0 = TOP + 2 * ROW
st(1350, y0 + 0, "D1 账号库")
s.flow([(1380, y0 + 48), (920, y0 + 92)], "账号状态", 1120, y0 + 64)
st(1000, y0 + 222, "D3 角色权限库")
s.flow([(1030, y0 + 222), (760, y0 + 190)], "角色状态/已有绑定", 820, y0 + 220)
s.flow([(880, y0 + 190), (1220, y0 + 222)], "用户-角色绑定", 1350, y0 + 214)

# 行 4：P2.4 绑定/解绑角色功能点
y0 = TOP + 3 * ROW
st(1000, y0 + 222, "D3 角色权限库")
s.flow([(1030, y0 + 222), (760, y0 + 190)], "角色/功能点", 820, y0 + 220)
s.flow([(880, y0 + 190), (1220, y0 + 222)], "角色-功能点绑定", 1360, y0 + 214)

# 行 5：P2.5 数据范围授权
y0 = TOP + 4 * ROW
st(1000, y0 + 222, "D4 数据权限库")
s.flow([(880, y0 + 190), (1220, y0 + 222)], "角色-数据范围授权", 1370, y0 + 214, dashed=True)

s.legend(TOP + 5 * ROW + 50, [("process", "子加工"), ("faded", "后续阶段实现"), ("entity", "外部实体"), ("store", "数据存储"),
                               ("text", "左上角带斜线/双竖线为重复绘制；虚线框 P4、P5 为父图中的其他加工")])
s.save(sys.argv[1])
