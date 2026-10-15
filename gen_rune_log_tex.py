"""Rune log textures, 32x32, modelled on vanilla OAK log.

Outputs:
  rune_log_top.png   growth rings (up + down faces)
  rune_log_side.png  irregular vertical bark grooves + short cracks

Vanilla oak reference:
  - top: ~5 dark ring lines on a light wood ground; pith dark at centre,
    tones get lighter outward; square border and corners are bark
  - side: vertical dark grooves at IRREGULAR columns (2-3px wide), lighter
    ridge highlights beside them, occasional short horizontal cracks;
    never uniform picket-fence spacing, never random speckle noise

All patterns are deterministic per pixel. Run: python gen_rune_log_tex.py
"""
import math
import struct
import zlib

W = H = 32
CX = CY = 15.5

# ----------------------------------------------------------------- colours --
# Top face wood: pith dark -> outer rings lighter (vanilla oak gradient).
T_PITH = (105, 74, 40)
T_LINE = (120, 86, 48)   # dark ring line
T_Z1 = (146, 108, 62)
T_Z2 = (154, 116, 68)
T_Z3 = (162, 124, 74)
T_Z4 = (170, 132, 80)
T_Z5 = (178, 142, 88)
T_SEP = (98, 68, 38)     # dark separator between wood disc and bark rim

# Bark (shared by top rim and side face).
B_MID = (125, 90, 50)
B_DARK = (92, 64, 34)
B_LIGHT = (150, 112, 66)
B_FAINT = (133, 97, 55)

RING_BOUNDS = [1.6, 4.2, 6.8, 9.4, 12.0]
RING_ZONES = [T_Z1, T_Z2, T_Z3, T_Z4, T_Z5]
DISC_R = 13.2


def h2(x, y, seed=0):
    """Deterministic 0..99 hash for texture variation."""
    return (x * 7349 + y * 1571 + seed * 37 + x * y * 13) % 100


def write_png(px, path):
    raw = bytearray()
    for y in range(H):
        raw.append(0)  # filter type 0
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


def bark_speckle(x, y):
    v = h2(x, y, 3)
    if v % 7 == 0:
        return B_DARK
    if v % 9 == 0:
        return B_LIGHT
    return B_MID


def ring_face():
    px = []
    for y in range(H):
        row = []
        for x in range(W):
            dx, dy = x - CX, (y - CY) * 0.96  # very slight ellipse
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            wob = 0.7 * math.sin(3 * a + 1.2) + 0.4 * math.sin(5 * a + 0.5)
            re = r + wob
            if re > DISC_R + 0.7:
                row.append(bark_speckle(x, y))
            elif re > DISC_R - 0.3:
                row.append(T_SEP)
            elif re < RING_BOUNDS[0]:
                row.append(T_PITH)
            else:
                on_line = any(abs(re - b) < 0.55 for b in RING_BOUNDS[1:])
                if on_line:
                    row.append(T_LINE)
                else:
                    zi = len(RING_ZONES) - 1
                    for i, b in enumerate(RING_BOUNDS[1:]):
                        if re < b:
                            zi = i
                            break
                    row.append(RING_ZONES[zi])
        px.append(row)
    return px


# Groove columns: (x, width) -- irregular spacing like vanilla oak side.
GROOVES = [(1, 2), (5, 3), (10, 2), (13, 2), (17, 3), (22, 2), (26, 3), (30, 2)]
# Short horizontal cracks: (y, start_x, length).
CRACKS = [(6, 11, 3), (6, 24, 2), (13, 2, 3), (13, 18, 4),
          (21, 7, 2), (21, 27, 3), (28, 14, 3), (28, 25, 2)]


def side_face():
    px = []
    for y in range(H):
        row = []
        for x in range(W):
            c = B_MID
            v = h2(x, y, 1)
            if v % 13 == 0:
                c = B_FAINT
            elif v % 17 == 0:
                c = B_LIGHT
            row.append(c)
        px.append(row)

    # grooves: dark columns 2-3px wide, wobble sideways on some rows,
    # with a highlight ridge just right of each groove
    for gx, gw in GROOVES:
        for y in range(H):
            shift = 0
            v = h2(gx, y, 2)
            if v % 11 == 0:
                shift = 1
            elif v % 13 == 0:
                shift = -1
            for i in range(gw):
                x = gx + i + shift
                if 0 <= x < W:
                    px[y][x] = B_DARK
                    if h2(x, y, 4) % 8 == 0:
                        px[y][x] = B_MID  # broken groove = texture
            rx = gx + gw + shift
            if 0 <= rx < W and h2(rx, y, 6) % 3:
                px[y][rx] = B_LIGHT
            lx = gx - 1 + shift
            if 0 <= lx < W and h2(lx, y, 8) % 5 == 0:
                px[y][lx] = B_LIGHT

    # horizontal cracks crossing grain
    for cy, cs, cl in CRACKS:
        for i in range(cl):
            x = cs + i
            if 0 <= x < W:
                px[cy][x] = B_DARK if i < cl - 1 else B_FAINT
    return px


def preview(px, title, palette):
    print(title)
    for row in px:
        print("  " + "".join(palette.get(c, "?") for c in row))
    print()


top = ring_face()
write_png(top, r"D:\Mods_create\mod_1yuwb1l\rune_log_top.png")
preview(top, "rune_log_top (oak, 32x32)", {
    T_PITH: "c", T_LINE: "-", T_SEP: "#",
    T_Z1: "1", T_Z2: "2", T_Z3: "3", T_Z4: "4", T_Z5: "5",
    B_MID: "b", B_DARK: "B", B_LIGHT: "l", B_FAINT: ".",
})

side = side_face()
write_png(side, r"D:\Mods_create\mod_1yuwb1l\rune_log_side.png")
preview(side, "rune_log_side (oak, 32x32)", {
    B_MID: ".", B_FAINT: "f", B_DARK: "#", B_LIGHT: "L",
})

print("OK: rune_log_top.png, rune_log_side.png (oak 32x32)")
