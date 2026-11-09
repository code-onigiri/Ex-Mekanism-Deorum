# Ex Mekanism Deorum — 1.20.1 backport notes

Branch: `backport/1.20.1` (never pushed, never tagged).
Target: Minecraft 1.20.1 / Forge 47.2.0 / Java 17 / ForgeGradle 6, official mappings.
Source: NeoForge 1.21.1 / Java 21.

Verification in this environment is **static only** — the Gradle daemon is broken in
this VM (known issue), so no `gradle build` was run. Build verification happens on
GitHub Actions (workflow now also triggers on `backport/1.20.1`).

## Phase 1 — build system, metadata, CI

- `build.gradle`
  - Replaced `net.neoforged.moddev` with `net.minecraftforge.gradle` `[6.0,6.2)`.
  - Java toolchain 21 → 17.
  - Added `minecraft { mappings channel: 'official', version: '1.20.1' }` plus
    `client` / `server` / `data` runs (FG6 `args` instead of MDG `programArgument`).
  - Dependencies now `minecraft "net.minecraftforge:forge:${forge_version}"` and
    CurseMaven ids from `DEPS.md` wrapped in `fg.deobf(...)`.
  - Kept the quick-play argument switch, `runData` generated-resources dir,
    `runServer` EULA helper, and stdin forwarding.
  - `processResources` expands `META-INF/mods.toml` and `forge_version_range`.
- `gradle.properties`
  - `minecraft_version=1.20.1`, `forge_version=1.20.1-47.2.0`,
    `forge_version_range=[47,)`, `loader_version_range=[47,)`.
  - `exdeorum_version_range=[1.53,)` (Ex Deorum 1.20.1 line),
    `mekanism_version_range=[10.4.16,)`.
  - `mod_version` stays `0.2.3`.
- `settings.gradle`: plugin repository switched from NeoForged maven to
  `https://maven.minecraftforge.net/`.
- `META-INF/neoforge.mods.toml` → `META-INF/mods.toml` (git rename).
  - `neoforge` dependency → `forge`, `${neo_version_range}` → `${forge_version_range}`.
  - JEI optional range set to the verified 1.20.1 file line (`[15.74.0,)`).
  - Removed the `mekenergistics` optional dependency (and its comment).
  - `mekanism_empowered` range lowered to `[1.0,)` because the 1.20.1 line uses
    the `20.1-1.3.0` naming rather than the 1.21.1 `21.1` line.
  - The `[[mixins]]` block is removed in phase 4 together with the mixin sources.
- `.github/workflows/build.yml`: JDK 17, Gradle 8.8, push trigger for
  `backport/1.20.1`.
- `flake.nix`: description updated to Forge 1.20.1, dev-shell JDK 21 → 17.
- Removed a stale ignored `.gradle-user-home` directory that an earlier broken
  Gradle invocation had created inside `src/main/java`.

### Decisions / uncertainties recorded

- **GuideMe**: `BRIEF.md` says drop it (AE2 15 does not use it); `DEPS.md` says a
  20.1.x line exists and could be kept. Both documents conflict. I followed the
  brief and dropped it. It is dev-classpath only either way.
- **Mek Energistics**: out of scope by user decision; removed in phase 4.
- **Addon file ids**: used exactly the ids in `DEPS.md`, no substitutions.
- **Empowered / Extras item ids and tag names**: the addon-tier recipe JSON
  still contains the 1.21.1 data (`c:` tags and the same item paths). These
  could not be verified against the 1.20.1 addon jars in this VM; they affect
  optional addon tiers only and do not affect compilation. Flagged for a
  post-build data review.
