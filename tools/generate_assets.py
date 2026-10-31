#!/usr/bin/env python3
"""Generate placeholder assets and data for the Ex Mekanism Deorum machines.

Everything this script writes is a *placeholder*: flat 16x16 block textures, one 256x256 GUI
sheet, blockstates, models, language files, loot tables and simple crafting recipes.  Replace the
generated PNG files with your own art; re-running the script regenerates the JSON too, which is
fine as long as you did not hand-edit it.

Block layout (Mekanism convention):
    <family>_machine                      untiered machine (hammer/sieve/barrel/crucible)
    basic|advanced|elite|ultimate_<family>_factory   tiered factories

Usage:
    python3 tools/generate_assets.py
"""
from __future__ import annotations

import json
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "exmekdeorum")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "exmekdeorum")
MOD_ID = "exmekdeorum"

TIERS = ["basic", "advanced", "elite", "ultimate"]
FAMILIES = ["hammer", "sieve", "barrel", "crucible", "water_crucible"]

# Base colour per tier; the untiered machine uses a neutral steel colour.
TIER_COLORS = {
    None: (154, 158, 162),
    "basic": (134, 138, 142),
    "advanced": (196, 152, 58),
    "elite": (74, 163, 200),
    "ultimate": (86, 74, 112),
}

TIER_NAMES_EN = {"basic": "Basic", "advanced": "Advanced", "elite": "Elite", "ultimate": "Ultimate"}
TIER_NAMES_JA = {"basic": "基本", "advanced": "上級", "elite": "エリート", "ultimate": "究極"}
FAMILY_NAMES_EN = {"hammer": "Hammer", "sieve": "Sieve", "barrel": "Barrel", "crucible": "Crucible", "water_crucible": "Water Crucible"}
FAMILY_NAMES_JA = {"hammer": "粉砕", "sieve": "ふるい", "barrel": "バレル", "crucible": "るつぼ", "water_crucible": "水るつぼ"}

FACINGS = ["north", "east", "south", "west"]
FACING_ROT = {"north": 0, "east": 90, "south": 180, "west": -90}

# Old (pre-restructure) names, removed so the obsolete 32-block assets do not linger.
OLD_NAMES = {
    f"{tier}_{family}_{kind}"
    for tier in TIERS
    for family in FAMILIES
    for kind in ("machine", "factory")
}
OLD_TEXTURES = {f"machine_{tier}_{family}" for tier in TIERS for family in FAMILIES}


def all_blocks() -> list[tuple[str, str | None, bool]]:
    """Return (family, tier, factory) for every block. tier is None for untiered machines."""
    blocks: list[tuple[str, str | None, bool]] = []
    for family in FAMILIES:
        blocks.append((family, None, False))
        for tier in TIERS:
            blocks.append((family, tier, True))
    return blocks


def block_name(family: str, tier: str | None, factory: bool) -> str:
    return f"{tier}_{family}_factory" if factory else f"{family}_machine"


def write_json(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def write_png(path: str, width: int, height: int, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)

    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type: none
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (
            struct.pack(">I", len(payload))
            + tag
            + payload
            + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)  # 8-bit RGBA
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n")
        fh.write(chunk(b"IHDR", ihdr))
        fh.write(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
        fh.write(chunk(b"IEND", b""))


def clamp(v: int) -> int:
    return max(0, min(255, v))


def shade(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(clamp(c + amount) for c in color)  # type: ignore[return-value]


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(clamp(round(x * (1 - t) + y * t)) for x, y in zip(a, b))  # type: ignore[return-value]


# 正面の内側 (14x13) を塗るファミリー色。Mek の鋼色に馴染むよう彩度を落としている。
FAMILY_TINT = {
    "hammer": (167, 135, 90),
    "sieve": (105, 140, 167),
    "barrel": (145, 120, 95),
    "crucible": (170, 106, 85),
    "water_crucible": (95, 130, 175),
}
# Mekanism 本体の factory LED (assets/mekanism/textures/block/factory/led.png) から採取した
# ティア色。明・暗の 2 色を 1px 交互に並べて LED ストリップにする。
TIER_LED = {
    "basic": ((39, 255, 161), (42, 224, 151)),
    "advanced": ((221, 117, 93), (206, 80, 69)),
    "elite": ((117, 229, 229), (44, 222, 223)),
    "ultimate": ((200, 90, 203), (185, 78, 196)),
}
# 各ファミリーのベースに使う Mekanism 本体テクスチャ (machines/<name>, factories/<name>)。
BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "mekanism_base")
# すべてのファミリーで crusher のベースを使う (正面だけファミリーアイコンで差し替え)。
BASE_MAP = {
    "hammer": ("crusher", "crushing"),
    "sieve": ("crusher", "crushing"),
    "barrel": ("crusher", "crushing"),
    "crucible": ("crusher", "crushing"),
    "water_crucible": ("crusher", "crushing"),
}

# 手で描き直したテクスチャは生成時に上書きしない (ファイル名で指定)。
PRESERVE_TEXTURES = {
    "hammer_machine_front.png",
    "hammer_machine_front_active.png",
}
FACES = ("front", "front_active", "side", "top", "bottom")
ICON_W, ICON_H = 14, 13  # 16x16 の上 2px / 他 1px を縁として残した内側

# 14x13 のアイコン。'#' が本体 (左上がハイライト、右下が影になる)、'-' がバンド。
FAMILY_ART = {
    "hammer": [
        "..............",
        "...########...",
        "..##########..",
        "..##########..",
        "..##########..",
        "...########...",
        ".....####.....",
        ".....####.....",
        ".....####.....",
        ".....####.....",
        ".....####.....",
        "....######....",
        "..............",
    ],
    "sieve": [
        "..............",
        "..##########..",
        "..#....#...#..",
        "..#....#...#..",
        "..#....#...#..",
        "..##########..",
        "..#....#...#..",
        "..#....#...#..",
        "..##########..",
        "..#....#...#..",
        "..#....#...#..",
        "..##########..",
        "..............",
    ],
    "barrel": [
        "..............",
        "...########...",
        "..##########..",
        "..----------..",
        "..##########..",
        "..##########..",
        "..----------..",
        "..##########..",
        "..##########..",
        "..----------..",
        "..##########..",
        "...########...",
        "..............",
    ],
    "crucible": [
        "..............",
        "..##########..",
        "..##########..",
        "..##########..",
        "..##------##..",
        "..##------##..",
        "...########...",
        "....######....",
        ".....#..#.....",
        "....#.##.#....",
        "...#.####.#...",
        "..#..####..#..",
        "..............",
    ],
    "water_crucible": [
        "..............",
        "......####....",
        ".....######...",
        "....########..",
        "...##########.",
        "...##########.",
        "..############",
        "..############",
        "..############",
        "...##########.",
        "....########..",
        ".....######...",
        "..............",
    ],
}


def front_icon_pixels(family: str, active: bool) -> list[list[tuple[int, int, int]]]:
    # Mek の機械はほぼ無彩色なので、ファミリー色は鋼色に寄せて控えめにする。
    tint = mix(FAMILY_TINT[family], (148, 148, 148), 0.60)
    if active:
        tint = shade(tint, 15)
    hi = mix(tint, (225, 225, 225), 0.30)
    lo = shade(tint, -45)
    plate = (34, 34, 36)
    band = shade(tint, -55)
    art = FAMILY_ART[family]
    out: list[list[tuple[int, int, int]]] = []
    for y, row in enumerate(art):
        line = []
        for x, ch in enumerate(row):
            if ch == "-":
                line.append(band)
            elif ch == "#":
                up = y > 0 and art[y - 1][x] == "#"
                left = x > 0 and row[x - 1] == "#"
                down = y < len(art) - 1 and art[y + 1][x] == "#"
                right = x < len(row) - 1 and row[x + 1] == "#"
                if not up or not left:
                    line.append(hi)
                elif not down or not right:
                    line.append(lo)
                else:
                    line.append(tint)
            else:
                line.append(plate)
        out.append(line)
    return out


ADAM7 = ((0, 0, 8, 8), (4, 0, 8, 8), (0, 4, 4, 8), (2, 0, 4, 4), (0, 2, 2, 4), (1, 0, 2, 2), (0, 1, 1, 2))


def _filter_rows(raw, offset, stride, count, bpp):
    rows = []
    prev = bytearray(stride)
    for _ in range(count):
        f = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                line[x] = (line[x] + ((line[x - bpp] if x >= bpp else 0) + prev[x]) // 2) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        rows.append(bytes(line))
        prev = line
    return rows, offset


def _pixel(line, x, ct, plte, trns):
    if ct == 3:
        idx = line[x]
        r, g, b = plte[idx * 3:idx * 3 + 3]
        a = trns[idx] if trns and idx < len(trns) else 255
    elif ct == 6:
        r, g, b, a = line[x * 4:x * 4 + 4]
    elif ct == 2:
        r, g, b = line[x * 3:x * 3 + 3]
        a = 255
    elif ct == 0:
        r = g = b = line[x]
        a = 255
    else:
        r = g = b = line[x * 2]
        a = line[x * 2 + 1]
    return (r, g, b, a)


def decode_png(path: str) -> list[list[tuple[int, int, int, int]]]:
    """Mekanism のパレット PNG / Adam7 interlace PNG / アニメ 1 フレーム目を読む。"""
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos = 8
    idat = b""
    w = h = bd = ct = interlace = 0
    plte = trns = None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if tag == b"IHDR":
            w, h, bd, ct, _, _, interlace = struct.unpack(">IIBBBBB", payload)
        elif tag == b"PLTE":
            plte = payload
        elif tag == b"tRNS":
            trns = payload
        elif tag == b"IDAT":
            idat += payload
        elif tag == b"IEND":
            break
    raw = zlib.decompress(idat)
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    out: list[list[tuple[int, int, int, int]]] = [[(0, 0, 0, 0)] * w for _ in range(h)]
    if bd == 4 and ct == 3:
        stride = (w + 1) // 2
        rows, _ = _filter_rows(raw, 0, stride, h, 1)
        for y in range(h):
            for x in range(w):
                out[y][x] = _pixel(bytes([line for line in [rows[y][x // 2]]])[0:1] + b"\x00\x00\x00", 0, ct, plte, trns) if False else _pixel4(rows[y][x // 2], x % 2, plte, trns)
        return out
    bpp = max(1, ch * bd // 8)
    if interlace == 0:
        stride = w * ch * bd // 8
        rows, _ = _filter_rows(raw, 0, stride, h, bpp)
        for y in range(h):
            for x in range(w):
                out[y][x] = _pixel(rows[y], x, ct, plte, trns)
        return out
    off = 0
    for sx, sy, dx, dy in ADAM7:
        pw = (w - sx + dx - 1) // dx
        ph = (h - sy + dy - 1) // dy
        if pw <= 0 or ph <= 0:
            continue
        rows, off = _filter_rows(raw, off, pw * ch, ph, bpp)
        for py in range(ph):
            for px in range(pw):
                out[sy + py * dy][sx + px * dx] = _pixel(rows[py], px, ct, plte, trns)
    return out


def _pixel4(byte: int, half: int, plte, trns):
    idx = (byte >> (4 * (1 - half))) & 15
    r, g, b = plte[idx * 3:idx * 3 + 3]
    a = trns[idx] if trns and idx < len(trns) else 255
    return (r, g, b, a)


def _mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(clamp(round(x * (1 - t) + y * t)) for x, y in zip(a, b))  # type: ignore[return-value]


def machine_texture(family: str, tier: str | None, face: str) -> list[list[tuple[int, int, int, int]]]:
    """Mekanism 本体のテクスチャを読み、正面のアイコンとティア LED だけ差し替える。"""
    base_face = face
    machine_name, factory_name = BASE_MAP[family]
    sub = os.path.join("machines", machine_name) if tier is None else os.path.join("factories", factory_name)
    px = decode_png(os.path.join(BASE, sub, base_face + ".png"))
    # アニメーション テクスチャは 1 フレーム目 (16x16) だけ使う。
    px = [row[:16] for row in px[:16]]

    # ファクトリー: Mekanism の front_led と同じ位置 (テクスチャ row 1, x 5..10) に
    # led.png のティア色を市松で置く。
    if tier is not None:
        bright, dark = TIER_LED[tier]
        for i, x in enumerate(range(5, 11)):
            c = bright if i % 2 == 0 else dark
            if face == "front_active":
                c = shade(c, 18)
            px[1][x] = (*c, 255)

    # 正面の内側 (上 2px / 他 1px の縁を残した 14x13) をファミリーアイコンで塗る。
    if face in ("front", "front_active"):
        icon = front_icon_pixels(family, face == "front_active")
        for gy, row in enumerate(icon):
            for gx, color in enumerate(row):
                px[2 + gy][1 + gx] = (*color, 255)
    # ブロック面は不透明に (Mekanism の overlay 用 alpha 0 をそのまま出すと穴になる)。
    px = [[(r, g, b, 255) for (r, g, b, _a) in row] for row in px]
    return px


def remove_stale() -> None:
    stale_paths = []
    for name in OLD_NAMES:
        stale_paths += [
            os.path.join(ASSETS, "blockstates", f"{name}.json"),
            os.path.join(ASSETS, "models", "item", f"{name}.json"),
            os.path.join(DATA, "loot_table", "blocks", f"{name}.json"),
            os.path.join(DATA, "recipe", f"{name}.json"),
        ]
    for name in OLD_TEXTURES:
        stale_paths += [
            os.path.join(ASSETS, "models", "block", f"{name}.json"),
            os.path.join(ASSETS, "textures", "block", f"{name}.png"),
        ]
    # Legacy single-face textures from the cube_all placeholder era.
    for family, tier, factory in all_blocks():
        stale_paths.append(os.path.join(ASSETS, "textures", "block", f"{block_name(family, tier, factory)}.png"))
    for path in stale_paths:
        if os.path.isfile(path):
            os.remove(path)


def generate_models() -> None:
    for family, tier, factory in all_blocks():
        name = block_name(family, tier, factory)
        for face in FACES:
            path = os.path.join(ASSETS, "textures", "block", f"{name}_{face}.png")
            if os.path.basename(path) in PRESERVE_TEXTURES and os.path.isfile(path):
                continue
            write_png(path, 16, 16, machine_texture(family, tier, face))
        base_textures = {
            "front": f"{MOD_ID}:block/{name}_front",
            "side": f"{MOD_ID}:block/{name}_side",
            "top": f"{MOD_ID}:block/{name}_top",
            "bottom": f"{MOD_ID}:block/{name}_bottom",
        }
        active_textures = dict(base_textures, front=f"{MOD_ID}:block/{name}_front_active")
        write_json(
            os.path.join(ASSETS, "models", "block", f"{name}.json"),
            {"parent": "minecraft:block/orientable_with_bottom", "textures": base_textures},
        )
        write_json(
            os.path.join(ASSETS, "models", "block", f"{name}_active.json"),
            {"parent": "minecraft:block/orientable_with_bottom", "textures": active_textures},
        )
        write_json(
            os.path.join(ASSETS, "models", "item", f"{name}.json"),
            {"parent": f"{MOD_ID}:block/{name}"},
        )

    # The GUI background (textures/gui/deorum_machine.png) is NOT generated here.
    # It ships as a copy of Mekanism's gui/base.png (MIT, see README) so that the
    # 4-pixel nine-slice border matches Mekanism exactly. Edit that PNG directly.


def generate_blockstates() -> None:
    for family, tier, factory in all_blocks():
        name = block_name(family, tier, factory)
        variants = {}
        for facing in FACINGS:
            rotation = FACING_ROT[facing]
            variants[f"facing={facing},active=false"] = _variant(name, rotation)
            variants[f"facing={facing},active=true"] = _variant(f"{name}_active", rotation)
        write_json(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"variants": variants})


def _variant(model: str, rotation: int) -> dict:
    variant = {"model": f"{MOD_ID}:block/{model}"}
    if rotation:
        variant["y"] = rotation
    return variant


def generate_lang() -> None:
    en = {"itemGroup.exmekdeorum": "Ex Mekanism Deorum"}
    ja = {"itemGroup.exmekdeorum": "Ex Mekanism Deorum"}
    for family, tier, factory in all_blocks():
        name = block_name(family, tier, factory)
        if factory:
            en_name = f"{TIER_NAMES_EN[tier]} {FAMILY_NAMES_EN[family]} Factory"
            ja_name = f"{TIER_NAMES_JA[tier]}{FAMILY_NAMES_JA[family]}工場"
            en_desc = f"{TIER_NAMES_EN[tier]}-tier {FAMILY_NAMES_EN[family].lower()} factory (parallel)."
            ja_desc = f"{TIER_NAMES_JA[tier]}ティアの{FAMILY_NAMES_JA[family]}工場（並列処理）。"
        else:
            en_name = f"{FAMILY_NAMES_EN[family]} Machine"
            ja_name = f"{FAMILY_NAMES_JA[family]}機"
            en_desc = f"Ex Deorum {FAMILY_NAMES_EN[family].lower()} machine."
            ja_desc = f"Ex Deorum の{FAMILY_NAMES_JA[family]}を機械化したもの。"
        en[f"block.{MOD_ID}.{name}"] = en_name
        ja[f"block.{MOD_ID}.{name}"] = ja_name
        # TileEntityMekanism#getDisplayName uses Util.makeDescriptionId("container", blockId), so the
        # GUI title needs container.<name> as well.
        en[f"container.{MOD_ID}.{name}"] = en_name
        ja[f"container.{MOD_ID}.{name}"] = ja_name
        en[f"description.{name}"] = en_desc
        ja[f"description.{name}"] = ja_desc
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
    write_json(os.path.join(ASSETS, "lang", "ja_jp.json"), ja)


def generate_loot_tables() -> None:
    for family, tier, factory in all_blocks():
        name = block_name(family, tier, factory)
        table = {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "bonus_rolls": 0,
                    "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{name}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        }
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{name}.json"), table)


def generate_recipes() -> None:
    """Mekanism-style recipes.

    * Untiered machines: basic circuit + redstone + iron ring around the Ex Deorum
      mechanical counterpart (mechanical hammer/sieve, oak barrel, porcelain crucible).
      Crucibles get a lava/water bucket in the bottom row to tell them apart.
    * Factories follow Mekanism's upgrade ladder: the previous block in the centre and the
      matching alloy/circuit/ingot ring (basic -> advanced -> elite -> ultimate).
    """
    parts = {
        "hammer": "exdeorum:mechanical_hammer",
        "sieve": "exdeorum:mechanical_sieve",
        "barrel": "exdeorum:oak_barrel",
        "crucible": "exdeorum:porcelain_crucible",
        "water_crucible": "exdeorum:porcelain_crucible",
    }
    fluids = {"crucible": "minecraft:lava_bucket", "water_crucible": "minecraft:water_bucket"}
    tiers = {
        "basic": ("minecraft:redstone", "mekanism:basic_control_circuit", "minecraft:iron_ingot"),
        "advanced": ("mekanism:alloy_infused", "mekanism:advanced_control_circuit", "mekanism:ingot_osmium"),
        "elite": ("mekanism:alloy_reinforced", "mekanism:elite_control_circuit", "minecraft:gold_ingot"),
        "ultimate": ("mekanism:alloy_atomic", "mekanism:ultimate_control_circuit", "minecraft:diamond"),
    }
    previous_tier = {"advanced": "basic", "elite": "advanced", "ultimate": "elite"}

    for family in FAMILIES:
        machine = block_name(family, None, False)
        key = {
            "A": {"item": "minecraft:redstone"},
            "C": {"item": "mekanism:basic_control_circuit"},
            "I": {"item": "minecraft:iron_ingot"},
            "P": {"item": parts[family]},
        }
        pattern = ["ACA", "IPI", "ACA"]
        if family in fluids:
            key["B"] = {"item": fluids[family]}
            pattern = ["ACA", "IPI", "ABA"]
        write_json(
            os.path.join(DATA, "recipe", f"{machine}.json"),
            {
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "pattern": pattern,
                "key": key,
                "result": {"id": f"{MOD_ID}:{machine}", "count": 1},
            },
        )

        for tier in TIERS:
            name = block_name(family, tier, True)
            alloy, circuit, ingot = tiers[tier]
            if tier == "basic":
                center = {"item": f"{MOD_ID}:{machine}"}
            else:
                center = {"item": f"{MOD_ID}:{block_name(family, previous_tier[tier], True)}"}
            write_json(
                os.path.join(DATA, "recipe", f"{name}.json"),
                {
                    "type": "minecraft:crafting_shaped",
                    "category": "misc",
                    "pattern": ["ACA", "IPI", "ACA"],
                    "key": {
                        "A": {"item": alloy},
                        "C": {"item": circuit},
                        "I": {"item": ingot},
                        "P": center,
                    },
                    "result": {"id": f"{MOD_ID}:{name}", "count": 1},
                },
            )


def main() -> None:
    remove_stale()
    generate_models()
    generate_blockstates()
    generate_lang()
    generate_loot_tables()
    generate_recipes()
    print(f"Generated placeholder assets for {len(all_blocks())} blocks.")


if __name__ == "__main__":
    main()
