# Blockbench でモデルを編集する

この Mod のブロックモデルは「親モデル参照 + テクスチャ差し替え」だけの薄い JSON です。

```json
{
  "parent": "minecraft:block/orientable_with_bottom",
  "textures": { "front": "...", "side": "...", "top": "...", "bottom": "..." }
}
```

このため Blockbench の「Open Model」で直接開くことはできません (ジオメトリはバニラの
`minecraft:block/orientable_with_bottom` 側にあり、手元の JSON には `elements` がないため)。
既存の見た目をそのまま Blockbench に読み込むための書き出しスクリプトを用意しています。

## 書き出し

```console
$ python3 tools/export_blockbench.py
```

`docs/blockbench/` に次のファイルができます。

| ファイル | 用途 |
| --- | --- |
| `<block>.bbmodel` / `<block>_active.bbmodel` | テクスチャ埋め込み済み。Blockbench で `File > Open Model` |
| `flat/<block>.json` | 親を解決した素の Java モデル。`File > Import > Block/Item Model` |

対象は 5 ファミリー (hammer / sieve / barrel / crucible / water_crucible) の 無印 + 稼働中で、
計 10 種類 × 2 形式です。ファクトリーは同じジオメトリ・同じテクスチャを使い、正面の LED
(1 行目中央 6px) だけがティア色で変わるため、個別の bbmodel はありません。

> 書き出した `.bbmodel` と `flat/` の JSON は生成物なので `.gitignore` 済みです
> (リポジトリには含まれません)。使う前に上のコマンドを実行してください。

## Blockbench での編集

### 方法 A: テクスチャだけ描き直す

1. `docs/blockbench/hammer_machine.bbmodel` を Blockbench で開く
2. 右パネルの Textures から該当テクスチャを選び、Edit / Replace で描き直す
   (または画像をドラッグ&ドロップ)
3. `File > Export > Save Texture` で PNG を書き出し、
   `src/main/resources/assets/exmekdeorum/textures/block/` の同名ファイルと差し替える
4. `gradle build`

### 方法 B: ジオメトリを変える (面を足す、厚みを付けるなど)

1. `docs/blockbench/<block>.bbmodel` を開く
2. キューブを追加・編集する (解像度は 16×16 のまま)
3. `File > Export > Java Block/Item Model` で JSON + テクスチャを書き出す
4. 書き出した JSON を `src/main/resources/assets/exmekdeorum/models/block/<block>.json`
   に上書きする (親を持たない完全なモデルとして出力されます)
5. 稼働中の見た目も変える場合は `<block>_active.json` も同様に差し替える
6. `gradle build`

> 注意: `tools/generate_assets.py` を実行するとモデルとテクスチャが再生成されて上書きされます。
> Blockbench で作ったモデルを残す場合は、生成後に手でコピーするか、ジェネレーターの
> `generate_models()` / `generate_textures()` を無効化してください。

## テクスチャの対応

| 面 | テクスチャ |
| --- | --- |
| 正面 (north、facing で回転) | `<block>_front.png` (稼働中は `_front_active.png`) |
| 左右/背面 | `<block>_side.png` |
| 上面 | `<block>_top.png` |
| 下面 | `<block>_bottom.png` |

UV は全面 0,0–16,16 です。追加以外の変更 (テクスチャだけ) なら、モデルを触らず
PNG を差し替えるのが一番安全です。

## 参考

- テクスチャの自動生成・調整: `tools/generate_assets.py` (`front_pattern_pixels` / `TIER_LED` / `BASE_MAP`)
- 手順の詳細: [docs/CUSTOMIZE.md](../CUSTOMIZE.md)
