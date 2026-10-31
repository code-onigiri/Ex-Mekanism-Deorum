"""MOD アイコン (Hammer Machine の 3D 等角レンダリング) を書き出す。

使い方: python3 tools/make_icon.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_assets as ga


def render_3d_icon(out_path: str, size: int, scale: float = 160.0) -> None:
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    tex_dir = os.path.join(root, "src/main/resources/assets/exmekdeorum/textures/block")

    top_tex = ga.decode_png(os.path.join(tex_dir, "hammer_machine_top.png"))
    front_tex = ga.decode_png(os.path.join(tex_dir, "hammer_machine_front.png"))
    side_tex = ga.decode_png(os.path.join(tex_dir, "hammer_machine_side.png"))

    canvas = [[(0, 0, 0, 0) for _ in range(size)] for _ in range(size)]
    zbuf = [[-999999.0 for _ in range(size)] for _ in range(size)]

    yaw = math.radians(-35)
    pitch = math.radians(30)

    cos_y, sin_y = math.cos(yaw), math.sin(yaw)
    cos_p, sin_p = math.cos(pitch), math.sin(pitch)

    def transform(x: float, y: float, z: float) -> tuple[float, float, float]:
        x1 = x * cos_y + z * sin_y
        y1 = y
        z1 = -x * sin_y + z * cos_y
        x2 = x1
        y2 = y1 * cos_p - z1 * sin_p
        z2 = y1 * sin_p + z1 * cos_p
        return x2, y2, z2

    def shade_color(rgba: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
        r, g, b, a = rgba
        return (
            min(255, max(0, int(r * factor))),
            min(255, max(0, int(g * factor))),
            min(255, max(0, int(b * factor))),
            a,
        )

    # 視覚的な中心調整（上方向の突起・傾きによる重心の補正）
    cx, cy = size / 2.0, size / 2.0 + (size * 0.02)

    def to_screen(x: float, y: float, z: float) -> tuple[float, float, float]:
        tx, ty, tz = transform(x, y, z)
        sx = cx + tx * scale * (size / 512.0)
        sy = cy - ty * scale * (size / 512.0)
        return sx, sy, tz

    def render_quad(
        p0: tuple[float, float, float],
        p1: tuple[float, float, float],
        p2: tuple[float, float, float],
        p3: tuple[float, float, float],
        color: tuple[int, int, int, int],
    ) -> None:
        def draw_tri(
            v0: tuple[float, float, float],
            v1: tuple[float, float, float],
            v2: tuple[float, float, float],
        ) -> None:
            min_x = max(0, int(min(v0[0], v1[0], v2[0])))
            max_x = min(size - 1, int(max(v0[0], v1[0], v2[0]) + 1))
            min_y = max(0, int(min(v0[1], v1[1], v2[1])))
            max_y = min(size - 1, int(max(v0[1], v1[1], v2[1]) + 1))

            denom = (v1[1] - v2[1]) * (v0[0] - v2[0]) + (v2[0] - v1[0]) * (v0[1] - v2[1])
            if abs(denom) < 1e-6:
                return

            for y in range(min_y, max_y + 1):
                for x in range(min_x, max_x + 1):
                    px = x + 0.5
                    py = y + 0.5
                    w0 = ((v1[1] - v2[1]) * (px - v2[0]) + (v2[0] - v1[0]) * (py - v2[1])) / denom
                    w1 = ((v2[1] - v0[1]) * (px - v2[0]) + (v0[0] - v2[0]) * (py - v2[1])) / denom
                    w2 = 1.0 - w0 - w1
                    if w0 >= -1e-4 and w1 >= -1e-4 and w2 >= -1e-4:
                        z = w0 * v0[2] + w1 * v1[2] + w2 * v2[2]
                        if z > zbuf[y][x]:
                            zbuf[y][x] = z
                            canvas[y][x] = color

        draw_tri(p0, p1, p2)
        draw_tri(p0, p2, p3)

    # 1. 上面 (Y = 1.0, 光源係数 = 1.0)
    for u in range(16):
        for v in range(16):
            c = shade_color(top_tex[v][u], 1.0)
            x0 = -1.0 + u * (2.0 / 16.0)
            x1 = x0 + (2.0 / 16.0)
            z0 = -1.0 + v * (2.0 / 16.0)
            z1 = z0 + (2.0 / 16.0)
            p0 = to_screen(x0, 1.0, z0)
            p1 = to_screen(x1, 1.0, z0)
            p2 = to_screen(x1, 1.0, z1)
            p3 = to_screen(x0, 1.0, z1)
            render_quad(p0, p1, p2, p3, c)

    # 2. 正面 (Z = 1.0, 光源係数 = 0.85)
    for u in range(16):
        for v in range(16):
            c = shade_color(front_tex[v][u], 0.85)
            x0 = -1.0 + u * (2.0 / 16.0)
            x1 = x0 + (2.0 / 16.0)
            y0 = 1.0 - v * (2.0 / 16.0)
            y1 = y0 - (2.0 / 16.0)
            p0 = to_screen(x0, y0, 1.0)
            p1 = to_screen(x1, y0, 1.0)
            p2 = to_screen(x1, y1, 1.0)
            p3 = to_screen(x0, y1, 1.0)
            render_quad(p0, p1, p2, p3, c)

    # 3. 側面 (X = 1.0, 光源係数 = 0.65)
    for u in range(16):
        for v in range(16):
            c = shade_color(side_tex[v][u], 0.65)
            z0 = 1.0 - u * (2.0 / 16.0)
            z1 = z0 - (2.0 / 16.0)
            y0 = 1.0 - v * (2.0 / 16.0)
            y1 = y0 - (2.0 / 16.0)
            p0 = to_screen(1.0, y0, z0)
            p1 = to_screen(1.0, y0, z1)
            p2 = to_screen(1.0, y1, z1)
            p3 = to_screen(1.0, y1, z0)
            render_quad(p0, p1, p2, p3, c)

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    ga.write_png(out_path, size, size, canvas)
    print(f"Generated {size}x{size} icon to {out_path}")


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    render_3d_icon(os.path.join(root, "src/main/resources/icon.png"), size=128)
    render_3d_icon(os.path.join(root, "docs/icon.png"), size=512)
