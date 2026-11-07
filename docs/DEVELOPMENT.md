# 開発環境

JDK 21・Gradle・クライアント用のネイティブライブラリは Nix flake が供給し、Mod 本体のビルドは
Gradle (ModDevGradle) が行います。Mod の依存はすべて CurseForge Maven から解決するので、
Nix 側で jar を固定する必要はありません。

- [必要なもの](#必要なもの)
- [クイックスタート](#クイックスタート)
- [固定バージョン](#固定バージョン)
- [ディレクトリ構成](#ディレクトリ構成)
- [依存の仕組み](#依存の仕組み)
- [開発の流れ](#開発の流れ)
- [動作確認](#動作確認)
- [依存 Mod の更新](#依存-mod-の更新)
- [NeoForge / Minecraft の更新](#neoforge--minecraft-の更新)
- [任意連携 (Mek Energistics)](#任意連携-mek-energistics)
- [ライセンスと再配布](#ライセンスと再配布)

## 必要なもの

- Nix (flakes 有効)。NixOS 26.11 / x86_64-linux で動作確認
- 初回の Gradle 実行時のみネットワーク (Maven Central, maven.neoforged.net, cursemaven.com)

## クイックスタート

```console
$ nix develop
$ gradle build        # build/libs/exmekdeorum-<version>.jar
$ gradle runClient    # Minecraft クライアント (dev プロファイル)
$ gradle runServer    # 専用サーバー (--nogui, run/ を使用)
$ gradle runData      # データ生成 (src/generated/resources)
```

`nix develop` の shellHook が次を設定します。

- `JAVA_HOME`: Nix の JDK 21
- `GRADLE_USER_HOME`: `.gradle-user-home/` (リポジトリ内。`~/.gradle` に書けない環境でも動く)
- `LD_LIBRARY_PATH`: クライアント用の GL / X11 / 音声ライブラリ

### Linux / NixOS でクライアントが起動しない

`Failed to find a valid GLFW profile` / `GLX: Failed to load GLX` は、JVM から
`libGLX` / `libGL` が見えていないことが原因です。`flake.nix` の devShell は libglvnd・
Mesa・X11・ALSA/Pulse を追加し、`LD_LIBRARY_PATH` に `/run/opengl-driver/lib` とそれらを
設定します。

- flake を更新したら、新しいターミナルで `nix develop` をやり直す
- 古い Gradle デーモンが残っている場合は `gradle --stop` してから `gradle runClient`
- それでもだめなら `nix develop --command gradle --no-daemon runClient`
- IDE から起動する場合も、IDE 自体を `nix develop` の中で立ち上げる

`oshi` の `Did not find udev library` 警告は無害です。

## 固定バージョン

| コンポーネント | バージョン | この Mod が要求する範囲 |
| --- | --- | --- |
| Minecraft | 1.21.1 | `[1.21.1]` |
| NeoForge | 21.1.265 | `[21.1.265,)` |
| Ex Deorum | 3.12 | `[3.12,)` |
| Mekanism | 10.7.19.85 (jar のビルド番号) | `[10.7.19,)` |
| JEI (開発専用・任意) | 19.54.0.429 | `[19.21,)` (クライアントのみ) |

バージョンの出典は `build.gradle` の CurseForge 座標 (`curse.maven:<slug>-<projectId>:<fileId>`)、
要求範囲は `gradle.properties` と `src/main/resources/META-INF/neoforge.mods.toml` にあります。

## ディレクトリ構成

| パス | 役割 |
| --- | --- |
| `flake.nix` | devShell (JDK 21, Gradle, クライアント用ネイティブライブラリ) |
| `build.gradle` | ModDevGradle の設定・依存 Mod の座標・run 構成 |
| `gradle.properties` | Mod メタデータと依存バージョン範囲 |
| `src/main/java/dev/codeonigiri/exmekdeorum/machine/` | ティア・ファミリー定義とブロック属性 |
| `src/main/java/dev/codeonigiri/exmekdeorum/tile/` | 5 種類の機械のブロックエンティティ |
| `src/main/java/dev/codeonigiri/exmekdeorum/registry/` | ブロック・BE タイプ・コンテナ・クリエイティブタブ |
| `src/main/java/dev/codeonigiri/exmekdeorum/client/` | GUI とスクリーン登録 |
| `src/main/java/dev/codeonigiri/exmekdeorum/compat/` | 任意連携の共通処理 (ティア属性、Mek Energistics 連携) |
| `src/main/java/dev/codeonigiri/exmekdeorum/compat/jei/` | JEI プラグイン (触媒登録・進行バークリック) |
| `src/main/java/dev/codeonigiri/exmekdeorum/mixin/` | Mek Energistics 用 Mixin と、その適用可否を判定するプラグイン |
| `src/main/java/dev/codeonigiri/exmekdeorum/block/` / `inventory/` | 機械ブロック本体 / 入力スロット |
| `src/main/resources/assets/exmekdeorum/` | テクスチャ・モデル・blockstate・言語 |
| `src/main/resources/data/exmekdeorum/` | クラフトレシピ・ルートテーブル |
| `docs/` | スクリーンショットとドキュメント |

## 依存の仕組み

`build.gradle` の `dependencies` にすべての Mod を `curse.maven` 座標で列挙しています。
開発実行ではすべての Mod が classpath に入り、ビルドした jar にはどれも含まれません。

- 必須: Ex Deorum / Mekanism
- 開発専用: JEI
- 互換対象: Mek Energistics (+ AE2 / Applied Mekanistics / GuideME)、Evolved Mekanism
  (+ Iglee's Library)、Mekanism Extras、Evolved Mekanism Extras、Mekanism: Empowered
  (+ Core / Kotlin for Forge)

`gradle runClient` / `runServer` は追加オプションなしでこれらすべてを読み込みます。

## 開発の流れ

- IDE: IntelliJ IDEA を使う場合、`nix develop` の中で IDE を起動してから Gradle プロジェクト
  として import し、Gradle JVM / Project SDK を JDK 21 にする。`gradle runClient` の構成が
  IDE の実行構成にも現れる (ModDevGradle が生成)。
- サーバー: `gradle runServer` は `run/eula.txt` を自動で書く。EULA への同意は利用者の責任。
  起動後はコンソールに `stop` と入力すると安全に停止する。
- アセット: `src/main/resources/` の PNG / JSON を直接編集する (自動生成はしません)
- 起動確認: `nix develop -c gradle runServer` で 1.21.1 の専用サーバーを起動し、
  機械を設置してレシピが動くことを確認する

## 動作確認

```console
$ nix develop -c gradle build
```

- `gradle build`: Mod がコンパイルでき、`build/libs/exmekdeorum-<version>.jar` ができる
- 起動確認 (任意): `nix develop -c gradle runServer` が `run/` で起動する

## 依存 Mod の更新

1. CurseForge で新ファイルの ID を控え、`build.gradle` の座標
   (`curse.maven:<slug>-<projectId>:<fileId>`) を更新する
2. `gradle.properties` の対応するバージョン範囲を更新する
3. `gradle build` と `gradle runServer` で解決・起動を確認する

## NeoForge / Minecraft の更新

- `gradle.properties` の `neo_version` / `neo_version_range` / `minecraft_version_range` /
  `loader_version_range` を変更
- `nix develop -c gradle build` で再解決 (初回は新しい NeoForge をダウンロード)

## ライセンスと再配布

- この Mod は GNU General Public License v3.0 (`LICENSE`)
- Ex Deorum / Mekanism などの jar は再配布せず、CurseForge Maven から取得する
- GUI 背景とブロックのベーステクスチャは Mekanism のもの (MIT,
  Copyright Aidancbrady and contributors)
- Minecraft EULA (<https://aka.ms/MinecraftEULA>) への同意は利用者の責任

## 任意連携 (Mek Energistics)

Mek Energistics のパターン自動化 SPI はコンパイル時に参照し、実行時は Mixin で
`TileEntityDeorumMachine` にインターフェースを追加します。Mixin 設定は
`META-INF/neoforge.mods.toml` の `[[mixins]] config = "exmekdeorum.mixins.json"` で宣言して
いるため、この宣言を消すと連携が無効になります。Mek Energistics が無い環境では
`DeorumMixinPlugin` が Mixin を適用しないので、通常どおり起動します。
