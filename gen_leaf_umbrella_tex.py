"""Leaf umbrella textures, 32x32, vanilla-style.

Outputs:
  leaf_canopy_top.png   radial veins + light-centre/dark-edge green (canopy up faces)
  leaf_canopy_side.png  folded leaf-edge band (canopy sides + underside)
  leaf_stem.png         vertical fibres + node rings (handle)

Deterministic per-pixel, stdlib only. Run: python gen_leaf_umbrella_tex.py
"""
import math
import struct
import zlib

W = H = 32
CX = CY = 15.5

# ----------------------------------------------------------------- colours --
# Canopy top: pale at centre -> deep green at the rim, pale veins.
C_CTR = (120, 186, 96)
C_MID = (66, 148, 66)
C_OUT = (40, 116, 48)
C_RIM = (24, 88, 38)
VEIN = (176, 216, 128)
SPECK = (52, 128, 56)

# Canopy side / underside: darker leaf back.
S_TOP = (86, 158, 76)   # folded rim highlight (top 2 rows)
S_BASE = (46, 122, 52)
S_DARK = (30, 92, 40)
S_VEIN = (120, 176, 96)

# Stem fibres.
T_BASE = (128, 150, 86)
T_DARK = (96, 116, 64)
T_LIGHT = (158, 178, 110)
T_NODE = (84, 104, 58)


def h2(x, y, seed=0):
    return (x * 7349 + y * 1571 + seed * 37 + x * y * 13) % 100


def write_png(px, path):
    raw = bytearray()
    for y in range(H):
        raw.append(0)
        for x in range(W):
            raw.extend(px[y][x])

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def canopy_top():
    px = []
    for y in range(H):
        row = []
        for x in range(W):
            dx, dy = x - CX, y - CY
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            if r > 15.2:
                row.append(C_RIM)
                continue
            # radial zones: pale centre -> deep rim
            if r < 4:
                c = C_CTR
            elif r < 9:
                c = C_MID
            else:
                c = C_OUT
            # 8 veins every 45 deg, widening outward
            k = a % (math.pi / 4)
            d = min(k, math.pi / 4 - k)
            if d < 0.045 + r * 0.0035:
                c = VEIN
            # centre node
            if r < 1.6:
                c = VEIN
            if h2(x, y, 1) % 19 == 0 and c not in (VEIN, C_RIM):
                c = SPECK
            row.append(c)
        px.append(row)
    return px


def canopy_side():
    px = []
    for y in range(H):
        row = []
        for x in range(W):
            if y < 2:
                c = S_TOP                      # folded rim
            elif y == H - 1:
                c = S_DARK                     # bottom shadow line
            else:
                c = S_BASE
                if x % 6 in (0, 1):
                    c = S_DARK                 # vein stubs hanging down
                elif x % 6 == 2 and h2(x, y, 2) % 3:
                    c = S_VEIN
                if h2(x, y, 3) % 17 == 0:
                    c = S_DARK
            row.append(c)
        px.append(row)
    return px


def stem():
    px = []
    for y in range(H):
        row = []
        for x in range(W):
            c = T_BASE
            if x % 5 == 0:
                c = T_DARK
            elif x % 7 == 3:
                c = T_LIGHT
            if h2(x, y, 4) % 23 == 0:
                c = T_DARK
            row.append(c)
        px.append(row)
    for ny in (3, 11, 19, 27):                 # node rings
        for y in (ny, ny + 1):
            for x in range(W):
                px[y][x] = T_NODE if x % 4 else T_DARK
    return px


def preview(px, title, palette):
    print(title)
    for row in px:
        print("  " + "".join(palette.get(c, "?") for c in row))
    print()


top = canopy_top()
write_png(top, r"D:\Mods_create\mod_1yuwb1l\leaf_canopy_top.png")
preview(top, "leaf_canopy_top", {
    C_CTR: "o", C_MID: "m", C_OUT: "M", C_RIM: "#", VEIN: "*", SPECK: ".",
})

side = canopy_side()
write_png(side, r"D:\Mods_create\mod_1yuwb1l\leaf_canopy_side.png")
preview(side, "leaf_canopy_side", {
    S_TOP: "=", S_BASE: "m", S_DARK: "#", S_VEIN: "v",
})

st = stem()
write_png(st, r"D:\Mods_create\mod_1yuwb1l\leaf_stem.png")
preview(st, "leaf_stem", {
    T_BASE: ".", T_DARK: "#", T_LIGHT: "L", T_NODE: "=",
})

print("OK: leaf_canopy_top.png, leaf_canopy_side.png, leaf_stem.png")
