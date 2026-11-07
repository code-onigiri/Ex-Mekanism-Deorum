# Ex Mekanism Deorum

**Ex Deorum machines, rebuilt as Mekanism machines.**

Automate the Hammer, Sieve, Barrel, Lava Crucible and Water Crucible with FE power,
Mekanism upgrades, side configuration and security — using Ex Deorum's own recipes,
including recipes added by datapacks.

![GUI Preview](https://raw.githubusercontent.com/code-onigiri/Ex-Mekanism-Deorum/main/docs/gui-preview.png)

## Overview

Ex Deorum's manual and mechanical machines are re-implemented on Mekanism's machine
architecture. Recipes are read directly from Ex Deorum's recipe cache at runtime, so
**custom recipes added via datapacks work out of the box** — this mod ships no recipes
of its own for processing.

- **Up to 90 blocks** — 5 base machines + 85 factories. Without any optional addon you
  get the base 25 blocks (5 machines + Basic / Advanced / Elite / Ultimate factories);
  each addon below adds its own tiers on top.
- **Full Mekanism integration** — Speed / Energy / Muffling upgrades, side configuration,
  security, redstone control and JEI catalysts.
- **Factory parallelism** — Basic 4 / Advanced 8 / Elite 12 / Ultimate 16 parallel lanes.
  Addon tiers reach 20–64 lanes. Crucible factories do not add lanes; they multiply
  melting speed instead (5× / 10× / 20× / 30× for the base tiers, up to 120×).
- **Scaling buffers** — Input slots hold 64 × lanes and the internal FE buffer holds
  40,000 FE × lanes, so a factory can run for the same length of time as a base machine.
- **Compressed Hammer support** — Insert a compressed hammer to process
  `compressed_hammer` recipes; results are emitted as `compressed_*` items.
- **Animated fronts** — The front LED breathes while idle and pulses with a scan line
  while the machine is working.

## Requirements

| Requirement | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 or higher |
| Ex Deorum | 3.12 or higher (required) |
| Mekanism | 10.7.19 or higher (required) |
| JEI | Optional |

Install on both client and server. No configuration file is needed.

## Installation

1. Set up Minecraft 1.21.1 with NeoForge 21.1.265 or higher.
2. Put **Ex Deorum** (3.12+) and **Mekanism** (10.7.19+) in your `mods/` folder.
3. Put this mod's jar in your `mods/` folder.

## Machines & Factories

| Family | Base machine | Factories | Base consumption |
| --- | --- | --- | --- |
| Hammer | Hammer Machine | `<tier>_hammer_factory` | 20 FE/t per lane |
| Sieve | Sieve Machine | `<tier>_sieve_factory` | 40 FE/t per lane |
| Barrel | Barrel Machine | `<tier>_barrel_factory` | 10 FE/t per lane |
| Lava Crucible | Crucible Machine | `<tier>_crucible_factory` | 20 FE/t × melt multiplier |
| Water Crucible | Water Crucible Machine | `<tier>_water_crucible_factory` | 20 FE/t × melt multiplier |

### Tiers

| Tier | From | Parallel lanes | Crucible multiplier |
| --- | --- | ---: | ---: |
| Basic | Mekanism | 4 | 5× |
| Advanced | Mekanism | 8 | 10× |
| Elite | Mekanism | 12 | 20× |
| Ultimate | Mekanism | 16 | 30× |
| Overclocked / Quantum / Dense / Multiversal / Creative | Evolved Mekanism | 20 / 24 / 28 / 32 / 36 | 40× / 50× / 60× / 70× / 80× |
| Absolute / Supreme / Cosmic / Infinite | Mekanism Extras | 20 / 24 / 28 / 32 | 40× / 50× / 60× / 70× |
| Absolute Overclocked / Supreme Quantum / Cosmic Dense / Infinite Multiversal | Evolved Mekanism Extras | 40 / 48 / 56 / 64 | 90× / 100× / 110× / 120× |

Addon-tier blocks, recipes and JEI catalysts are **only registered when the matching
addon is installed**. The addons' own tier installers can upgrade these factories
(Basic → Advanced → Elite → Ultimate → addon tiers).

## Machine Details

### Hammer & Sieve

- **Hammer** — 200 ticks per operation by default. Needs a hammer in the auxiliary slot
  (slot 22); 1 durability is consumed per operation and Efficiency / Fortune apply.
  Output goes to 20 output slots. Inserting a compressed hammer
  (`#exdeorum:compressed_hammers`) switches compressed blocks to `compressed_hammer`
  recipes, and the results keep their compressed form (nine drops consolidated into
  one `compressed_*` item).
- **Sieve** — 100 ticks per operation by default. Needs a mesh in the auxiliary slot
  (slot 22); mesh Efficiency / Fortune apply. Compressed blocks are detected
  automatically and processed with the same mesh via `compressed_sieve` recipes.
  Recipes marked `by_hand_only: true` are skipped by the machine.
- **Item consumption** — Input items stay in their slot while processing and are
  consumed (by 1) only when an operation completes.

### Barrel

- 2 item input slots, 1 item output slot, and fluid input / output tanks of
  1,000 mB × lanes each.
- Supports composting, item + fluid mixing, in-world mixing (fluid block directly
  above), catalyst transformation (3×3 blocks below) and rainwater filling.
- **Composting** — Inserted items immediately add to the compost value; the progress
  bar shows the fill level first, then switches to the dirt-creation phase.
- External fluid filling is only accepted while the barrel is idle
  (compost value 0 and both item inputs empty).

### Crucibles

- **Lava Crucible** — Melts one item at a time. Melting speed depends on the heat value
  of the block directly underneath (`crucible_heat_source`); with no heat source,
  nothing is consumed — neither items nor energy. Tank: 4,000 mB × melt multiplier,
  output only (extract with pipes; external filling is not accepted).
- **Water Crucible** — Dedicated to `water_crucible` recipes. No heat source needed;
  fixed base rate of 4 mB per 10 ticks, scaled by the factory multiplier
  (energy per mB stays the same).

![Block Fronts](https://raw.githubusercontent.com/code-onigiri/Ex-Mekanism-Deorum/main/docs/block-fronts.png)

## Crafting Recipes

Base machines all use the pattern `ACA / IPI / ACA`
(A = Redstone, C = `mekanism:basic_control_circuit`, P = the Ex Deorum part below,
I = the side ingredient below).

| Machine | Center part `P` | Side ingredient `I` |
| --- | --- | --- |
| Hammer Machine | `exdeorum:mechanical_hammer` | Iron Ingot |
| Sieve Machine | `exdeorum:mechanical_sieve` | Iron Ingot |
| Barrel Machine | `exdeorum:oak_barrel` | Iron Ingot |
| Lava Crucible | `exdeorum:porcelain_crucible` | Lava Bucket |
| Water Crucible | `exdeorum:porcelain_crucible` | Water Bucket |

Factories follow Mekanism's tier progression: craft the next tier around the
previous-tier block, using that tier's alloy, control circuit and ingot.
Addon tiers mirror the corresponding addon's own factory recipes and are only
loaded when that addon is present.

## Optional Integrations

All of the following are optional; the mod works without them.

- **Evolved Mekanism / Mekanism Extras / Evolved Mekanism Extras** — Extra factory
  tiers (see the tier table above), upgradeable with each addon's tier installers.
- **Mekanism: Empowered** — All machines accept its extra upgrades
  (`empowered_speed`, `empowered_energy`, `io_capacity`, `fast_item_insert`,
  `fast_item_eject`, `auto_inserter`).
- **Mekanism Extras** — All machines accept the `STACK` upgrade (processing speed
  ×2 per level, energy cost scales with it) and the `CREATIVE` upgrade
  (zero energy usage).
- **Mek Energistics** — Machines are registered with the AE2 pattern automation SPI
  (item and fluid I/O, persistent hammer / mesh inputs, factory parallelism).
  The **ME Pattern Provider / ME Passive Crafting / ME Output Interface upgrades can
  be installed in every machine**, turning it into an ME-network-connected pattern
  automation host. Verified with Mek Energistics 3.0.8 + AE2 19.2.
- **JEI** — No custom categories: Ex Deorum's categories list these machines as
  catalysts, and clicking a machine's progress bar opens the matching JEI recipe.

## Known Limitations

- Textures and GUIs are currently placeholder assets.
- The Compressed Hammer is for the Hammer machines only; sieves handle both normal
  and compressed blocks with a mesh alone.
- Barrels being destroyed by hot fluids is not implemented.

## Credits & License

- Author: codeonigiri
- Mod license: GNU General Public License v3.0 ([LICENSE](https://github.com/code-onigiri/Ex-Mekanism-Deorum/blob/main/LICENSE))
- Base textures and GUI base: Mekanism (MIT License, Copyright Aidancbrady and contributors)
- Recipe caches, tags and machine parts: [Ex Deorum](https://github.com/thedarkcolour/ExDeorum) by thedarkcolour

## Links

- GitHub: <https://github.com/code-onigiri/Ex-Mekanism-Deorum>
