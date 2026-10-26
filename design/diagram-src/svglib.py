"""DFD / 结构图 SVG 生成小工具：与仓库现有 ER 图、DFD 图保持同一配色。"""
from xml.sax.saxutils import escape

FONT = "PingFang SC,Microsoft YaHei,SimHei,sans-serif"


class Svg:
    def __init__(self, w, h, title):
        self.w, self.h = w, h
        self.parts = [
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" font-family="{FONT}">',
            '<defs><marker id="arr" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">'
            '<path d="M0,0 L8,3 L0,6 Z" fill="#333"/></marker>'
            '<marker id="arrb" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth">'
            '<path d="M0,0 L8,3 L0,6 Z" fill="#1f4e8c"/></marker></defs>',
            f'<rect width="{w}" height="{h}" fill="#ffffff"/>',
            f'<text x="{w / 2}" y="42" text-anchor="middle" font-size="26" font-weight="bold">{escape(title)}</text>',
        ]

    def text(self, x, y, s, size=13, color="#b03030", anchor="middle", weight="normal", bg=True):
        if bg:  # 白底衬托，避免标签压线难读
            wpx = len(s) * size * 0.98
            x0 = x - wpx / 2 if anchor == "middle" else (x if anchor == "start" else x - wpx)
            self.parts.append(f'<rect x="{x0 - 3:.0f}" y="{y - size:.0f}" width="{wpx + 6:.0f}" height="{size + 5}" fill="#fff" opacity="0.9"/>')
        self.parts.append(f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-size="{size}" font-weight="{weight}" fill="{color}">{escape(s)}</text>')

    def entity(self, x, y, w, h, l1, l2="", dup=False):
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#fff" stroke="#222" stroke-width="1.8"/>')
        if dup:
            self.parts.append(f'<line x1="{x}" y1="{y + 14}" x2="{x + 14}" y2="{y}" stroke="#222" stroke-width="1.6"/>')
        cy = y + h / 2 + (-6 if l2 else 6)
        self.text(x + w / 2, cy, l1, 16, "#111", bg=False)
        if l2:
            self.text(x + w / 2, cy + 22, l2, 14, "#111", bg=False)

    def process(self, x, y, w, h, l1, l2="", dashed=False, faded=False):
        fill = "#f3f3f3" if faded else "#e8f1fc"
        dash = ' stroke-dasharray="8,5"' if dashed else ""
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="{fill}" stroke="#1f4e8c" stroke-width="1.8"{dash}/>')
        cy = y + h / 2 + (-4 if l2 else 6)
        self.text(x + w / 2, cy, l1, 17, "#13396b", weight="bold", bg=False)
        if l2:
            self.text(x + w / 2, cy + 24, l2, 13, "#33507a", bg=False)

    def store(self, x, y, w, l, dup=False, faded=False):
        fill = "#f3f3f3" if faded else "#fff7e0"
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="48" fill="{fill}" stroke="#8a6d1d" stroke-width="1.6"/>'
                          f'<line x1="{x + 18}" y1="{y}" x2="{x + 18}" y2="{y + 48}" stroke="#8a6d1d" stroke-width="1.6"/>')
        if dup:
            self.parts.append(f'<line x1="{x + 6}" y1="{y}" x2="{x + 6}" y2="{y + 48}" stroke="#8a6d1d" stroke-width="1.6"/>')
        self.text(x + 18 + (w - 18) / 2, y + 30, l, 13.5, "#5c4710", bg=False)

    def flow(self, pts, label="", lx=None, ly=None, color="#333", dashed=False):
        d = "M" + " L".join(f"{px},{py}" for px, py in pts)
        dash = ' stroke-dasharray="6,4"' if dashed else ""
        self.parts.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="1.4"{dash} marker-end="url(#arr)"/>')
        if label:
            if lx is None:  # 默认放在最长线段中点
                segs = list(zip(pts, pts[1:]))
                (ax, ay), (bx, by) = max(segs, key=lambda s: abs(s[1][0] - s[0][0]) + abs(s[1][1] - s[0][1]))
                lx, ly = (ax + bx) / 2, (ay + by) / 2 - 6
            self.text(lx, ly, label)

    def box(self, x, y, w, h, l1, l2="", fill="#e8f1fc", stroke="#1f4e8c", size=15):
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>')
        cy = y + h / 2 + (-3 if l2 else 5)
        self.text(x + w / 2, cy, l1, size, "#13396b", weight="bold", bg=False)
        if l2:
            self.text(x + w / 2, cy + 19, l2, 12.5, "#33507a", bg=False)

    def call(self, x1, y1, x2, y2):
        self.parts.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#1f4e8c" stroke-width="1.4" marker-end="url(#arrb)"/>')

    def raw(self, s):
        self.parts.append(s)

    def legend(self, y, items):
        x = 60
        for kind, label in items:
            if kind == "process":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" rx="4" fill="#e8f1fc" stroke="#1f4e8c"/>')
            elif kind == "faded":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" rx="4" fill="#f3f3f3" stroke="#1f4e8c" stroke-dasharray="4,3"/>')
            elif kind == "entity":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" fill="#fff" stroke="#222"/>')
            elif kind == "store":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" fill="#fff7e0" stroke="#8a6d1d"/>')
            elif kind == "line":
                self.raw(f'<line x1="{x}" y1="{y + 8}" x2="{x + 22}" y2="{y + 8}" stroke="#333" stroke-dasharray="6,4"/>')
            elif kind == "module":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" fill="#e8f1fc" stroke="#1f4e8c"/>')
            elif kind == "infra":
                self.raw(f'<rect x="{x}" y="{y}" width="22" height="15" fill="#fff7e0" stroke="#8a6d1d"/>')
            elif kind == "text":
                self.raw(f'<text x="{x}" y="{y + 13}" font-size="14" fill="#b03030">{escape(label)}</text>')
                x += len(label) * 14 + 40
                continue
            self.raw(f'<text x="{x + 30}" y="{y + 13}" font-size="14">{escape(label)}</text>')
            x += 30 + len(label) * 14 + 40

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(self.parts) + "</svg>\n")
