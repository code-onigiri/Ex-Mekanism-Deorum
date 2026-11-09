<p align="center"><img src="docs/icon.png" width="96" height="96" alt="Ex Mekanism Deorum"></p>

# Ex Mekanism Deorum

**English** | [日本語](README.ja.md)

**A Forge mod that adds Ex Deorum's machines as Mekanism machines.**

Hammers / sieves / barrels / crucibles / water crucibles, automated with the
same feel as Mekanism (FE power, upgrades, side configuration, security).
Recipes are read directly from Ex Deorum's recipe cache, so **recipes added
by data packs work as-is**.

| Item | Details |
| --- | --- |
| Minecraft / Forge | 1.20.1 / 47.2.0 or later |
| Required dependencies | Ex Deorum 1.53 or later / Mekanism 10.4.16 or later |
| This mod's version | 0.2.1 (textures and GUI are placeholder assets) |
| Install on | Both client and server (no config) |
| Block count | 25 base / up to 90 with all compatible mods installed (see below) |

![GUI preview](docs/gui-preview.png)

## Contents

- [Features](#features)
- [Installation](#installation)
- [Machines and tiers](#machines-and-tiers)
- [Usage](#usage)
- [Crafting recipes](#crafting-recipes)
- [Compatible mods (all optional)](#compatible-mods-all-optional)
- [JEI integration](#jei-integration)
- [Screenshots](#screenshots)
- [Known limitations](#known-limitations)
- [Customization / development](#customization--development)
- [License / credits](#license--credits)

## Features

- **Up to 90 blocks** — 5 base machines + 85 factories.
  Breakdown: Mekanism's 4 tiers + Evolved Mekanism's 5 tiers +
  Mekanism Extras' 4 tiers + Evolved Mekanism Extras' 4 tiers (5 families each).
  Without the compatible mods installed, only the base 25 blocks are registered.
- **Automatic recipe lookup** — uses Ex Deorum's hammer, sieve, barrel, and
  crucible recipes as-is (including ones added by data packs).
- **Mekanism integration** — runs on FE. Supports speed / energy / muffling
  upgrades, side configuration, security, redstone control, and JEI catalysts.
- **Factory parallel processing** — Basic 4 / Advanced 8 / Elite 12 / Ultimate 16 lanes.
  With compatible mods installed, additional tiers of 20–64 lanes are available
  (crucibles use a speed multiplier instead of lanes).
- **Compressed hammer support** — inserting a compressed hammer processes
  `compressed_hammer` recipes, and outputs `compressed_*` items.
- **Large buffers** — input slots hold 64 × lane count, and the internal FE buffer
  holds 40,000 FE × lane count (crucible factories: × crucible multiplier).
- **Animations** — the front LED breathes while idle, and blinks with a scanline
  while running.

![Block fronts (top to bottom: base / Basic / Advanced / Elite / Ultimate; left to right: hammer, sieve, barrel, lava crucible, water crucible)](docs/block-fronts.png)

## Installation

1. Prepare a Minecraft 1.20.1 + Forge 47.2.0 or later environment
2. Install [Ex Deorum](https://modrinth.com/mod/ex-deorum) 1.53 or later
3. Install [Mekanism](https://modrinth.com/mod/mekanism) 10.4.16 or later
4. Put this mod's jar in `mods/`

Install it on both client and server. There is no config.

## Machines and tiers

### Machine families

| Family | Base machine | Factory name pattern | Consumption per lane |
| --- | --- | --- | --- |
| Hammer | `hammer_machine` | `<tier>_hammer_factory` | 20 FE/t |
| Sieve | `sieve_machine` | `<tier>_sieve_factory` | 40 FE/t |
| Barrel | `barrel_machine` | `<tier>_barrel_factory` | 10 FE/t |
| Lava crucible | `crucible_machine` | `<tier>_crucible_factory` | 20 FE/t × melt multiplier |
| Water crucible | `water_crucible_machine` | `<tier>_water_crucible_factory` | 20 FE/t × melt multiplier |

- Hammer / sieve / barrel factories process as many lanes in parallel as their
  lane count. There is no per-tier speed bonus, and FE per lane is the same as
  the base machine.
- **Crucible factories are the exception**: instead of adding lanes, they raise
  the melting speed. Power rises by the same multiplier, so FE per unit of
  liquid is the same as the base machine.
- Side configuration right after placement matches Mekanism machines
  (items: input on all sides + output on the right + energy on the back;
  energy: input on all sides; barrel / crucible fluids: input and output on
  all sides). Cables and pipes connect as placed.
- All registered blocks appear in the `Ex Mekanism Deorum` creative tab.

### Tier list (lane count, crucible multiplier, energy capacity)

The internal FE buffer is **40,000 FE × lane count** — except for crucible
factories, whose per-tick consumption scales with the crucible multiplier
instead of the lane count, so their buffer is **40,000 FE × crucible
multiplier** (the FE capacity column below shows the crucible value).
Either way the buffer scales with the per-tick consumption, keeping "how
long the buffer lasts" equal to the base machine.
The input slot limit is also **64 × lane count** (e.g. Basic 256, Ultimate 1,024).

| Tier | From | Lanes | Crucible multiplier | FE capacity |
| --- | --- | ---: | ---: | ---: |
| Base | — | 1 | 1× | 40,000 FE |
| Basic | Mekanism | 4 | 5× | 200,000 FE |
| Advanced | Mekanism | 8 | 10× | 400,000 FE |
| Elite | Mekanism | 12 | 20× | 800,000 FE |
| Ultimate | Mekanism | 16 | 30× | 1,200,000 FE |
| Overclocked | Evolved Mekanism | 20 | 40× | 1,600,000 FE |
| Quantum | Evolved Mekanism | 24 | 50× | 2,000,000 FE |
| Dense | Evolved Mekanism | 28 | 60× | 2,400,000 FE |
| Multiversal | Evolved Mekanism | 32 | 70× | 2,800,000 FE |
| Creative | Evolved Mekanism | 36 | 80× | 3,200,000 FE |
| Absolute | Mekanism Extras | 20 | 40× | 1,600,000 FE |
| Supreme | Mekanism Extras | 24 | 50× | 2,000,000 FE |
| Cosmic | Mekanism Extras | 28 | 60× | 2,400,000 FE |
| Infinite | Mekanism Extras | 32 | 70× | 2,800,000 FE |
| Absolute Overclocked | Evolved Mekanism Extras | 40 | 90× | 3,600,000 FE |
| Supreme Quantum | Evolved Mekanism Extras | 48 | 100× | 4,000,000 FE |
| Cosmic Dense | Evolved Mekanism Extras | 56 | 110× | 4,400,000 FE |
| Infinite Multiversal | Evolved Mekanism Extras | 64 | 120× | 4,800,000 FE |

If speed upgrades push per-tick consumption above the capacity, the machine
keeps running capped at the capacity (it never fully stops).

### About the additional tiers

- Additional-tier blocks are **only registered when the matching mod is
  installed** (without the mod, only Mekanism's 25 blocks). Recipes also carry
  a `forge:mod_loaded` condition. Without the additional mods, the
  additional-tier blocks, recipes, and JEI catalysts are not shown.
- Additional-tier recipes use the same material layout (`mekanism:mek_data`)
  as each compatible mod's own factories. `P` is the block one tier below
  (for Evolved Mekanism Extras, one tier below on the Extras / Evolved side
  respectively).
  - **Evolved Mekanism**: alloys (hypercharged → subatomic → singular →
    exoversal → creative) + circuits (overclocked → quantum → dense →
    multiversal → creative) + uranium / tin / bronze / netherite / nether star
  - **Mekanism Extras**: alloys (radiance → thermonuclear → shining →
    spectrum) + circuits (absolute → supreme → cosmic → infinite) +
    emerald / netherite / refined obsidian / plutonium + polonium pellets
  - **Evolved Mekanism Extras**: alloys from both mods + the `ACT / PXQ / TCA`
    pattern (X is `mekanism:steel_casing`; P / Q are the tier below on the
    Extras / Evolved side)
- With an additional mod installed, that mod's tier installer upgrades this
  mod's factories from Basic → … → Ultimate → that mod's additional tiers.
  When Evolved Mekanism and Mekanism Extras are both installed, the installer
  path uses the Evolved Mekanism side (Overclocked onwards).
- The 4 Evolved Mekanism Extras tiers correspond to that mod's installer
  (Absolute Overclocked → Supreme Quantum → Cosmic Dense → Infinite
  Multiversal). These 4 tiers can also be crafted directly (the installer is
  for upgrading within the tier).

## Usage

### Where recipes come from

Machines hold no recipes of their own; at runtime they look recipes up through
Ex Deorum's helper (`thedarkcolour.exdeorum.recipe.RecipeUtil`) directly.

| Machine | Ex Deorum recipes referenced |
| --- | --- |
| Hammer | `exdeorum:hammer` (`exdeorum:compressed_hammer` when a compressed hammer is used) |
| Sieve | `exdeorum:sieve` (`exdeorum:compressed_sieve` for compressed blocks) |
| Barrel | `exdeorum:barrel_compost` / `barrel_mixing` / `barrel_fluid_mixing` / `barrel_fluid_transformation` |
| Lava crucible | `exdeorum:lava_crucible` / `exdeorum:crucible_heat_source` |
| Water crucible | `exdeorum:water_crucible` |

### Hammer / sieve

- **Hammer**: 200 ticks per item by default. The hammer in the auxiliary slot
  (slot 22) loses 1 durability per operation, and Efficiency and Fortune apply.
  There are 20 output slots. Putting a compressed hammer
  (`#exdeorum:compressed_hammers`) in slot 22 processes compressed blocks with
  `compressed_hammer` recipes. Insertion order of input and hammer does not
  matter. Compressed hammer results keep their compression stage, and 9 items'
  worth of drops are combined into one `exdeorum:compressed_*` item.
- **Sieve**: 100 ticks per item by default. A mesh in the mesh slot (slot 22) is
  required, and the mesh's Efficiency / Fortune enchantments apply. Compressed
  blocks are detected automatically and processed with `compressed_sieve`
  recipes using the same mesh. Recipes with `by_hand_only: true` are excluded
  from the machine's roll.
- **Input handling (shared)**: items being processed stay in the input slot
  and only 1 is consumed when an operation completes (they are not consumed
  the moment they are inserted).

### Barrel

- 2 item input slots (either works for the same process), 1 item output slot,
  and one fluid input tank and one fluid output tank of 1,000 mB × lane count
  each.
- Supported processes: composting, item + fluid mixing, mixing with the fluid
  directly above (in-world mixing), fluid transformation via the 3×3 catalyst
  below, and collecting water while raining.
- **Composting**: inserted items immediately accumulate as a `compost` value,
  and the progress bar shows the "accumulated fraction". When full, the bar
  switches to the dirt-making step, and dirt is output on completion.
- **Item + fluid mixing** (e.g. water + snowball → ice): the finished product
  goes into the item output slot.
- Fluid injected from outside goes into the input tank, and transformation
  results go into the output tank; pipes can move fluids in and out.
  Transformation only progresses while the input tank is full, and on
  completion the input tank empties and the result fluid enters the output tank.

### Crucibles

- **Lava crucible**: melts **one item at a time**. The item in the input slot
  stays there while processing, and when the progress bar fills from 0 to 1,
  the recipe's fluid (e.g. 250 mB from cobblestone) enters the tank and 1 item
  is consumed. Speed is set by the heat value of the block directly below
  (`crucible_heat_source`); without a heat source the bar does not advance and
  no items or energy are consumed. The tank holds 4,000 mB × melt multiplier,
  and nothing can be injected from outside (output only; extract with pipes).
- **Water crucible**: for `exdeorum:water_crucible` recipes only. No heat source
  is needed; the base rate is fixed at 4 (4 mB per 10 ticks). In a factory the
  speed rises by the tier multiplier, and power rises by the same multiplier,
  so "power per 1 mB of liquid" stays unchanged.

## Crafting recipes

### Base machines

All patterns are `ACA / IPI / ACA`. The surroundings are redstone (A) and
`mekanism:basic_control_circuit` (C), the center (P) is an Ex Deorum machine
part, and the sides (I) are the materials in the table below.

| Machine | Center (P) | Sides (I) |
| --- | --- | --- |
| Hammer machine | `exdeorum:mechanical_hammer` | `minecraft:iron_ingot` |
| Sieve machine | `exdeorum:mechanical_sieve` | `minecraft:iron_ingot` |
| Barrel machine | `exdeorum:oak_barrel` | `minecraft:iron_ingot` |
| Lava crucible | `exdeorum:porcelain_crucible` | `minecraft:lava_bucket` |
| Water crucible | `exdeorum:porcelain_crucible` | `minecraft:water_bucket` |

### Factories

Like Mekanism factories, upgrade by placing the tier below in the center.

| Tier | Alloy (A) | Circuit (C) | Ingot (I) |
| --- | --- | --- | --- |
| Basic | `minecraft:redstone` | `mekanism:basic_control_circuit` | `minecraft:iron_ingot` |
| Advanced | `mekanism:alloy_infused` | `mekanism:advanced_control_circuit` | `mekanism:ingot_osmium` |
| Elite | `mekanism:alloy_reinforced` | `mekanism:elite_control_circuit` | `minecraft:gold_ingot` |
| Ultimate | `mekanism:alloy_atomic` | `mekanism:ultimate_control_circuit` | `minecraft:diamond` |
| Additional tiers | Each mod's alloy | Each mod's circuit | Each mod's ingot / pellet |

Additional tiers reproduce their compatible mod's own recipes
(only Evolved Mekanism Extras uses a full 9-slot `ACT / PXQ / TCA` recipe).
Additional-tier recipes are only loaded when the matching mod is present.

## Compatible mods (all optional)

This mod works with only its required dependencies (Ex Deorum / Mekanism).
The integrations below only become active when the listed mod is installed.

| Mod | Integration |
| --- | --- |
| Evolved Mekanism / Mekanism Extras / Evolved Mekanism Extras | Each mod's tier installer can upgrade this mod's factories from Basic → Advanced → Elite → Ultimate → that mod's additional tiers (additional-tier blocks are only registered when the matching mod is present). |
| Mekanism: Empowered | Accepts the additional upgrades below. Speed / energy multiplier upgrades are calculated via Mekanism's `MekanismUtils`, so they are reflected automatically. |
| Mekanism Extras | Accepts the additional upgrades `STACK` (2× processing speed per level, with power rising by the same multiplier) and `CREATIVE` (0 power consumption) on all machines. |
| JEI | Shows this mod's machines as catalysts for Ex Deorum recipes (see next section). |

> **About Mekanism: Empowered upgrades**
> `empowered_speed` / `empowered_energy` / `io_capacity` / `fast_item_insert` /
> `fast_item_eject` / `auto_inserter` are accepted on all machines in this mod
> (registered via reflection only when the mod is present).

## JEI integration

JEI is an optional dependency (the mod works without it; it is not included
in the distributed jar).

- No custom categories are added; this mod's machines are added as catalysts
  to Ex Deorum's categories (`exdeorum:hammer`, `sieve`, `barrel_compost`, etc.).
- Left-clicking a GUI progress bar opens the corresponding JEI recipe directly.

## Screenshots

![GUI](docs/gui-preview.png)

![Side configuration window](docs/side-config-preview.png)

![Barrel GUI (2 inputs + input/output tanks)](docs/barrel-preview.png)

![Water crucible GUI (input + fluid output)](docs/water-crucible-preview.png)

![Lava crucible GUI](docs/crucible-preview.png)

![Hammer factory GUI (20 outputs)](docs/hammer-factory-preview.png)

## Known limitations

- Block textures and the GUI are placeholder assets (meant to be replaced).
  The front panel is a hand-drawn base with a per-family mechanical pattern
  (ribs / punched holes / louvers / hatches / fins), and the sides, top, and
  bottom duplicate Mekanism's Crusher texture.
  See [docs/CUSTOMIZE.md](docs/CUSTOMIZE.md) for details.
- Compressed hammers are for the hammer machine only. The sieve handles both
  normal and compressed blocks with just a mesh.
- The barrel's "burning up in hot fluid" behavior is not implemented (that
  destructive behavior does not suit a machine).
- Fluid injection into the barrel from outside is only allowed while
  compost is 0 and both item input slots are empty.

## Customization / development

- Adjusting textures, GUI, and balance: [docs/CUSTOMIZE.md](docs/CUSTOMIZE.md) (in Japanese)
- Build environment, dependency mechanics, and version updates: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) (in Japanese)
- CurseForge listing text (English): [docs/CURSEFORGE.md](docs/CURSEFORGE.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)

```console
$ nix develop
$ gradle build
```

## License / credits

- This mod: GNU General Public License v3.0 ([LICENSE](LICENSE))
- Base block textures and GUI background: Mekanism (MIT License,
  Copyright Aidancbrady and contributors)
- Uses Ex Deorum's recipes, tags, and machine parts. Ex Deorum is a mod by
  [thedarkcolour](https://github.com/thedarkcolour/ExDeorum).
- Agreeing to the Minecraft EULA (<https://aka.ms/MinecraftEULA>) is the user's
  responsibility.
