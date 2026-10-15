"""Ritual stone textures: veined marble with a glowing rune.

Outputs two 16x16 vanilla-style pixel textures:
  ritual_stone.png       plain marble (block bottom face)
  ritual_stone_rune.png  marble + glowing teal rune (top + four side faces)

Rune layout (exact pixel geometry, centred on 7.5/7.5):
  - mid-teal outer ring (radius ~5.5), touches the inner diamond at top/bottom
  - bright-teal diamond, dark-teal engraved lip, four corner sparks
  - white-hot 2x2 core, sparse faint halo pixels bleeding into the marble

Run: python gen_marble_tex.py
"""
import struct
import zlib

W = H = 16

# ----------------------------------------------------------------- colours --
M_W = (243, 243, 244)  # marble base white
M_S = (228, 228, 231)  # pale mineral chip
M_M = (205, 205, 210)  # mid chip
M_D = (180, 180, 187)  # rare dark chip

R_HALO = (204, 239, 238)  # faint teal halo
R_EDGE = (22, 84, 92)     # dark teal engraved lip
R_RING = (52, 184, 196)   # outer ring
R_LINE = (107, 240, 232)  # inner diamond
R_DOT = (190, 255, 250)   # corner sparks
R_CORE = (255, 255, 255)  # white-hot core

# Marble chips authored as coordinates so no row can drift out of width.
# Kept around the rim; the rune area stays calm white stone.
CHIPS_S = {
    (1, 0), (5, 0), (12, 0), (0, 1), (15, 1),
    (14, 2), (0, 3), (15, 4), (0, 6), (15, 7), (1, 8),
    (15, 9), (0, 10), (15, 11), (0, 13),
    (2, 14), (6, 14), (11, 14), (14, 14),
    (1, 15), (5, 15), (9, 15), (13, 15),
}
CHIPS_M = {
    (8, 0), (3, 1), (4, 1), (12, 1), (13, 1),
    (1, 14), (9, 14), (15, 15),
}
CHIPS_D = {(1, 4), (0, 9), (14, 10)}

# Glyph coordinates, from the exact ring/diamond geometry.
RING = {
    (6, 2), (7, 2), (8, 2), (9, 2),
    (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (11, 3),
    (3, 4), (4, 4), (11, 4), (12, 4),
    (2, 5), (3, 5), (12, 5), (13, 5),
    (2, 6), (3, 6), (12, 6), (13, 6),
    (2, 7), (3, 7), (12, 7), (13, 7),
    (2, 8), (3, 8), (12, 8), (13, 8),
    (2, 9), (3, 9), (12, 9), (13, 9),
    (2, 10), (3, 10), (12, 10), (13, 10),
    (3, 11), (4, 11), (11, 11), (12, 11),
    (4, 12), (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12), (11, 12),
    (6, 13), (7, 13), (8, 13), (9, 13),
}
DIAMOND = {
    (7, 4), (8, 4),
    (6, 5), (9, 5),
    (5, 6), (10, 6),
    (4, 7), (11, 7),
    (4, 8), (11, 8),
    (5, 9), (10, 9),
    (6, 10), (9, 10),
    (7, 11), (8, 11),
}
SPARKS = {(5, 5), (10, 5), (5, 10), (10, 10)}
CORE = {(7, 7), (8, 7), (7, 8), (8, 8)}

GLYPH = {p: R_RING for p in RING}
GLYPH.update({p: R_LINE for p in DIAMOND})
GLYPH.update({p: R_DOT for p in SPARKS})
GLYPH.update({p: R_CORE for p in CORE})


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


def marble_canvas():
    canvas = [[M_W for _ in range(W)] for _ in range(H)]
    for x, y in CHIPS_S:
        canvas[y][x] = M_S
    for x, y in CHIPS_M:
        canvas[y][x] = M_M
    for x, y in CHIPS_D:
        canvas[y][x] = M_D
    return canvas


def adjacent(points, x, y, radius):
    for py in range(y - radius, y + radius + 1):
        for px in range(x - radius, x + radius + 1):
            if (px, py) in points:
                return True
    return False


def add_rune(base):
    """Glyphs first; a dark seat hugs only the white-hot core; every glowing
    line gets a broken ring of faint halo pixels (kept sparse so the marble
    stays visible between the outer ring and the inner diamond)."""
    out = [row[:] for row in base]
    glyph_points = set(GLYPH)
    for y in range(H):
        for x in range(W):
            if (x, y) in GLYPH:
                out[y][x] = GLYPH[(x, y)]
            elif adjacent(CORE, x, y, 1):
                out[y][x] = R_EDGE
            elif adjacent(glyph_points, x, y, 1) and (x + y) % 2 == 0:
                out[y][x] = R_HALO
    return out


def preview(px, title, palette):
    print(title)
    for row in px:
        print("  " + "".join(palette.get(c, "?") for c in row))


plain = marble_canvas()
write_png(plain, r"D:\Mods_create\mod_1yuwb1l\ritual_stone.png")

runed = add_rune(plain)
write_png(runed, r"D:\Mods_create\mod_1yuwb1l\ritual_stone_rune.png")

print("OK: ritual_stone.png, ritual_stone_rune.png")
preview(runed, "ritual_stone_rune", {
    M_W: "W", M_S: "s", M_M: "m", M_D: "d",
    R_HALO: "h", R_EDGE: "k", R_RING: "o",
    R_LINE: "r", R_DOT: "*", R_CORE: "c",
})
