# Changelog

このファイルは [Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) の形式に沿っています。
バージョン番号は [Semantic Versioning](https://semver.org/lang/ja/) に従います。

## [Unreleased]

### Added

- **Mekanism: Empowered 連携**: `empowered_speed` / `empowered_energy` / `io_capacity` /
  `fast_item_insert` / `fast_item_eject` / `auto_inserter` を全機械で受け付けます。
- **Mekanism Extras / Evolved Mekanism Extras 連携**: 追加ティア (Absolute〜Infinite、
  Absolute Overclocked〜Infinite Multiversal) と、各 MOD のティアインストーラーに対応。
- **ティアインストーラー**: Basic → Advanced → Elite → Ultimate → 各連携 MOD の追加ティアへ
  アップグレード可能。在庫・進行状況は引き継がれます。
- **連携 MOD 準拠のレシピ**: 追加ティアのファクトリーは各 MOD 本来の `mekanism:mek_data`
  レシピ (合金・回路・インゴット/ペレット) を再現。
- **圧縮ふるい**: 入力が `compressed_sieve` レシピに一致する場合、メッシュだけで自動的に
  圧縮レシピで処理。
- **圧縮ハンマー出力**: 圧縮ハンマーでの処理結果を `compressed_*` アイテムで出力
  (9 個分を 1 個にまとめる。対応アイテムがない場合は通常アイテム)。
- **テクスチャアニメーション**: 前面 LED が待機中はゆっくり呼吸、稼働中は明滅 + 走査線。
- **ME パターン自動化**: Mek Energistics の SPI に全機械を登録 (アイテム/液体入出力、
  ハンマー・メッシュの常設入力、ファクトリーの並列処理)。
- **JEI**: 圧縮ふるいの触媒、進行バーのクリックでレシピを開く機能。
- **入力容量の拡張**: 入力スロットが 64 × 並列数までスタック可能。
- **エネルギー容量の拡張**: 内部バッファが 40,000 FE × 並列数。

### Changed

- **並列数の増加**: Basic 4 / Advanced 8 / Elite 12 / Ultimate 16。
  追加ティアは Evolved 20〜36、Extras 20〜32、EM Extras 40〜64 (2 ティアの和)。
- 上面テクスチャの向きを Mekanism の `orientable_with_bottom` に合わせて 180° 回転。
- 無印機械・ファクトリーのレシピを `ACA / IPI / ACA` に統一 (るつぼはバケツを `I` に使用)。
- 開発環境は既定で連携 MOD をすべて classpath に含める (`-PnoAddons` で除外)。
- 依存解決を CurseForge Maven 既定に変更 (`-PuseMavenDeps` / env / `libs/` で上書き可能)。

### Fixed

- 入力アイテムが挿入直後に消費されていた問題 (完了時に 1 個消費するよう変更)。
- 側面設定の既定値が `NONE` のため外部電力・ケーブルが繋がらない問題
  (`SIDE_CONFIG` コンポーネント + 既存ワールド向けの `onLoad` 移行)。
- 1 個だけ処理するときに進行バーが並列数で薄まる問題 (稼働レーンのみで平均)。
- 高速アップグレードで 1 tick の消費が容量を超えて停止する問題 (容量でクランプ)。
- 速度と Empowered 速度を併用したときの計算・登録。
- バレルに液体が入っていると堆肥化できない問題。
- 水るつぼの液体変換とタンク出力。

## [0.1.0] - 2026-11-02

初回リリース。

- 無印 5 種 + Basic / Advanced / Elite / Ultimate ファクトリー (5 ファミリー × 4 ティア)
- Ex Deorum のハンマー / ふるい / バレル / 溶岩るつぼ / 水るつぼレシピを自動参照
- Mekanism の FE・アップグレード・側面設定・セキュリティ・JEI 触媒に対応
- 仮テクスチャ / GUI / MOD アイコン
