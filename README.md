<p align="center"><img src="docs/icon.png" width="96" height="96" alt="Ex Mekanism Deorum"></p>

# Ex Mekanism Deorum

**Ex Deorum** の機械 (ハンマー / ふるい / バレル / るつぼ / 水るつぼ) を
**Mekanism の機械**として追加する NeoForge Mod です。

Mekanism と同じ操作感 (FE 給電・アップグレード・側面設定・セキュリティ) で、
Ex Deorum のレシピをそのまま自動化できます。レシピは Ex Deorum 側のレシピキャッシュを
直接参照するため、データパックで追加したレシピもそのまま動きます。

- Minecraft **1.21.1** / NeoForge **21.1.265**
- 必須依存: **Ex Deorum 3.12+** / **Mekanism 10.7.19+**
- バージョン: `0.2.0` (テクスチャ・GUI は仮アセットです)

![GUI プレビュー](docs/gui-preview.png)

## 特徴

- **90 ブロック**: 無印の機械 5 種 + ファクトリー 85 種 (Mekanism の 4 ティア + Evolved Mekanism の 5 ティア + Mekanism Extras の 4 ティア + Evolved Mekanism Extras の 4 ティア、各 5 ファミリー)
- **レシピ自動参照**: Ex Deorum のハンマー・ふるい・バレル・るつぼレシピをそのまま使用
  (データパック追加分も自動)
- **Mekanism 統合**: FE で動作。スピード / エネルギー / マフリングのアップグレード、
  側面設定、セキュリティ、レッドストーン制御、JEI 触媒に対応
- **ファクトリー**: Basic 4 / Advanced 8 / Elite 12 / Ultimate 16 レーンの並列処理。
  Evolved Mekanism / Mekanism Extras / Evolved Mekanism Extras を入れると 20〜64 レーンの
  追加ティアも使えます (るつぼは速度倍率)
- **圧縮ハンマー対応**: 圧縮ハンマーをセットすると `compressed_hammer` レシピを処理し、
  結果も `compressed_*` アイテムで出力
- **大容量バッファ**: 入力スロットは 64 × 並列数、内部 FE は 40,000 × 並列数まで保持
- **アニメーション**: 前面 LED が待機中は呼吸、稼働中は明滅 + 走査線

![ブロックの正面 (上から無印 / Basic / Advanced / Elite / Ultimate、左からハンマー・ふるい・バレル・溶岩るつぼ・水るつぼ)](docs/block-fronts.png)

## 導入方法

1. Minecraft 1.21.1 + NeoForge 21.1.265 の環境を用意する
2. [Ex Deorum](https://modrinth.com/mod/ex-deorum) 3.12 以降を入れる
3. [Mekanism](https://modrinth.com/mod/mekanism) 10.7.19 以降を入れる
4. この Mod の jar を `mods/` に入れる

クライアント・サーバーの両方に入れてください。コンフィグはありません。

## 追加される機械

| ファミリー | 無印の機械 | ファクトリー | 1 レーンあたりの消費 |
| --- | --- | --- | --- |
| ハンマー | `hammer_machine` | `basic_/advanced_/elite_/ultimate_hammer_factory` | 20 FE/t |
| ふるい | `sieve_machine` | `basic_/advanced_/elite_/ultimate_sieve_factory` | 40 FE/t |
| バレル | `barrel_machine` | `basic_/advanced_/elite_/ultimate_barrel_factory` | 10 FE/t |
| 溶岩るつぼ | `crucible_machine` | `basic_/advanced_/elite_/ultimate_crucible_factory` | 20 FE/t × 溶解倍率 |
| 水るつぼ | `water_crucible_machine` | `basic_/advanced_/elite_/ultimate_water_crucible_factory` | 20 FE/t × 溶解倍率 |

- ハンマー / ふるい / バレルのファクトリーは Basic 4 / Advanced 8 / Elite 12 / Ultimate 16 本の
  並列処理です。ティアによる速度ボーナスはなく、1 レーンあたりの FE は無印と同じです。
  入力スロットの上限も並列数に合わせて 64 × 並列数まで増え、いろいろなアイテムを同時に
  処理させやすくしています。
- **るつぼファクトリーだけは例外**で、レーンを増やさず溶解速度を Basic 5 倍 / Advanced 10 倍 /
  Elite 20 倍 / Ultimate 30 倍にします。電力も同じ倍率で増えるため、液体 1 単位あたりの FE は
  無印と同じです。
- 設置直後の側面設定は Mekanism の機械と同じです (アイテム: 全側面入力 + 右出力 + 背面
  エネルギー、エネルギー: 全側面入力、バレル/るつぼの液体: 全側面入出力)。ケーブルやパイプは
  設置してそのまま接続できます。
- クリエイティブタブ `Ex Mekanism Deorum` に 90 個すべて入っています。

### Evolved Mekanism / Mekanism Extras の追加ティア

| ティア | 追加元 | 並列 | るつぼ倍率 |
| --- | --- | --- | --- |
| Overclocked | Evolved Mekanism | 20 | 40 倍 |
| Quantum | Evolved Mekanism | 24 | 50 倍 |
| Dense | Evolved Mekanism | 28 | 60 倍 |
| Multiversal | Evolved Mekanism | 32 | 70 倍 |
| Creative | Evolved Mekanism | 36 | 80 倍 |
| Absolute | Mekanism Extras | 20 | 40 倍 |
| Supreme | Mekanism Extras | 24 | 50 倍 |
| Cosmic | Mekanism Extras | 28 | 60 倍 |
| Infinite | Mekanism Extras | 32 | 70 倍 |
| Absolute Overclocked | Evolved Mekanism Extras | 40 | 90 倍 |
| Supreme Quantum | Evolved Mekanism Extras | 48 | 100 倍 |
| Cosmic Dense | Evolved Mekanism Extras | 56 | 110 倍 |
| Infinite Multiversal | Evolved Mekanism Extras | 64 | 120 倍 |

- 追加ティアのブロックは **対応する MOD が入っているときだけ登録されます** (MOD なしなら
  Mekanism の 25 ブロックのみ)。レシピにも `neoforge:mod_loaded` 条件が付きます。
- 追加ティアのレシピは、各連携 MOD のファクトリーと同じ素材構成 (`mekanism:mek_data`) に
  揃えています。`P` は 1 つ下のティア (EM Extras は Extras / Evolved それぞれの 1 つ下) です。
  - Evolved Mekanism: 合金 (hypercharged → subatomic → singular → exoversal → creative) +
    回路 (overclocked → quantum → dense → multiversal → creative) + ウラン / 錫 / 青銅 /
    ネザライト / ネザースター
  - Mekanism Extras: 合金 (radiance → thermonuclear → shining → spectrum) +
    回路 (absolute → supreme → cosmic → infinite) + エメラルド / ネザライト / 精製黒曜石 /
    プルトニウム + ポロニウムのペレット
  - Evolved Mekanism Extras: 両 MOD の合金 + `ACT / PXQ / TCA` パターン (X は
    `mekanism:steel_casing`、P / Q は Extras / Evolved 側の 1 つ下のティア)
- 追加 MOD が入っていると、その MOD のティアインストーラーで Basic → … → Ultimate →
  追加ティアまでアップグレードできます。Evolved Mekanism と Mekanism Extras が同時に入って
  いる場合、インストーラーの経路は Evolved Mekanism 側 (Overclocked 以降) を使います。
- Evolved Mekanism Extras のティアは同 MOD のインストーラー (Absolute Overclocked →
  Supreme Quantum → Cosmic Dense → Infinite Multiversal) に対応しています。この 4 ティアは
  合成で直接作れます (インストーラーはティア内の昇格用)。
- 追加 MOD なしでは追加ティアのブロック・レシピ・JEI 触媒は表示されません。

## 使い方

### レシピの参照元

機械はレシピを自前で持たず、稼働時に Ex Deorum のレシピキャッシュ
(`thedarkcolour.exdeorum.recipe.RecipeCaches`) を直接引きます。

| 機械 | 参照する Ex Deorum レシピ |
| --- | --- |
| ハンマー | `exdeorum:hammer` (圧縮ハンマー使用時は `exdeorum:compressed_hammer`) |
| ふるい | `exdeorum:sieve` |
| バレル | `exdeorum:barrel_compost` / `barrel_mixing` / `barrel_fluid_mixing` / `barrel_fluid_transformation` |
| 溶岩るつぼ | `exdeorum:lava_crucible` / `exdeorum:crucible_heat_source` |
| 水るつぼ | `exdeorum:water_crucible` |

### ハンマー / ふるい

- **ハンマー**: 1 個あたり基本 200 tick。ハンマースロット (スロット 22) のハンマーを 1 回ごとに
  1 耐久消費し、効率 (Efficiency) と幸運 (Fortune) が効きます。出力は 20 スロット。
  スロット 22 に圧縮ハンマー (`#exdeorum:compressed_hammers`) を入れると、圧縮ブロックは
  `compressed_hammer` レシピで処理します。入力とハンマーの挿入順は問いません。圧縮ハンマーの
  結果は圧縮段階を保ち、9 個ぶんのドロップは 1 個の `exdeorum:compressed_*` にまとまります。
- **ふるい**: 1 個あたり基本 100 tick。メッシュスロット (スロット 22) が必須で、
  メッシュの効率 / 幸運エンチャントが効きます。圧縮ブロックは自動判別され、同じメッシュで
  Ex Deorum の `compressed_sieve` レシピを使って処理されます。`by_hand_only: true` のレシピは機械では
  抽選対象外です。
- **入力の扱い**: 処理中のアイテムは入力スロットに残り、1 回の処理が完了したときにだけ
  1 個消費します (入れた瞬間に減ることはありません)。

### バレル

- アイテム入力 2 スロット (どちらでも同じ処理に使えます)、アイテム出力 1 スロット、
  液体入力タンクと液体出力タンクを各 1000 mB × 並列数
- 堆肥化・アイテム+液体の混合・真上の液体との混合 (in-world mixing)・下 3×3 の触媒による
  液体変換・雨天時の給水に対応
- **堆肥化**: 入れたアイテムがすぐに `compost` 値として貯まり、進行バーは「貯まった割合」を
  表示します。満杯になるとバーは土を作る工程に切り替わり、完了すると土が出力されます。
- **アイテム+液体の混合** (例: 水 + 雪玉 → 氷) は完成品がアイテム出力スロットに入ります。
- 外部からの液体注入は入力タンクへ、変換結果は出力タンクへ入り、パイプで出し入れできます。
  変換は入力タンクが満杯のときだけ進み、完了時に入力タンクが空になって出力タンクに結果液体が
  入ります。

### るつぼ

- **溶岩るつぼ**: **1 個ずつ**溶解します。入力スロットのアイテムは処理中も残り、進行バーが
  0→1 まで進んで満タンになると、レシピの液体 (例: 丸石 250 mB) がタンクに入ってアイテムが
  1 個消費されます。速度は**真下ブロックの熱値** (`crucible_heat_source`) で決まり、熱源が
  ないとバーは進まずアイテムも消費しません (エネルギーも消費しません)。タンク 4000 mB ×
  溶解倍率で、外部からの注入はできません (出力専用。パイプで取り出します)。
- **水るつぼ**: `exdeorum:water_crucible` レシピ専用。熱源不要で、基準速度は固定 4
  (10 tick あたり 4 mB)。るつぼファクトリーでは速度が 5〜30 倍になり、電力も同じ倍率で
  消費するので「液体 1 mB あたりの電力」は変わりません。

### エネルギー容量

内部バッファは **40,000 FE × 並列数** です。ファクトリーは 1 tick の消費も並列数だけ
増えるため、容量も同じ比率で増やして「バッファで動ける時間」を無印と揃えています。

| ティア | 並列数 | 容量 |
| --- | --- | --- |
| 無印 | 1 | 40,000 FE |
| Basic | 4 | 160,000 FE |
| Advanced | 8 | 320,000 FE |
| Elite | 12 | 480,000 FE |
| Ultimate | 16 | 640,000 FE |
| Evolved (Overclocked〜Creative) | 20 / 24 / 28 / 32 / 36 | 800,000 〜 1,440,000 FE |
| Extras (Absolute〜Infinite) | 20 / 24 / 28 / 32 | 800,000 〜 1,280,000 FE |
| EM Extras (hybrid) | 40 / 48 / 56 / 64 | 1,600,000 〜 2,560,000 FE |

速度アップグレードを盛って 1 tick の消費が容量を超える場合は、容量を上限に動き続けます
(完全停止はしません)。入力スロットは **64 × 並列数** (Basic 256、Ultimate 1,024 など) まで
スタックできます。

## クラフトレシピ

無印の機械は、Mekanism の基本回路 + 赤石 + 鉄のリングで Ex Deorum の機械パーツを囲みます
(パターンはすべて `ACA / IPI / ACA`、`P` が中央)。

| 機械 | 中央 (P) | 左右 (I) |
| --- | --- | --- |
| ハンマー機 | `exdeorum:mechanical_hammer` | `minecraft:iron_ingot` |
| ふるい機 | `exdeorum:mechanical_sieve` | `minecraft:iron_ingot` |
| バレル機 | `exdeorum:oak_barrel` | `minecraft:iron_ingot` |
| 溶岩るつぼ | `exdeorum:porcelain_crucible` | `minecraft:lava_bucket` |
| 水るつぼ | `exdeorum:porcelain_crucible` | `minecraft:water_bucket` |

周囲は `mekanism:basic_control_circuit` (C)・`minecraft:redstone` (A) です。

ファクトリーは Mekanism の工場と同じアップグレード段階で、1 つ下のティアを中央に置きます。

| ティア | 合金 (A) | 回路 (C) | インゴット (I) |
| --- | --- | --- | --- |
| Basic | `minecraft:redstone` | `mekanism:basic_control_circuit` | `minecraft:iron_ingot` |
| Advanced | `mekanism:alloy_infused` | `mekanism:advanced_control_circuit` | `mekanism:ingot_osmium` |
| Elite | `mekanism:alloy_reinforced` | `mekanism:elite_control_circuit` | `minecraft:gold_ingot` |
| Ultimate | `mekanism:alloy_atomic` | `mekanism:ultimate_control_circuit` | `minecraft:diamond` |
| 追加ティア | 各 MOD の合金 | 各 MOD の回路 | 各 MOD のインゴット / ペレット |

追加ティアは連携 MOD 本来のレシピを再現しています (EM Extras だけは `ACT / PXQ / TCA` の
9 マス全体を使うレシピ)。

追加ティアのレシピは対応 MOD がある時だけ読み込まれます。

## 互換 MOD（すべて任意）

> Mekanism: Empowered の追加アップグレード (`empowered_speed` / `empowered_energy` /
> `io_capacity` / `fast_item_insert` / `fast_item_eject` / `auto_inserter`) も、当 MOD の
> 全機械で受け付けます (MOD があるときだけリフレクションで登録)。

この MOD は単体で動作します。以下が入っているときだけ、対応する連携が有効になります。

| MOD | 連携内容 |
| --- | --- |
| Evolved Mekanism / Mekanism Extras / Evolved Mekanism Extras | 各 MOD のティアインストーラーで、この MOD のファクトリーを Basic → Advanced → Elite → Ultimate → その MOD の追加ティアまでアップグレードできます（追加ティアのブロックは対応 MOD がある時だけ登録されます）。 |
| Mekanism: Empowered | 速度・エネルギー倍率アップグレードは Mekanism の `MekanismUtils` 経由で計算されるため、自動的に反映されます（専用コードなし）。 |
| Mek Energistics | AE2 のパターン自動化 SPI に 90 ブロックを登録します。アイテム/液体の入出力、ハンマー・メッシュの常設入力、ファクトリーのパラレル処理に対応します。 |
| JEI | Ex Deorum のレシピにこの MOD の機械を触媒として表示します。 |

## JEI 連携

JEI は任意依存です (なくても動作します。配布 jar には含まれません)。

- 独自カテゴリは追加せず、Ex Deorum のカテゴリ (`exdeorum:hammer`, `sieve`,
  `barrel_compost` など) にこの Mod の機械を触媒として追加します
- GUI の進行バーを左クリックすると、対応する JEI レシピを直接開きます
- EMI 併用時は Ex Deorum のカテゴリ名前空間 (`exdeorum_emi`) に追従します

![GUI](docs/gui-preview.png)

![側面設定ウィンドウ](docs/side-config-preview.png)

![バレル GUI (2 入力 + 入力/出力タンク)](docs/barrel-preview.png)

![水るつぼ GUI (入力 + 液体出力)](docs/water-crucible-preview.png)

![溶岩るつぼ GUI](docs/crucible-preview.png)

![ハンマーファクトリー GUI (20 出力)](docs/hammer-factory-preview.png)

## 既知の制限

- ブロックテクスチャと GUI は仮アセットです (差し替え前提)。正面パネルは手描きのベースに
  ファミリーごとの機械的な柄 (リブ / パンチ穴 / ルーバー / ハッチ / フィン) を描き、
  側面・上面・底面は Mekanism の Crusher テクスチャを複製しています。
  詳細は [docs/CUSTOMIZE.md](docs/CUSTOMIZE.md) を参照してください。
- 圧縮ハンマーはハンマー機械専用です。ふるいはメッシュだけで通常/圧縮ブロックの
  両方を処理します。
- バレルの「熱い液体による焼失」は実装していません (破壊される挙動は機械向きでないため)。
- バレルの外部からの液体注入は「堆肥 0 かつアイテム入力 2 スロットが空」のときだけ許可して
  います。

## カスタマイズ / 開発

- テクスチャ・GUI・バランスの調整: [docs/CUSTOMIZE.md](docs/CUSTOMIZE.md)
- ビルド環境・依存の仕組み・バージョン更新: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
- 変更履歴: [CHANGELOG.md](CHANGELOG.md)

```console
$ nix develop
$ gradle build
```

## ライセンス / クレジット

- この Mod: GNU General Public License v3.0 ([LICENSE](LICENSE))
- ブロックのベーステクスチャと GUI 背景: Mekanism (MIT License,
  Copyright Aidancbrady and contributors)
- Ex Deorum のレシピ・タグ・機械パーツを利用しています。Ex Deorum は
  [thedarkcolour](https://github.com/thedarkcolour/ExDeorum) による Mod です
- Minecraft EULA (<https://aka.ms/MinecraftEULA>) への同意は利用者の責任です
