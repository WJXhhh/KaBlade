# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

KBlade2 (`kablade`) — a Forge 1.20.1 addon for **SlashBlade Resharped (拔刀剑重锋)** 1.5.49. Java 17, official Mojang mappings.

## Build Commands

```bash
# Build the mod jar
./gradlew build

# Run Minecraft client
./gradlew runClient

# Run dedicated server
./gradlew runServer

# (Re)generate SlashBladeDefinition JSON files from KabladeBlades::bootstrap
# Required after adding/changing a blade definition
./gradlew runData

# IDE run config generation
./gradlew genIntellijRuns   # IntelliJ
./gradlew genEclipseRuns    # Eclipse
```

**Note:** Gradle daemon is disabled (`org.gradle.daemon=false` in gradle.properties). The sandbox blocks the Gradle daemon loopback, so `gradlew` tasks can't execute inside this session — validate syntactically with `javac` instead.

## Key Architecture

### Dependency: SlashBlade Resharped
- **MMMaven**: `mods.flammpfeil.slashblade:SlashBlade_Resharped:1.5.49`
- `mixin.env.remapRefMap=true` is required in dev runs because SlashBlade ships mixins with SRG refmaps that need remapping to official mappings at runtime.

### Entry Point: `Main.java`
- `@Mod("kablade")` — wires up DeferredRegisters for items, slash arts, creative tabs.
- Constants `MODID`/`MOD_NAME`/`VERSION` are synced from `gradle.properties` by the `syncMainConstants` Gradle task before compile — edit properties, not Java strings.

### Adding a New Blade (4 files, plus assets)
1. **Material items** — add `RegistryObject<Item>` fields in `ModItems.java` (use `registerItemBase` for simple items).
2. **Blade definition class** — extend `BladeDefineBase` in `blades/` subpackage. Call `context.register()` with the carrier item ID, `RenderDefinition`, and `PropertiesDefinition`. Override `getKey()` to return the JSON path (e.g. `"wjx/ordinary/rimmed_earth"`).
3. **Register blade** — add a `new YourBlade(context)` call in `KabladeBlades.bootstrap()`.
4. **Data gen** — run `./gradlew runData` to produce the JSON under `src/generated/resources/data/kablade/slashblade/named_blades/`.
5. **Assets** — OBJ model, PNG texture, lang entries (en_us.json / zh_cn.json).

### Blade Carrier Item
- `KbladeBladeItem` (`kablade:kablade_blade_named`) extends `ItemSlashBlade` and is the single carrier item for all Kablade named blades.
- The `@CustomBladeModel` annotation on the `KABLADE_BLADE` field triggers `KbladeClientEvents` to swap its inventory model to SlashBlade's 3D `BladeModel`.

### Blade System
- **`KabladeBlades`** — static `bootstrap(BootstapContext)` method called by the `InitializeEvent` data generator. Each blade registers a `SlashBladeDefinition` (carrier + name + render + properties).
- **`BladeDefineBase`** — abstract base; has `BaseBladeType` enum (`ORDINARY` → `kablade_blade_named`, `HONKAI` → `kablade_honkai_named`) to pick the carrier item.
- **`RimmedEarth`** — the only concrete blade so far; fires `hangtu` slash art, uses `katana` carry type, bewitched sword type.
- Definitions are loaded from datapack JSON at runtime by SlashBlade; `fillCreativeTab()` lists all blades whose carrier matches `kablade_blade_named`.

### Mixins
- Config: `kablade.mixins.json` → `com.wjx.kablade.mixin` package.
- `ItemSlashBladeMixin` — example injection into `ItemSlashBlade#hurtEnemy`. Add new mixins by creating a class in the mixin package and listing it in the JSON.

### Slash Arts
- Register via `ModSlashArts.REGISTRY` (DeferredRegister of `SlashArts`).
- `KabladeSlashArts` — fires 3 blue `EntityDrive` projectiles in a spread.

### Creative Tabs
- Two tabs: `tab_kablade` (main) and `tab_kablade_noted`.
- Use `CreativeTabBuilder` to build tabs, add items via `addItem()`/`addStack()`, or add dynamic blade display via `addDisplayItems()`.

### Source Layout
```
src/main/java/com/wjx/kablade/
  api/          — @CustomBladeModel annotation
  blades/       — base/ (BladeDefineBase), ordinary/ (RimmedEarth), honkai/ (placeholder)
  client/       — KbladeClientEvents (model-baking hook)
  event/        — InitializeEvent (GatherDataEvent subscriber)
  init/         — ModItems, ModSlashArts (DeferredRegister hubs)
  mixin/        — ItemSlashBladeMixin
  object/item/  — ItemBase, KbladeBladeItem
  slasharts/    — KabladeSlashArts
  util/         — ResourceUtil, creative_tab/
src/generated/resources/  — datagen output (blade JSON definitions)
src/main/resources/       — lang, models, textures, mixins.json, mods.toml
```

### Resource Paths
- Blade data JSONs are generated under `data/kablade/slashblade/named_blades/`.
- Blade models/textures are at `assets/kablade/model/named/{name}/mdl.obj` / `tex.png`.
- `ResourceUtil.getLocation(path)` creates a `ResourceLocation` under the `kablade` namespace.
