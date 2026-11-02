# 開発環境

JDK 21・Gradle・依存 jar は Nix flake が供給し、Mod 本体のビルドは Gradle
(ModDevGradle) が行います。

## 必要なもの

- Nix (flakes 有効)。NixOS 26.11 / x86_64-linux で動作確認
- 初回の Gradle 実行時のみネットワーク (Maven Central, maven.neoforged.net, Modrinth CDN)
- jar はリポジトリに同梱せず、Nix が取得したストアパスを参照します

## クイックスタート

```console
$ nix develop
$ gradle build        # build/libs/exmekdeorum-0.2.1.jar
$ gradle runClient    # Minecraft クライアント (dev プロファイル)
$ gradle runServer    # 専用サーバー (--nogui, run/ を使用)
$ gradle runData      # データ生成 (src/generated/resources)
```

`nix develop` の shellHook が次を設定します。

- `JAVA_HOME`: Nix の JDK 21
- `GRADLE_USER_HOME`: `.gradle-user-home/` (リポジトリ内。`~/.gradle` に書けない環境でも動く)
- 既定では依存は **CurseForge Maven** (`https://cursemaven.com`) から取得します。
- `EXDEORUM_JAR` / `MEKANISM_JAR` / `JEI_JAR` / `MEKENERGISTICS_JAR`: オフラインビルド用の
  ハッシュ固定 jar パス (任意。Nix シェルでは自動で export されます)

### Linux / NixOS でクライアントが起動しない

`Failed to find a valid GLFW profile` / `GLX: Failed to load GLX` は、JVM から
`libGLX` / `libGL` が見えていないことが原因です。`flake.nix` の devShell は libglvnd・
Mesa・X11・ALSA/Pulse を追加し、`LD_LIBRARY_PATH` に `/run/opengl-driver/lib` とそれらを
設定します。

- flake を更新したら、新しいターミナルで `nix develop` をやり直す
- 古い Gradle デーモンが残っている場合は `gradle --stop` してから `gradle runClient`
- それでもだめなら `nix develop --command gradle --no-daemon runClient` (`--no-daemon` で起動)
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

バージョンの出典は `build.gradle` の CurseForge 座標 (オフライン用のハッシュは `mods.nix`)、要求範囲は `gradle.properties` と
`src/main/resources/META-INF/neoforge.mods.toml` にあります。`gradle.properties` の範囲を
変えたら `mods.nix` のバージョンも揃えてください。

## ディレクトリ構成

| パス | 役割 |
| --- | --- |
| `flake.nix` | devShell (JDK 21, Gradle)・依存 jar のパッケージ・checks |
| `mods.nix` | 必須 Mod のバージョン・URL・SRI/SHA-1/SHA-512・サイズ |
| `build.gradle` | ModDevGradle の設定・依存 jar の配線・run 構成 |
| `gradle.properties` | Mod メタデータと依存バージョン範囲 |
| `src/main/java/dev/codeonigiri/exmekdeorum/machine/` | ティア・ファミリー定義とブロック属性 |
| `src/main/java/dev/codeonigiri/exmekdeorum/tile/` | 5 種類の機械のブロックエンティティ |
| `src/main/java/dev/codeonigiri/exmekdeorum/registry/` | ブロック・BE タイプ・コンテナ・クリエイティブタブ |
| `src/main/java/dev/codeonigiri/exmekdeorum/client/` | GUI とスクリーン登録 |
| `src/main/java/dev/codeonigiri/exmekdeorum/compat/jei/` | JEI プラグイン (触媒登録・進行バークリック) |
| `src/main/resources/assets/exmekdeorum/` | テクスチャ・モデル・blockstate・言語 |
| `src/main/resources/data/exmekdeorum/` | クラフトレシピ・ルートテーブル |
| `docs/` | スクリーンショットとドキュメント |

## 依存の仕組み

1. `build.gradle` が `curse.maven:<slug>-<projectId>:<fileId>` を宣言し、`cursemaven.com`
   から解決する (Ex Deorum 3.12 / Mekanism 10.7.19.85 / JEI 19.54.0.429 / Mek Energistics 3.0.8)
2. 互換 MOD (Evolved Mekanism + Iglee's Library / Mekanism Extras / Evolved Mekanism Extras /
   Mekanism: Empowered + Core / Kotlin for Forge) は**既定で** dev の classpath に入り、
   互換コードを実環境で試せる。外すときは `-PnoAddons`
   (`gradle runServer -PuseMavenDeps -PnoAddons`)
3. 両 jar は `META-INF/neoforge.mods.toml` を持つため、dev 実行時に FML が Mod として読み込む
4. この Mod 自身の `neoforge.mods.toml` が Ex Deorum / Mekanism を `type = "required"` で宣言する
5. オフライン / 再現性が必要な場合は `mods.nix` の固定 jar を `EXDEORUM_JAR` などで渡すか
   `libs/` に置くと、そちらが優先される (`-PuseMavenDeps` で Maven 強制)
5. JEI は `JEI_JAR` から dev の classpath にだけ追加する任意依存 (`type = "optional"`,
   client のみ)。jar には含めない

`nix develop` の外でビルドする場合は、jar の場所を明示します。

```console
$ gradle build -PexdeorumJar=/path/to/exdeorum-3.12.jar -PmekanismJar=/path/to/Mekanism-1.21.1-10.7.19.85.jar
```

または `libs/` に jar を置いても解決します (git 管理外)。

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
$ nix flake check
$ nix develop -c gradle build
```

- `checks.mods`: 依存 jar がハッシュ通りに取得できる
- `gradle build`: Mod がコンパイルでき、`build/libs/exmekdeorum-0.2.1.jar` ができる
- 起動確認 (任意): `nix develop -c gradle runServer` が `run/` で起動する

## 依存 Mod の更新

1. Modrinth で新バージョンの jar URL / SHA-512 / SHA-1 / サイズを取得
2. CurseForge の該当ファイル ID を控えて `build.gradle` の座標
   (`curse.maven:<slug>-<projectId>:<fileId>`) を更新する。オフライン用に `mods.nix` も
   (url, hash (SRI), sha1, sha512, size, version, fileName) を揃える
3. `gradle.properties` の対応する範囲を更新
4. 検証:

```console
$ nix build .#mods
$ nix flake check
$ nix develop -c gradle build
```

SRI ハッシュは次のように作れます。

```console
$ nix hash convert --hash-algo sha512 --to sri <sha512-hex>
```

## NeoForge / Minecraft の更新

- `mods.nix` の `neoforgeVersion` / `minecraftVersion` を変更
- `gradle.properties` の `neo_version` / `neo_version_range` / `minecraft_version_range` /
  `loader_version_range` を変更
- `nix develop -c gradle build` で再解決 (初回は新しい NeoForge をダウンロード)

`gradle.properties` の `neo_version` が実際に使う NeoForge、`mods.nix` の
`neoforgeVersion` は依存 jar の取得元バージョンです。

## ライセンスと再配布

- この Mod は GNU General Public License v3.0 (`LICENSE`)
- Ex Deorum / Mekanism の jar は再配布せず、Modrinth CDN から取得する
- GUI 背景とブロックのベーステクスチャは Mekanism のもの (MIT,
  Copyright Aidancbrady and contributors)
- Minecraft EULA (<https://aka.ms/MinecraftEULA>) への同意は利用者の責任

## Optional integration API

`build.gradle` also compiles against Mek Energistics 3.0.8 (`MEKENERGISTICS_JAR`,
`-PmekenergisticsJar=...`, or `libs/mekenergistics-3.0.8.jar`) because the pattern-automation
mixin implements its SPI at compile time. The jar is `compileOnly`: it is never shipped, and the
mixin plugin skips the integration when the mod is not installed, so it is not a runtime
dependency. The Nix dev shell exports the pinned jar.

Mek Energistics (and its AE2 / Applied Mekanistics / GuideME dependencies) is part of the
default development classpath together with the other optional add-ons, so a plain
`gradle runServer` / `runClient` loads it. To start without every optional mod, pass
`-PnoAddons`:

```sh
nix develop --command gradle --no-daemon runServer -PnoAddons
```

The mixin itself is declared through `[[mixins]] config = "exmekdeorum.mixins.json"` in
`META-INF/neoforge.mods.toml`; without that declaration the config is never applied.
