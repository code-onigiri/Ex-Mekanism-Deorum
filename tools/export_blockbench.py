"""ブロックモデルを Blockbench で開ける形に書き出す。

- docs/blockbench/<name>.bbmodel : テクスチャ埋め込み済み (File > Open Model)
- docs/blockbench/flat/<name>.json : 親を解決した素の Java モデル (File > Import)

使い方: python3 tools/export_blockbench.py
"""
import base64
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(ROOT, "src/main/resources/assets/exmekdeorum/models/block")
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/exmekdeorum/textures/block")
OUT = os.path.join(ROOT, "docs/blockbench")

FAMILIES = ["hammer", "sieve", "barrel", "crucible", "water_crucible"]
FACES = {
    # face -> (texture slot, uv)
    "north": ("front", [0, 0, 16, 16]),
    "east": ("side", [0, 0, 16, 16]),
    "south": ("side", [0, 0, 16, 16]),
    "west": ("side", [0, 0, 16, 16]),
    "up": ("top", [0, 0, 16, 16]),
    "down": ("bottom", [0, 0, 16, 16]),
}


def texture_refs(block):
    """resource paths for the inactive variant of one block."""
    return {
        "front": f"exmekdeorum:block/{block}_front",
        "front_active": f"exmekdeorum:block/{block}_front_active",
        "side": f"exmekdeorum:block/{block}_side",
        "top": f"exmekdeorum:block/{block}_top",
        "bottom": f"exmekdeorum:block/{block}_bottom",
    }


def read_png(path):
    with open(path, "rb") as f:
        return f.read()


def data_uri(path):
    return "data:image/png;base64," + base64.b64encode(read_png(path)).decode("ascii")


def faces_for(textures, tex_index):
    faces = {}
    for face, (slot, uv) in FACES.items():
        faces[face] = {
            "uv": list(uv),
            "texture": tex_index[slot],
        }
    return faces


def make_bbmodel(block, active):
    name = block + ("_active" if active else "")
    refs = texture_refs(block)
    slots = {"front": "front_active" if active else "front",
             "side": "side", "top": "top", "bottom": "bottom"}
    tex_index = {"front": 0, "side": 1, "top": 2, "bottom": 3}
    textures = []
    for slot in ("front", "side", "top", "bottom"):
        tex_slot = slots[slot]
        png = os.path.join(TEX_DIR, f"{block}_{tex_slot}.png")
        textures.append({
            "path": "",
            "name": f"{block}_{tex_slot}",
            "folder": "block",
            "namespace": "exmekdeorum",
            "id": str(tex_index[slot]),
            "particle": slot == "front",
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": False,
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, refs[tex_slot])),
            "relative_path": "",
            "source": data_uri(png),
        })

    element_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, "exmekdeorum:" + name))
    element = {
        "name": "cube",
        "uuid": element_uuid,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "light_emission": 0,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": [0, 0, 0],
        "to": [16, 16, 16],
        "autouv": 0,
        "color": 7,
        "origin": [0, 0, 0],
        "faces": faces_for(textures, tex_index),
    }
    model = {
        "meta": {
            "format_version": "4.10",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": name,
        "model_identifier": "",
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "resolution": {"width": 16, "height": 16},
        "elements": [element],
        "outliner": [element_uuid],
        "textures": textures,
    }
    return model


def make_flat(block, active):
    refs = texture_refs(block)
    faces = {}
    for face, (slot, _uv) in FACES.items():
        faces[face] = {"uv": [0, 0, 16, 16], "texture": f"#{slot}"}
    front = "front_active" if active else "front"
    return {
        "textures": {
            "front": refs[front],
            "side": refs["side"],
            "top": refs["top"],
            "bottom": refs["bottom"],
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": faces,
        }],
    }


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.join(OUT, "flat"), exist_ok=True)
    written = 0
    for family in FAMILIES:
        block = f"{family}_machine"
        for active in (False, True):
            name = block + ("_active" if active else "")
            bb = os.path.join(OUT, name + ".bbmodel")
            with open(bb, "w") as f:
                json.dump(make_bbmodel(block, active), f, indent=2)
            flat = os.path.join(OUT, "flat", name + ".json")
            with open(flat, "w") as f:
                json.dump(make_flat(block, active), f, indent=2)
            written += 2
    print(f"wrote {written} files to {OUT}")


if __name__ == "__main__":
    main()
