# カスタマイズ (テクスチャ / GUI / バランス)

この Mod の見た目と数値は、すべてリポジトリ内のテクスチャ・GUI コード・定数で完結しています。
ここでは「どこを触れば何が変わるか」をまとめます。

- [テクスチャ / モデル](#テクスチャ--モデル)
- [GUI](#gui)
- [バランス](#バランス)

## テクスチャ / モデル

ブロック 1 種につき 5 枚の PNG、モデル 2 種、blockstate、アイテムモデルを持ちます。
すべて `src/main/resources/` 配下の通常のリソースなので、PNG / JSON を直接編集して
`gradle build` すれば反映されます (アセットの自動生成はしません)。

| パス | 内容 |
| --- | --- |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_front.png` | 正面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_front_active.png` | 稼働中の正面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_side.png` | 左右 / 背面 (共通) |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_top.png` | 上面 |
| `src/main/resources/assets/exmekdeorum/textures/block/<block>_bottom.png` | 底面 |
| `src/main/resources/assets/exmekdeorum/models/block/<block>.json` | モデル (`minecraft:block/orientable_with_bottom`) |
| `src/main/resources/assets/exmekdeorum/models/block/<block>_active.json` | 稼働中モデル |
| `src/main/resources/assets/exmekdeorum/blockstates/<block>.json` | `facing` × `active` の 8 バリアント |
| `src/main/resources/assets/exmekdeorum/models/item/<block>.json` | アイテムモデル |
| `src/main/resources/assets/exmekdeorum/textures/gui/deorum_machine.png` | 共通 GUI (256×256 nine-slice) |

### 面の使い分け

- `_front` / `_front_active`: 正面 (`facing` で回転し、稼働中は `_front_active` に切り替わる)
- `_side`: 左右と背面で共通
- `_top` / `_bottom`: 上面と底面
- ファクトリーは正面の LED (上端 1 px・中央 6 px) の色だけティアで変えます。色は Mekanism の
  `BaseTier` に合わせています (Basic: mint / Advanced: coral / Elite: cyan / Ultimate: magenta)

### モデルと blockstate

- モデルはバニラの親モデル (`minecraft:block/orientable_with_bottom`) とテクスチャ参照だけの薄い JSON です。
  面を増やしたい場合は `elements` を持つモデルに差し替えてください
  (Blockbench で書き出した JSON をそのまま置けます)。
- blockstate は `facing` × `active` の 8 バリアントで、`active=true` のときだけ `_active` モデルを向きます。

### GUI 背景

`textures/gui/deorum_machine.png` は Mekanism の `gui/base.png` を複製した固定アセット (256×256) です。
4 px の nine-slice で描画しているため、枠はシートの外周 4 px 以内に置き、中央は単色にしてください。

### MOD アイコン

- ゲーム内の Mod 一覧用: `src/main/resources/icon.png` (128×128、`neoforge.mods.toml` の `logoFile = "icon.png"`)
- GitHub / CurseForge 用: `docs/icon.png` (512×512)
- どちらも PNG を同じサイズで上書きして `gradle build` すれば反映されます。

## GUI

GUI は `src/main/java/dev/codeonigiri/exmekdeorum/client/GuiDeorumMachine.java` の 1 クラスです。

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
| ファクトリーの並列数 (4/8/12/16、追加ティアは 20〜64) | `src/main/java/dev/codeonigiri/exmekdeorum/machine/MachineTier.java` |
| ファミリーごとの基本消費・基本容量 | `src/main/java/dev/codeonigiri/exmekdeorum/machine/DeorumMachineFamily.java` |
| ブロック生成時のエネルギー設定 | `src/main/java/dev/codeonigiri/exmekdeorum/registry/ModMachines.java` の `createBlock` |
| 内部バッファ容量 | `DeorumMachineFamily.BASE_ENERGY_STORAGE` (40,000) × 並列数。`createBlock` で `Math.multiplyExact` |
| 入力スロット上限 | `TileEntityDeorumMachine.inputLimit()` = 64 × 並列数 (`inventory/DeorumInputSlot`) |
| ハンマーの基本時間 (200 tick) | `tile/TileEntityDeorumHammer.java` の `BASE_TICKS` |
| ふるいの基本時間 (100 tick) | `tile/TileEntityDeorumSieve.java` の `BASE_TICKS` |
| バレルの堆肥速度 | Ex Deorum の `EConfig.SERVER.barrelProgressStep` |
| るつぼのファクトリー倍率 (基本ティア 5/10/20/30、追加ティア 40〜120) | `machine/MachineTier.java` の各列挙子の第 2 引数 (`getMeltMultiplier()`) |
| るつぼの水の溶解速度 (4) / タンク容量 | `tile/TileEntityDeorumCrucibleBase.java` |
| バレルのタンク・堆肥容量 | `tile/TileEntityDeorumBarrel.java` |
| 機械自体のクラフトレシピ | `src/main/resources/data/exmekdeorum/recipe/*.json` |
| ルートテーブル | `src/main/resources/data/exmekdeorum/loot_table/blocks/*.json` |

- ファクトリーは「同じ処理時間のレーンを並列数だけ回す」設計で、1 レーンが動くたびに
  `基本消費 FE/t` を 1 回ぶん消費します (3 レーン同時なら 3 倍)。1 レーンあたりのコストは
  無印の機械と同じです。
- るつぼファクトリーだけはレーンを増やさず、溶解・変換速度がティアの倍率ぶん上がります
  (基本ティアで 5 / 10 / 20 / 30 倍、追加ティアでは最大 120 倍)。
  電力も同じ倍率で増えるため、液体 1 単位あたりの FE は無印と同じです。
- 追加ティアの並列数とるつぼ倍率は次のとおりです (出典: `MachineTier`)。
  - Evolved Mekanism (Overclocked〜Creative): 20 / 24 / 28 / 32 / 36 レーン、るつぼ 40〜80 倍
  - Mekanism Extras (Absolute〜Infinite): 20 / 24 / 28 / 32 レーン、るつぼ 40〜70 倍
  - Evolved Mekanism Extras (Absolute Overclocked〜Infinite Multiversal):
    40 / 48 / 56 / 64 レーン、るつぼ 90〜120 倍
- 追加ティアのブロックは、**対応する Mod が入っているときだけ登録されます**
  (`MachineTier.isAvailable()` で判定し、`ModMachines` のループでスキップ)。
  Mod が入っている環境では、その Mod のティア属性 (インストーラー用) も付きます。

### 機械・ティアを増やす

- 新しいティア: `MachineTier` に列挙子を追加すると `ModMachines` のループがブロックと
  BE タイプを自動生成します。テクスチャ・blockstate・モデル・レシピ・言語は
  `src/main/resources/` の既存ファイルを雛形に手動で追加してください (ローカルの
  `tools/generate_assets.py` を使う場合は `TIERS` と `TIER_LED` に追記して再生成できます)。
- 新しいファミリー: `DeorumMachineFamily` に列挙子を追加し、`ModMachines` のタイル生成
  switch と `initInventory` を実装したタイルクラスを追加します。
