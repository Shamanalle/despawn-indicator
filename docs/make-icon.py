# Pixel-art mod icon on a 32x32 grid: a diamond lying on the ground inside a countdown ring that is running out,
# amber where time is left, dim where it is gone, with a red tip where it ends.
# Usage: python3 docs/make-icon.py docs/images/icon.png common/src/main/resources/assets/despawnindicator/icon.png (needs Pillow).
# Writes the 512px store icon and the 128px icon packed into the mod.
import math, sys
from PIL import Image

N = 32
BG, BORDER = (24, 22, 32), (54, 50, 66)
AMBER, RED, GONE = (245, 166, 35), (232, 72, 77), (62, 58, 76)
GEM, GEM_LIGHT, GEM_DARK, GEM_EDGE = (78, 214, 222), (196, 250, 252), (34, 150, 166), (18, 92, 108)
GROUND = (44, 40, 56)

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
px = img.load()

def put(x, y, c):
    px[x, y] = c + (255,)

# Rounded square with a 1px border
def inside(x, y, inset):
    r = 6 - inset
    lo, hi = inset, N - 1 - inset
    if not (lo <= x <= hi and lo <= y <= hi):
        return False
    cx = min(max(x, lo + r), hi - r)
    cy = min(max(y, lo + r), hi - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 1
for y in range(N):
    for x in range(N):
        if inside(x, y, 0):
            put(x, y, BORDER)
        if inside(x, y, 1):
            put(x, y, BG)

# Countdown ring, 2px thick, centre (15.5, 15.5). Time runs clockwise from 12 o'clock; 70% is left.
cx = cy = 15.5
left = 0.70
for y in range(N):
    for x in range(N):
        d = math.hypot(x - cx, y - cy)
        if 10.7 <= d <= 12.5:
            # 0 at 12 o'clock, growing clockwise
            a = (math.degrees(math.atan2(x - cx, cy - y)) + 360) % 360 / 360
            if a < 1 - left:
                c = GONE
            elif a < 1 - left + 0.05:
                c = RED
            else:
                c = AMBER
            put(x, y, c)

# The ground under the item
for x in range(10, 22):
    put(x, 21, GROUND)
for x in range(12, 20):
    put(x, 22, GROUND)

# A diamond, Minecraft-style: a faceted gem 10px wide
gem = [
    "...EEEEEE...",
    "..ELLGGGGE..",
    ".ELLGGGGGDE.",
    "EGGGGGGGGDDE",
    ".EGGGGGGDDE.",
    "..EGGGGDDE..",
    "...EGGDDE...",
    "....EGDE....",
    ".....EE.....",
]
colors = {"E": GEM_EDGE, "L": GEM_LIGHT, "G": GEM, "D": GEM_DARK}
for j, row in enumerate(gem):
    for i, ch in enumerate(row):
        if ch in colors:
            put(10 + i, 11 + j, colors[ch])

for size, out in ((512, sys.argv[1]), (128, sys.argv[2])):
    img.resize((size, size), Image.NEAREST).save(out, "PNG", optimize=True)
