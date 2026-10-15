"""生成七种属性饰品 + 终极饰品的 16x16 占位贴图。

用户后续会自行重绘，这里只保证物品有可见贴图、不出现紫黑方块。
用法: python tools/gen_curio_tex.py
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "..", "src", "main", "resources", "assets",
                    "floral_rite_bloom", "textures", "item")

TRANSPARENT = (0, 0, 0, 0)

# 每种饰品: (轮廓色, 中间色, 高光色, 花梗色)
PALETTES = {
    # 霜华 - 寒风铃
    "curio_ice": ((126, 206, 235), (198, 240, 255), (255, 255, 255), (92, 126, 150)),
    # 焚尽 - 焚星藤
    "curio_flamm": ((198, 88, 32), (255, 168, 62), (255, 240, 190), (108, 62, 34)),
    # 云雨 - 天泪花
    "curio_current": ((64, 140, 210), (120, 196, 240), (232, 250, 255), (58, 96, 128)),
    # 自然 - 森语藤
    "curio_vitality": ((64, 124, 54), (118, 186, 84), (216, 244, 176), (74, 96, 44)),
    # 虚空 - 星界花
    "curio_void": ((76, 44, 118), (140, 92, 200), (226, 206, 255), (58, 44, 78)),
    # 枯萎 - 黑灵草
    "curio_wither": ((44, 44, 48), (86, 82, 88), (156, 148, 156), (56, 52, 46)),
    # 苍穹 - 天穹花
    "curio_sky": ((126, 196, 132), (186, 236, 168), (245, 255, 230), (104, 148, 112)),
    # 万花万草之灵
    "curio_eternal": ((186, 142, 38), (255, 214, 92), (255, 252, 226), (108, 96, 48)),
}

# 花朵轮廓（7x7，外圈 -> 内圈 -> 花心）
SHAPE = [
    "..OOO..",
    ".OMMMO.",
    "OMMHMMO",
    "OMHHHMO",
    "OMMHMMO",
    ".OMMMO.",
    "..OOO..",
]

# 终极饰品：更大的花冠，占满 9x9
SHAPE_ETERNAL = [
    "...OOO...",
    "..OMMMO..",
    ".OMMHMMO.",
    "OMMHHHMMO",
    "OMHHHHHMO",
    "OMMHHHMMO",
    ".OMMHMMO.",
    "..OMMMO..",
    "...OOO...",
]

STEM_X = 8


def paint(name, outline, mid, high, stem, shape):
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    px = img.load()

    # 花梗与两片叶子
    for y in range(6, 15):
        px[STEM_X, y] = stem
    for dx, y in ((-1, 10), (-2, 11), (1, 11), (2, 12)):
        px[STEM_X + dx, y] = stem

    size = len(shape)
    ox = (16 - size) // 2
    oy = 6 - size // 2
    for row, line in enumerate(shape):
        for col, ch in enumerate(line):
            if ch == ".":
                continue
            x, y = ox + col, oy + row
            if not (0 <= x < 16 and 0 <= y < 16):
                continue
            px[x, y] = high if ch == "H" else (mid if ch == "M" else outline)

    out = os.path.normpath(os.path.join(ROOT, name + ".png"))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print("wrote", out)


def main():
    for name, (outline, mid, high, stem) in PALETTES.items():
        paint(name, outline, mid, high, stem,
              SHAPE_ETERNAL if name == "curio_eternal" else SHAPE)


if __name__ == "__main__":
    main()