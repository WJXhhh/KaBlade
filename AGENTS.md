# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

KBlade2 (`kablade`) — a Forge 1.20.1 addon for **SlashBlade Resharped (拔刀剑重锋)** 1.5.49. Java 17, official Mojang mappings.

### Legacy Reference
- The original 1.12.2 KBlade project is available locally at `E:\KaBlade`. Use it as the reference when comparing or porting old KBlade behavior, recipes, slash arts, world crafting, or blade definitions.

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

### Slash Art / Special Effect Targeting
- Any SA or SE code path that can damage, ignite, debuff, amplify damage, select targets, or spawn damaging summoned entities/projectiles must use `SaTargeting` for friendly-fire rules.
- `SaTargeting.canDamage(owner, target)` is the basic harmful-target check. It rejects null/dead targets and the owner itself, filters players according to config, rejects creative/spectator players, protects configured tamed pets, and applies player/team friendly-fire rules.
- `SaTargeting.canDamageAttackable(owner, target)` includes `canDamage` and then applies the attackable-entity rule used by target scans: `Mob` and `Player` targets are accepted directly; other living entities must pass SlashBlade's `TargetSelector.AttackablePredicate`. Use it when replacing `TargetSelector.AttackablePredicate` in a target selector, and use `canDamage` for direct damage or harmful effects against an already selected target.
- Do not call `Entity#isAlliedTo` directly for SA/SE friendly-fire checks: scoreboard same-team targets are only protected when that team has friendly fire disabled.
- Do not rely on SlashBlade `TargetSelector.AttackablePredicate` as the final selector for Kablade SA damage. It can filter neutral mobs via SlashBlade config; Kablade SA should be able to hit neutral mobs unless blocked by `SaTargeting`.
- Summoned/delayed SA entities must preserve an owner. If the owner cannot be resolved for delayed/pulse damage, cancel that damage rather than falling back to ownerless `magic()` damage.
- Real lightning spawned by SA/SE should be visual-only (`setVisualOnly(true)`) unless there is a deliberate, reviewed reason to allow vanilla lightning damage. Apply controlled damage separately through `SaTargeting`.
- Respect `KabladeConfig.FILTER_PLAYERS_IN_SA_TARGETING`: when enabled (default), Kablade SA/SE harmful selectors filter out players regardless of team.
- Respect `KabladeConfig.PROTECT_TAMED_PETS_IN_SA_TARGETING`: when enabled (default), Kablade SA/SE damage must not hit the user's own tamed pets; allied owners' pets follow scoreboard friendly-fire rules.
- `SaTargeting` treats a null owner as having no friendly-fire restriction, but delayed or summoned damage must not lose its owner. The owner is also used to resolve player attack permissions and scoreboard team relationships.
- `SaFriendlyFireHandler` is a final guard for damage whose direct entity is a Kablade or SlashBlade projectile/summon. It must not replace the per-target check at the point where SA/SE damage or effects are applied.

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
