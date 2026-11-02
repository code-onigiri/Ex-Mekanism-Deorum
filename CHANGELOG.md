# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Verified and documented the Mek Energistics ME upgrade path: every machine accepts the
  ME Pattern Provider, ME Passive Crafting and ME Output Interface upgrades and becomes an
  ME-network-connected pattern automation host once one is installed.

### Changed

- Simplified the development setup: `flake.nix` only provides JDK 21, Gradle and the client
  native libraries, and `build.gradle` resolves every mod from CurseForge Maven. The pinned
  jar machinery (`mods.nix`, `-PuseMavenDeps` and `-PnoAddons`) is gone;
  all optional mods load in development by default.

## [0.2.1] - 2026-11-03

### Added

- **Mekanism Extras upgrades**: all machines now accept `STACK` (doubles processing
  speed per level, energy cost scales with it) and `CREATIVE` (zero energy usage).

### Fixed

- Extras upgrade items could not be inserted into the machines' upgrade slots because
  the custom block types did not declare them as supported.
- **Mek Energistics integration**: machines were registered with the pattern-automation SPI
  using block instances before they were bound, which crashed mod construction. Registration now
  uses plain block ids, and the compatibility mixin is actually applied (its config is declared
  in `neoforge.mods.toml`, and the plugin probes a non-mixin class so Mek Energistics' own
  interface mixin is not forced to load too early).

## [0.2.0] - 2026-11-02

### Added

- **Mekanism: Empowered integration**: all machines now accept `empowered_speed`,
  `empowered_energy`, `io_capacity`, `fast_item_insert`, `fast_item_eject` and
  `auto_inserter`.
- **Mekanism Extras / Evolved Mekanism Extras integration**: additional tiers
  (Absolute–Infinite, Absolute Overclocked–Infinite Multiversal) and their tier installers.
- **Tier installers**: factories can be upgraded Basic → Advanced → Elite → Ultimate → the
  add-on tiers. Inventory and progress are preserved across the upgrade.
- **Add-on-equivalent recipes**: additional-tier factories use the original
  `mekanism:mek_data` recipes from each add-on (alloys, circuits, ingots/pellets).
- **Compressed sieving**: when the input matches a `compressed_sieve` recipe, the sieve
  automatically uses compressed recipes with just a mesh.
- **Compressed hammer output**: results are emitted as `compressed_*` items
  (nine items consolidated into one; falls back to normal items when no counterpart exists).
- **Texture animation**: the front LED breathes while idle and pulses with a moving scan
  line while active.
- **ME pattern automation**: all machines are registered with the Mek Energistics SPI
  (item/fluid I/O, persistent hammer/mesh inputs, factory parallelism).
- **JEI**: compressed-sieve catalysts and recipe lookup by clicking the progress bar.
- **Larger input capacity**: input slots hold up to 64 × parallel lanes.
- **Larger energy buffer**: internal buffer scales as 40,000 FE × parallel lanes.

### Changed

- **Increased parallelism**: Basic 4 / Advanced 8 / Elite 12 / Ultimate 16.
  Add-on tiers: Evolved 20–36, Extras 20–32, EM Extras 40–64 (sum of the two tiers).
- Top texture rotated 180° to match Mekanism's `orientable_with_bottom` orientation.
- Base machine and factory recipes unified to `ACA / IPI / ACA` (crucibles use a bucket
  as the side ingredient).
- Development runs include all add-on mods on the classpath by default (`-PnoAddons` opts out).
- Dependencies resolve from CurseForge Maven by default (env vars, `-PuseMavenDeps` or
  `libs/` can override).

### Fixed

- Input items were consumed the moment they were inserted; they are now consumed when an
  operation completes.
- External energy and cables did not connect because side configs defaulted to `NONE`
  (fixed with the `SIDE_CONFIG` component and an `onLoad` migration for existing worlds).
- The progress bar appeared diluted by the lane count when processing a single item;
  it now averages over active lanes only.
- High-speed upgrades could stop machines when the per-tick cost exceeded the buffer;
  the cost is now clamped to the buffer capacity.
- Combined vanilla and Empowered speed upgrades now compute and register correctly.
- Barrels could not compost while their tank contained fluid.
- Water crucible fluid conversion and tank output.

## [0.1.0] - 2026-11-02

Initial release.

- 5 base machines and Basic / Advanced / Elite / Ultimate factories for all five families.
- Automatic use of Ex Deorum hammer, sieve, barrel, lava crucible and water crucible recipes.
- FE, upgrades, side configuration, security and JEI catalysts from Mekanism.
- Placeholder textures, GUI and mod icon.
