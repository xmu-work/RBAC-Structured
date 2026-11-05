import sys
from svglib import Svg


W, H = 1800, 1150
s = Svg(W, H, "统一应用软件权限管理系统 — RBAC 核心 E-R 图")

ENTITY_FILL = "#eaf2fd"
ENTITY_STROKE = "#1f4e8c"
REL_FILL = "#fff4cf"
REL_STROKE = "#9a7a22"


def entity(x, y, w, h, title, table, fields):
    s.raw(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="#fff" stroke="{ENTITY_STROKE}" stroke-width="2"/>')
    s.raw(f'<path d="M{x + 8},{y} H{x + w - 8} Q{x + w},{y} {x + w},{y + 8} V{y + 48} H{x} V{y + 8} Q{x},{y} {x + 8},{y} Z" fill="{ENTITY_FILL}" stroke="{ENTITY_STROKE}" stroke-width="2"/>')
    s.raw(f'<text x="{x + 16}" y="{y + 22}" font-size="18" font-weight="bold" fill="#173b6b">{title}</text>')
    s.raw(f'<text x="{x + 16}" y="{y + 41}" font-size="12" fill="#49678f">{table}</text>')
    for i, field in enumerate(fields):
        fy = y + 72 + i * 24
        s.raw(f'<text x="{x + 16}" y="{fy}" font-size="13" fill="#202a36">{field}</text>')


def diamond(cx, cy, text, sub="", w=120, h=70):
    pts = f"{cx},{cy - h / 2} {cx + w / 2},{cy} {cx},{cy + h / 2} {cx - w / 2},{cy}"
    s.raw(f'<polygon points="{pts}" fill="{REL_FILL}" stroke="{REL_STROKE}" stroke-width="2"/>')
    ty = cy - 1 if not sub else cy - 7
    s.raw(f'<text x="{cx}" y="{ty}" text-anchor="middle" font-size="14" font-weight="bold" fill="#5e4810">{text}</text>')
    if sub:
        s.raw(f'<text x="{cx}" y="{cy + 14}" text-anchor="middle" font-size="11" fill="#755c16">{sub}</text>')


def path(points, dashed=False, color="#394b61"):
    d = " ".join(("M" if i == 0 else "L") + f"{x},{y}" for i, (x, y) in enumerate(points))
    dash = ' stroke-dasharray="7,5"' if dashed else ""
    s.raw(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="2"{dash}/>')


def card(x, y, text, color="#b03030"):
    s.raw(f'<rect x="{x - 14}" y="{y - 14}" width="28" height="20" rx="5" fill="#fff" stroke="none"/>')
    s.raw(f'<text x="{x}" y="{y}" text-anchor="middle" font-size="15" font-weight="bold" fill="{color}">{text}</text>')


# Relationships are drawn first so entity cards remain visually on top.
# 多系统 1:N 前端权限
path([(250, 205), (700, 255)])
diamond(520, 205, "定义", "系统拥有权限")
card(445, 192, "1")
card(650, 195, "N")

# 多系统 1:N 后端 API
path([(250, 150), (250, 75), (1440, 75), (1440, 190)])
diamond(1100, 75, "拥有", "系统组件", w=130)
card(320, 62, "1")
card(1530, 120, "N")

# 前端权限 1:N 后端 API
path([(1080, 310), (1130, 310), (1130, 300), (1340, 300)])
diamond(1130, 310, "映射", "权限→API", w=130)
card(1100, 282, "1")
card(1320, 282, "N")

# 前端权限树自关联
path([(890, 220), (890, 130), (1010, 130), (1010, 220)], color="#6683a6")
s.raw('<path d="M1010,220 l-8,-13 M1010,220 l8,-13" fill="none" stroke="#6683a6" stroke-width="2"/>')
card(950, 112, "1 : N")
s.raw('<text x="950" y="155" text-anchor="middle" font-size="12" fill="#49678f">父子菜单 / 按钮</text>')

# 角色 M:N 前端权限
path([(870, 360), (870, 440), (870, 520)])
diamond(870, 480, "授权", "iam_role_permission", w=170, h=60)
card(940, 462, "M")
card(940, 505, "N")

# 用户 M:N 角色
path([(820, 820), (870, 690)])
diamond(845, 755, "分配", "iam_user_role", w=150, h=74)
card(805, 815, "M")
card(870, 700, "N")

# 组织 1:N 用户
path([(460, 900), (650, 900)])
diamond(550, 900, "隶属于", "组织→用户", w=130)
card(500, 878, "1")
card(650, 878, "N")

# Six business entities.
entity(80, 130, 340, 190, "多系统", "iam_system", [
    "PK  id",
    "UK  system_code",
    "     system_name",
    "     status / callback_url",
])

entity(700, 220, 380, 220, "权限（前端菜单/按钮树）", "iam_permission", [
    "PK  id",
    "FK  system_id  → iam_system",
    "FK  parent_id  → iam_permission（自关联）",
    "UK  system_id + permission_code",
    "     permission_type：MENU / BUTTON",
    "     route_path / action / sort_no",
])

entity(1340, 190, 360, 220, "API（后端接口）", "iam_api", [
    "PK  id",
    "FK  system_id    → iam_system",
    "FK  permission_id → iam_permission",
    "UK  system_id + api_code",
    "UK  api_path + http_method",
    "     api_name / status",
])

entity(700, 520, 340, 170, "角色", "iam_role", [
    "PK  id",
    "UK  role_code",
    "     role_name / status",
    "     parent_id（角色继承，可选）",
])

entity(70, 780, 390, 210, "分层组织部门", "iam_org", [
    "PK  id",
    "FK  parent_id → iam_org（自关联）",
    "UK  org_code",
    "     org_name / org_type / org_level",
    "     ancestors：分层路径",
])

entity(650, 820, 360, 190, "用户", "iam_user", [
    "PK  id",
    "UK  user_no",
    "FK  org_id → iam_org",
    "     name_enc / phone_enc（加密）",
    "     account_status / post",
])

# Legend and design notes.
s.raw('<line x1="60" y1="1060" x2="1740" y2="1060" stroke="#c9d1da" stroke-width="1"/>')
s.raw(f'<polygon points="80,1085 110,1072 140,1085 110,1098" fill="{REL_FILL}" stroke="{REL_STROKE}" stroke-width="2"/>')
s.raw('<text x="155" y="1090" font-size="13" fill="#303946">联系（关联表不再画成独立实体）</text>')
s.raw(f'<rect x="430" y="1071" width="28" height="24" rx="5" fill="#fff" stroke="{ENTITY_STROKE}" stroke-width="2"/>')
s.raw('<text x="475" y="1090" font-size="13" fill="#303946">六个业务实体</text>')
s.raw('<text x="660" y="1090" font-size="13" fill="#8a2b2b">1 / N / M 为参与基数</text>')
s.raw('<text x="930" y="1090" font-size="13" fill="#303946">鉴权链：请求 API → 所属权限 → 用户角色授予的权限；前端隐藏菜单不能替代后端 API 校验</text>')

s.save(sys.argv[1])
