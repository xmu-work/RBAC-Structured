#!/bin/zsh
# 用法：render.sh 输入.svg 输出.png   （按 SVG 尺寸 2 倍导出 PNG）
set -e
svg=$1; png=$2
dir=$(mktemp -d)
cp "$svg" "$dir/in.svg"
w=$(grep -o 'width="[0-9]*"' "$svg" | head -1 | grep -o '[0-9]*')
h=$(grep -o 'height="[0-9]*"' "$svg" | head -1 | grep -o '[0-9]*')
print "<!doctype html><html><head><meta charset=utf-8><style>html,body{margin:0;overflow:hidden}img{display:block}</style></head><body><img src=in.svg width=$w height=$h></body></html>" > "$dir/p.html"
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars \
  --user-data-dir="$dir/prof" --force-device-scale-factor=2 --window-size=$w,$h \
  --screenshot="$dir/out.png" "file://$dir/p.html" >/dev/null 2>&1 &
pid=$!
for i in {1..60}; do [[ -s "$dir/out.png" ]] && break; sleep 0.5; done
sleep 1; kill $pid 2>/dev/null || true; pkill -f "$dir/prof" 2>/dev/null || true
cp "$dir/out.png" "$png"
rm -rf "$dir"
