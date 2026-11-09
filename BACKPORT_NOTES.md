# Ex Mekanism Deorum — 1.20.1 backport notes

Branch: `backport/1.20.1` (pushed to origin 2026-11-10; never tagged).
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
  1.20.1 shape and not verified with `javap`. CI then verified it: build green
  on run 42644904854 after three fix rounds (see "CI fixes" below), so the
  `RecipeUtil` helper names, the barrel/fluid recipe record component names
  (`fluidAmount`, `baseFluidAmount`, `consumesAdditive`, `catalyst`,
  `byproducts`, `duration`, `result`, `resultFluid`), `getResultNbt()`,
  `HammerLootModifier.calculateFortuneBonus(...)`, and
  `SieveLogic.setMesh/saveNbt/loadNbt` all compile against the real 1.20.1
  dependencies.
- Addon-tier recipe item ids and tags (Evolved Mekanism / Extras / Empowered)
  are unchanged from the 1.21.1 data and were not verified against the 1.20.1
  addon jars; they only affect optional tiers and not compilation.
- GuideMe was dropped (BRIEF.md) despite DEPS.md listing a 20.1.x line; it is a
  dev-classpath-only dependency either way.
- Empowered 1.20.1 may declare an additional "MekMM" runtime dependency; it
  affects dev runs only, not compilation.
- All work is committed on `backport/1.20.1` only; the branch is pushed to
  origin (CI green, run 42644904854), nothing is tagged, and `main` was not
  touched.

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

- **Fix 3 — 6 `:compileJava` errors (CI run 42643136321).** Every API was verified
  against the real 1.20.1 sources before editing (Mojang official mappings for
  vanilla; upstream GitHub for the exact dependency versions) — no guessing.

  - `tile/TileEntityDeorumBarrel` (`tryInWorldFluidMixing`): in 1.20.1
    `BucketPickup.pickupBlock` is `pickupBlock(LevelAccessor, BlockPos, BlockState)`.
    The `Player` parameter was added in a later Minecraft version, so there is no
    player argument to pass. Dropped the `null`:
    `pickup.pickupBlock(level, abovePos, aboveState)`.
  - `tile/TileEntityDeorumHammer` fortune handling: the 1.20.1 dependency is
    Ex Deorum **1.53** (the jar id in DEPS.md) and its `EItemTags` defines **no**
    `HAMMER_FORTUNE_BLACKLIST` or `COMPRESSED_HAMMER_FORTUNE_BLACKLIST` — those
    tags are 1.21.1-only. Verified on the upstream `1.20.1` branch
    (`version = '1.53'`, i.e. exactly the dependency) and in
    `MechanicalHammerBlockEntity` / `HammerLootModifier`: 1.20.1 Ex Deorum applies
    fortune to **all** hammer outputs with no blacklist. The blacklist condition
    was therefore dropped cleanly; the remaining guard is `count > 0`, so fortune
    applies to every hammer output as it does upstream. No invented tag names.
  - `tile/TileEntityDeorumHammer.damageHammer`: 1.20.1 has
    `ItemStack.hurt(int, RandomSource, @Nullable ServerPlayer)` (returns whether the
    item broke) and the generic `hurtAndBreak(int, LivingEntity,
    Consumer<LivingEntity>)`; the 1.21.1 four-argument
    `hurtAndBreak(int, ServerLevel, LivingEntity, Consumer)` does not exist.
    Mirrored Ex Deorum 1.20.1's own `damageHammer`:
    `if (hammer.hurt(1, level.getRandom(), null)) { hammer.shrink(1); if
    (hammer.isEmpty()) hammerSlot.setStack(ItemStack.EMPTY); }`.
    Mekanism 10.4 `BasicInventorySlot.getStack()` returns the live stack and
    `setStack(...)` is the public setter, so the slot is cleared when the hammer
    breaks. The original on-break lambda was empty, so no extra break behaviour was
    lost; the hammer still takes exactly 1 damage per completed operation.
  - `client/GuiDeorumMachine`: in Mekanism 10.4 `GuiElement.mouseClicked(double,
    double, int)` is `final`. The overridable hook is `onClick(double, double,
    int button)`, invoked by `mouseClickedNested` when the element is active,
    visible and hovered. Reworked the anonymous `GuiProgress` override into
    `onClick(...)`, keeping the same left-click (`button == 0`) behaviour that opens
    the matching JEI recipe category. The click is still consumed by the element in
    10.4, so nothing falls through to the parent screen.
  - Static sweep: no other source or resource references the two removed fortune
    tags, `pickupBlock(null, ...)`, the 4-arg `hurtAndBreak`, or an overridden
    `mouseClicked` remain in `src/`.

## Runtime verification (2026-11-10, local runClient under Xvfb)

CI only proves the jar builds; the mod was also booted in a real client
(Forge 47.2.0 dev environment, core dependency set: Mekanism 10.4.16,
Ex Deorum 1.53, JEI). Result: the game reached the main-menu warning
screen and the mod initialised cleanly:

    Ex Mekanism Deorum loaded; registered 25 machine blocks.

### Bug found by the runtime check (fixed in this branch)

- `mods.toml` declared dependencies NeoForge-style (`type = "required"` /
  `type = "optional"`). Forge 1.20.1 FML requires the `mandatory` field and
  rejected the mod file outright (`InvalidModFileException: Missing
  required field mandatory in dependency`), which also poisoned the whole
  mod scan. Converted: `required` -> `mandatory = true`,
  `optional` -> `mandatory = false` (9 entries). The pre-fix CI jar would
  not have loaded on a real 1.20.1 instance.

### Dev-environment issues with the full addon set (not mod bugs)

- `mekanism_empowered`, `mekanism_empowered_core` and `emextras` require
  Forge `[47.4.0, ...)`; the project pins 47.2.0, so the full addon set
  cannot load in dev until the Forge version is bumped within 47.x.
- AE2 (9357405) declares GuideMe `[20.1.7, 20.2.0)` as a mandatory
  dependency; GuideMe is not in the dev dependency set. AE2's
  `GuiGraphicsMixin` also failed to apply under official mappings in dev.
- Optional-version warning only: `mekanism_empowered` expects
  `mekanism_extras [1.20.1-1.4.6,)`; the pinned 1.5.0 jar reports its
  version as plain `1.5.0`, which sorts below that range.
- Minor: `hammer_machine` blockstate lacks models for the
  `active=*, facing=north|south` variants (ModelBakery warnings;
  non-fatal).

## In-world operation check (2026-11-10, continued)

Booted the dev client (Xvfb), created a world and exercised a Hammer
Machine end to end: hopper-fed 64 cobblestone, supplied energy, and the
machine processed cobblestone -> gravel continuously (input 64 -> 48,
16 gravel produced, `active=true`, energy draining per operation).
Recipe lookup, lane progress, GUI and localisation all work.

### Bug found by the in-world check (fixed in this branch)

- All 90 blockstate files keyed variants as `facing=...,active=...`,
  but 1.20.1's state serialisation expects `active=...,facing=...`, so
  every variant missed its model and every machine/factory block
  rendered as the magenta/black missing-model cube. Reordered all 720
  variant keys; blocks now render with their real textures.
- Added the missing `pack.mcmeta` (pack_format 15). Without it the
  exploded dev resources fail to load as a resource pack (production
  jars are unaffected because FML synthesises pack metadata), which in
  dev made GUI textures and lang entries fail to load.

### Notes from the check

- Creative Energy Cubes placed with /setblock do not feed the machines
  in dev (no stored energy / no ejection); energy was injected via the
  tile's `EnergyContainers` NBT for the test instead. Normal play with
  charged cubes/cables is unaffected.
