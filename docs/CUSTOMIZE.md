# カスタマイズ (テクスチャ / GUI / バランス)

この Mod の見た目と数値は、すべてリポジトリ内のテクスチャ・GUI コード・定数で完結しています。
ここでは「どこを触れば何が変わるか」をまとめます。

## テクスチャ

Blockbench でモデルごと編集したい場合は [docs/blockbench/README.md](blockbench/README.md) を参照してください
(`python3 tools/export_blockbench.py` で開ける形に書き出せます)。

### 生成されるファイル

ブロック 1 種につき 5 枚の PNG と、モデル/blockstate を生成します。

| パス | 内容 |
| --- | --- |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_front.png` | 正面 (ファミリーアイコン) |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_front_active.png` | 稼働中の正面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_side.png` | 側面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_top.png` | 上面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_bottom.png` | 底面 |
| `src/main/resources/assets/exmekdeorum/models/block/<block>.json` | `minecraft:block/orientable_with_bottom` (正面 = north) |
| `src/main/resources/assets/exmekdeorum/models/block/<block>_active.json` | 稼働中モデル |
| `src/main/resources/assets/exmekdeorum/blockstates/<block>.json` | `facing` × `active` の 8 バリアント |
| `src/main/resources/assets/exmekdeorum/models/item/<block>.json` | アイテムモデル |
| `src/main/resources/assets/exmekdeorum/textures/gui/deorum_machine.png` | 共通 GUI (256×256 nine-slice) |

GUI 背景だけは生成対象外です。Mekanism の `assets/mekanism/gui/base.png` を複製した固定
アセットで、直接編集してください (`tools/generate_assets.py` は上書きしません)。

### ベーステクスチャ

- **正面**: 手描きのパネル (`tools/mekanism_base/machines/custom/front.png`) がベース
- **側面 / 上面 / 底面**: Mekanism の `crusher` (ファクトリーは `factory/crushing`) がベース
- 生成時に変えるのは次の 2 点だけ
  1. 正面の内側 (上 2px / 左右下 1px の縁を残した **14×13**) にファミリーごとの機械的な柄を描く
     (`front_pattern_pixels`)
  2. ファクトリーは Mekanism の `front_led` と同じ位置 (上端 1px・中央 6px) にティア LED を置く
     (`TIER_LED`、色は Mekanism の `factory/led.png` から採取)

| ファミリー | 柄 | 実装 |
| --- | --- | --- |
| ハンマー | 45 度の滑り止めリブ | 斜めハッチ |
| ふるい | パンチングメタル (4px ピッチの穴) | 穴あき板 |
| バレル | 横ルーバー (鉄帯のスリット) | 横ストライプ |
| 溶岩るつぼ | 丸い圧力ハッチ | 同心円リング |
| 水るつぼ | 縦フィン (冷却ラジエーター) | 縦ストライプ |

柄は「絵」ではなくパターンなので、色を変えればそのまま別の機械感を出せます。共通の
フレーム (外周 1px + 四隅のボルト) は `_frame()` / `_bolt()` で描いています。

`tools/mekanism_base/` には他マシンのベースも残してあるので、`BASE_MAP` を書き換えれば
ファミリーごとに別ベースへ戻せます。正面だけ別の画像にしたい場合は
`machines/custom/front.png` を差し替えてください。

### 手描きテクスチャの保護

`tools/generate_assets.py` の `PRESERVE_TEXTURES` にファイル名を入れると、その PNG は生成時に
上書きされません。現在は空ですが、Blockbench で直接描いた面を残したいときに使えます。

```python
PRESERVE_TEXTURES = {
    "hammer_machine_front.png",
}
```

保護したテクスチャを消して生成し直したいときは、該当 PNG を削除するか、セットから外してから
`python3 tools/generate_assets.py` を実行してください。

### 差し替え方

- 正面のパネル自体 → `tools/mekanism_base/machines/custom/front.png` (16×16)
- 柄 → `tools/generate_assets.py` の `front_pattern_pixels` (ファミリーごとの分岐) と
  パレット (`PATTERN_PLATE` / `PATTERN_RIB` / `PATTERN_RIB_LO` / `PATTERN_DEEP` / `PATTERN_BOLT`)
- ベースを別の Mek 機械に変える → `BASE_MAP` (側面 / 上面 / 底面)
- ティア LED 色 → `TIER_LED`
- ベース画像そのもの → `tools/mekanism_base/` の PNG を置換

変更後は生成スクリプトを再実行します。

```console
$ python3 tools/generate_assets.py
$ gradle build
```

手で描いた PNG は再実行で上書きされるので注意してください。


## MOD アイコン

- ゲーム内の Mod 一覧用: `src/main/resources/icon.png` (128×128、`neoforge.mods.toml` の `logoFile = "icon.png"`)
- CurseForge / GitHub 用: `docs/icon.png` (512×512)
- 元絵は 32×32 のドット絵で `/tmp/icon.py` 相当のスクリプトから書き出しています。差し替える場合は
  同じサイズで上書きして `gradle build` してください (アイコンはテクスチャ生成の対象外です)。

## GUI

GUI は `src/main/java/dev/exmekdeorum/client/GuiDeorumMachine.java` の 1 クラスです。

- ベースは Mekanism の `GuiMekanismTile`。`dynamicSlots = true` なので、コンテナに登録された
  スロットが自動で描画されます。
- GUI は 200×188。背景は Mekanism と同じ **4 px の nine-slice**
  (`GuiUtils.renderBackgroundTexture(...)`) で描画しています。四隅の 4×4 px だけがそのまま
  コピーされ、辺と中央は引き伸ばされます。
- 背景を編集する場合も同じ規則で、**枠はシートの外周 4 px 以内**に置き、中央は単色に
  してください。見出し帯や区切り線のような位置依存の絵は、引き伸ばしでずれます。

| ウィジェット | 位置 (x, y) | 備考 |
| --- | --- | --- |
| タイトル | `titleLabelY = 6` | |
| エネルギーゲージ (縦) | 184, 18 / 高さ 54 | `GuiVerticalPowerBar` |
| 進行バー | ハンマー/ふるい: 30, 76 / バレル・るつぼ: 88, 44 | `GuiProgress` (25×9)。稼働中のレーンだけで平均。**クリックで JEI レシピを開く** |
| 液体ゲージ (1 本) | 溶岩るつぼ・水るつぼ: 121, 22 / 16×54 | 出力液体 |
| 液体ゲージ (2 本) | バレル: 64, 22 と 121, 22 / 16×54 | 左: 入力タンク、右: 出力タンク |
| エネルギータブ | 左下 | `GuiEnergyTab` |
| 側面設定タブ | 左上 (-26, 6) | `GuiSideConfigurationTab` |
| アップグレード/赤石/セキュリティタブ | 右側 | `GuiMekanismTile` が自動追加 |

### レイアウトのルール

バレル・るつぼは **進行バー (88,44) を中央の基準**に、左右へ 8 px ずつ要素を並べます。

- バレル: 左に [アイテム入力 2 個 + 入力タンク]、右に [出力タンク + アイテム出力]
- るつぼ: 左に [アイテム入力]、右に [液体出力]
- エネルギーは左のアイテム入力の真下
- **アイテム + エネルギーのまとまりの縦中央は、進行バーのライン (y≈48) に揃える**

ボタンやラベルを足すときも、この 8 px のリズムを崩さないと窮屈に見えます。

### ウィンドウサイズを変える

`GuiDeorumMachine.WIDTH/HEIGHT`、`DeorumMachineContainer` の `INVENTORY_X/Y`、
`titleLabelY` / `inventoryLabelY`、スロット座標を同時に変えます。
プレイヤーインベントリは `INVENTORY_X = 19` / `INVENTORY_Y = 104` (ラベル `94`) です。

### スロット

スロット index はファミリーごとに違います (`initInventory` の追加順)。

| ファミリー | index → 位置 / 用途 |
| --- | --- |
| ハンマー / ふるい | 0 = 30,18 入力 / 1–20 = 66 + 18×列, 18 + 18×行 出力 / 21 = 30,54 エネルギー / 22 = 30,36 補助 |
| バレル | 0 = 40,22 入力 A / 1 = 40,40 入力 B / 2 = 145,22 出力 / 3 = 40,58 エネルギー |
| 溶岩るつぼ / 水るつぼ | 0 = 64,31 入力 / 1 = 64,49 エネルギー |

スロット位置を変えたら、GUI 画像のスロット枠と上のウィジェット位置も合わせて動かしてください。

- 見た目を大きく変えたい場合は `renderBg` をオーバーライドして自前描画にするのが早いです。
- 進行度は `tile.getProgress()` (0–1)、エネルギーは `tile.getEnergyContainer()`、タンクは
  `tile.getFluidTanks()` (GUI 順。バレルは [入力, 出力]、るつぼは [出力]) から取れます。
- 出力スロットは Ex Deorum の機械ふるいと同じ 20 個です。データパックで 1 入力から 21 種類以上に
  分岐するレシピを追加する場合は、`OUTPUT_SLOTS` / `OUTPUT_COLUMNS` / `OUTPUT_ROWS` と
  `WIDTH` / `HEIGHT` を同時に増やし、ウィジェット位置も動かしてください。

## バランス

| 調整項目 | 場所 |
| --- | --- |
| ファクトリーの並列数 (3/5/7/9) | `src/main/java/dev/exmekdeorum/machine/MachineTier.java` |
| ファミリーごとの基本消費・基本容量 | `src/main/java/dev/exmekdeorum/machine/DeorumMachineFamily.java` |
| ブロック生成時のエネルギー設定 | `src/main/java/dev/exmekdeorum/registry/ModMachines.java` の `createBlock` |
| ハンマーの基本時間 (200 tick) | `tile/TileEntityDeorumHammer.java` の `BASE_TICKS` |
| ふるいの基本時間 (100 tick) | `tile/TileEntityDeorumSieve.java` の `BASE_TICKS` |
| バレルの堆肥速度 | Ex Deorum の `EConfig.SERVER.barrelProgressStep` |
| るつぼのファクトリー倍率 (5/10/20/30) | `machine/MachineTier.java` の `getMeltMultiplier()` |
| るつぼの水の溶解速度 (4) / タンク容量 | `tile/TileEntityDeorumCrucibleBase.java` |
| バレルのタンク・堆肥容量 | `tile/TileEntityDeorumBarrel.java` |
| 機械自体のクラフトレシピ | `src/main/resources/data/exmekdeorum/recipe/*.json` |
| ルートテーブル | `src/main/resources/data/exmekdeorum/loot_table/blocks/*.json` |

- ファクトリーは「同じ処理時間のレーンを並列数だけ回す」設計で、1 レーンが動くたびに
  `基本消費 FE/t` を 1 回ぶん消費します (3 レーン同時なら 3 倍)。1 レーンあたりのコストは
  無印の機械と同じです。
- るつぼファクトリーだけはレーンを増やさず、溶解・変換速度が 5/10/20/30 倍になります。
  電力も同じ倍率で増えるため、液体 1 単位あたりの FE は無印と同じです。

### 機械・ティアを増やす

- 新しいティア: `MachineTier` に列挙子を追加すると `ModMachines` のループがブロックと
  BE タイプを自動生成します。テクスチャ・blockstate・モデル・レシピ・言語は
  `python3 tools/generate_assets.py` で生成してください。
- 新しいファミリー: `DeorumMachineFamily` に列挙子を追加し、`ModMachines` のタイル生成
  switch と `initInventory` を実装したタイルクラスを追加します。
