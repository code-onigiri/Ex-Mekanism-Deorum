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

## Phase 2 — Forge loader packages and entry points

- All `net.neoforged.*` imports replaced with the Forge 1.20.1 equivalents:
  - `net.neoforged.bus.api.IEventBus` / `SubscribeEvent` ->
    `net.minecraftforge.eventbus.api.*`
  - `net.neoforged.fml.common.Mod` / `EventBusSubscriber` -> `net.minecraftforge.fml.common.*`
  - `net.neoforged.fml.ModList` -> `net.minecraftforge.fml.ModList`
  - `net.neoforged.api.distmarker.Dist` -> `net.minecraftforge.api.distmarker.Dist`
- `ExMekanismDeorum`: `@Mod` constructor no longer takes
  `(IEventBus, ModContainer)`; it takes no arguments and obtains the bus from
  `FMLJavaModLoadingContext.get().getModEventBus()`.
- `ClientSetup`: `RegisterMenuScreensEvent` replaced by `FMLClientSetupEvent`
  (`event.enqueueWork(...)`) calling
  `ClientRegistrationUtil.registerScreen(container, GuiDeorumMachine::new)`
  (Mekanism 10.4 signature). `@EventBusSubscriber` -> `@Mod.EventBusSubscriber`
  with `Mod.EventBusSubscriber.Bus.MOD`.
- Javadoc path `META-INF/neoforge.mods.toml` -> `META-INF/mods.toml`.

## Phase 3 — Mekanism 10.4 tile and registration rework

- `ModMachines`:
  - `Machine<T>` -> `BlockTypeTile<T>`, adding the attributes Mekanism's own
    `Machine` type adds (`AttributeParticleFX`, `ACTIVE_LIGHT`,
    `AttributeStateFacing`, `INVENTORY`, `SECURITY`, `REDSTONE`, `COMPARATOR`,
    `AttributeUpgradeSupport`).
  - `AttributeEnergy` now takes `Supplier<FloatingLong>`; energy/handling
    arithmetic converted from `long` to `FloatingLong`
    (`multiply`, `min`, `greaterThan`, `.equals`).
  - Removed item data components: no `MekanismDataComponents`,
    `AttachedSideConfig`, `AttachedEjector` or `.component(...)` calls. Blocks
    use `BLOCKS.registerDefaultProperties(...)` with a plain `ItemBlockTooltip`.
    Side config / ejector now live in the tile components and persist via tile
    NBT (same as Mekanism 10.4 factories).
  - Tile types registered with
    `TILE_TYPES.register(block, factory, TileEntityMekanism::tickServer,
    TileEntityMekanism::tickClient)` instead of the 1.21
    `mekBuilder(...).clientTicker(...).serverTicker(...).build()`.
  - Creative tab type is `CreativeTabRegistryObject`; `registerMain` takes the
    block registry object directly.
- `TileEntityDeorumMachine`:
  - Constructor takes `IBlockProvider` instead of `Holder<Block>`;
    `getBlockHolder()` -> `getBlockType()` for attribute lookup.
  - `TileComponentConfig` is created in `presetVariables()` (10.4 builds it in
    the tile); default side configuration moved into the tile and the 1.21
    item-component migration (`onLoad`/`applyDefaultSideConfig`) removed.
  - `EnergyContainerHelper` / `InventorySlotHelper` use
    `forSideWithConfig(this::getDirection, this::getConfig)`.
  - `onUpdateServer()` is `void` in 1.20.1; it calls `sendUpdatePacket()`.
  - NBT: `saveAdditional(CompoundTag)` / `load(CompoundTag)` (no
    `HolderLookup.Provider`); `ItemStack.save(new CompoundTag())` and
    `ItemStack.of(...)`; `isSameItemSameComponents` -> `isSameItemSameTags`.
  - Enchantment level via `EnchantmentHelper.getItemEnchantmentLevel(...)`.
- New `inventory/DeorumAuxSlot`: a one-item auxiliary slot used for the hammer
  and sieve-mesh slots (replaces the 1.21 `BasicInventorySlot.at(..., 1)`).
- `GuiDeorumMachine`: `ResourceLocation.fromNamespaceAndPath` ->
  `new ResourceLocation`, `List.getFirst()` -> `get(0)`, and a local
  `renderInventoryText` helper for the 1.20.1 GUI base class.

## Phase 4 — Ex Deorum 1.20.1 API, JEI, data formats, Mek Energistics removal

- Ex Deorum API (1.20.1 line, v1.53) adaptation:
  - `RecipeCaches` (1.21 object obtained via `RecipeUtil.getCaches`) does not
    exist in 1.20.1. All lookups now call the static
    `thedarkcolour.exdeorum.recipe.RecipeUtil` helpers:
    `getBarrelMixingRecipe`, `getBarrelCompostRecipe`, `getFluidMixingRecipe`,
    `getFluidTransformationRecipe`, `getLavaCrucibleRecipe`,
    `getWaterCrucibleRecipe`, `getHammerRecipe`,
    `getCompressedHammerRecipe`, `getHeatValue`.
  - `getRecipeCaches()` removed from the tile base and the sieve override.
  - Recipe record accessors switched to the 1.20.1 field names:
    `fluidAmount`, `baseFluidAmount`, `consumesAdditive`, `catalyst`,
    `byproducts`, `duration`, `result`, `resultFluid`; results are rebuilt as
    `new ItemStack(recipe.result)` + `recipe.getResultNbt()`.
  - `CompressedHammerRecipe` is not a separate JEI recipe type in 1.20.1, so
    `COMPRESSED_HAMMER` is a `RecipeType<HammerRecipe>`.
- JEI (15.x, 1.20.1):
  - Category namespace is always `exdeorum`; the 1.21 EMI remap to
    `exdeorum_emi` was removed (no EMI dependency in 1.20.1).
  - Catalyst registration uses 10.4 `BlockRegistryObject` accessors
    (`block.asItem()`, `block.getBlock()`); `getPluginUid` uses
    `new ResourceLocation(...)`.
- Hammer:
  - `HammerLootModifier.calculateFortuneBonus(hammer, random, boolean)` (the
    1.21 `registryAccess` argument is gone).
  - Result stacks built with a `hammerResult(recipe, count)` helper that applies
    the recipe's result NBT.
- Sieve: `SieveLogic.setMesh(mesh)`, `saveNbt(tag)`, `loadNbt(tag)` (no
  `HolderLookup.Provider`).
- Data JSON (`src/main/resources/data/exmekdeorum/`):
  - `recipe/` -> `recipes/`, `loot_table/` -> `loot_tables/` (90 + 90 files).
  - Crafting results `"result": { "id": ... }` -> `"result": { "item": ... }`.
  - Conditions `"neoforge:conditions"` -> `"conditions"` and
    `"type": "neoforge:mod_loaded"` -> `"type": "forge:mod_loaded"`.
- Mek Energistics removed completely:
  - Deleted `compat/MekEnergisticsCompat.java`,
    `mixin/DeorumMixinPlugin.java`,
    `mixin/TileEntityDeorumMachineMekEnergisticsMixin.java` and
    `exmekdeorum.mixins.json`.
  - Removed the reflective `ModList.isLoaded("mekenergistics")` hook in the main
    class (and the now-unused `ModList` import).
  - No `mekenergistics` dependency in `mods.toml` (removed in phase 1); no
    `[[mixins]]` block.
  - README / CURSEFORGE / DEVELOPMENT docs no longer advertise the integration.

## Phase 5 — final static sweep

Ran from the repo root on 2026-11-09 after phase 4 was committed. Results:

| Pattern | Source/config hits | Notes |
| --- | ---: | --- |
| `neoforged` | 0 | only this notes file mentions the old ModDevGradle plugin |
| `neoforge:` | 0 | conditions are `forge:mod_loaded` |
| `HolderLookup` | 0 | NBT signatures no longer carry a provider |
| `fromNamespaceAndPath` | 0 | all use `new ResourceLocation(...)` |
| `MekanismDataComponents` | 0 | side config/ejector moved to tile NBT |
| `mekenergistics` | 0 | integration and dependency removed (notes only) |
| `data/exmekdeorum/recipe/` | 0 | recipes live under `recipes/` |

Additional checks: no active references to `RecipeCaches`, `AttachedSideConfig`,
`AttachedEjector`, `isSameItemSameComponents`, `exdeorum_emi` (outside the
explanatory JEI Javadoc that documents the removal), or `data/exmekdeorum/loot_table/`.
`neoforge.mods.toml` remains only in `CHANGELOG.md` (historical release notes)
and in this notes file.

## Outstanding uncertainties (static-only verification)

- **Gradle could not be run in this VM**, and no dependency jars were present in
  a resolvable cache, so the Ex Deorum 1.20.1 API was adapted from the documented
  1.20.1 shape and not verified with `javap`. If CI fails, check first:
  `RecipeUtil` helper names, the barrel/fluid recipe record component names
  (`fluidAmount`, `baseFluidAmount`, `consumesAdditive`, `catalyst`,
  `byproducts`, `duration`, `result`, `resultFluid`), `getResultNbt()`,
  `HammerLootModifier.calculateFortuneBonus(...)`, and
  `SieveLogic.setMesh/saveNbt/loadNbt`.
- Addon-tier recipe item ids and tags (Evolved Mekanism / Extras / Empowered)
  are unchanged from the 1.21.1 data and were not verified against the 1.20.1
  addon jars; they only affect optional tiers and not compilation.
- GuideMe was dropped (BRIEF.md) despite DEPS.md listing a 20.1.x line; it is a
  dev-classpath-only dependency either way.
- Empowered 1.20.1 may declare an additional "MekMM" runtime dependency; it
  affects dev runs only, not compilation.
- All work is committed on `backport/1.20.1` only; nothing was pushed or tagged
  and `main` was not touched.

## CI fixes (after first push)

- **Fix 1 — `runData` task not found (CI run 42642044045):** ForgeGradle 6
  registers the run tasks in its own `afterEvaluate`, so `tasks.named('runData')`
  / `tasks.named('runServer')` at script-evaluation time failed with
  "Task with name 'runData' not found". Wrapped both blocks in `afterEvaluate`
  in `build.gradle`. (Review fix by Shion; no code-agent run needed.)

- **Fix 2 — `runData` still not found in `afterEvaluate` (CI run 42642552328):**
  ForgeGradle 6 creates the run tasks lazily, after project evaluation, so
  `afterEvaluate` was still too early. Replaced with `tasks.whenTaskAdded`,
  which hooks each run task whenever it appears. (Review fix by Shion.)
