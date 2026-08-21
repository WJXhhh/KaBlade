# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

KBlade2 (`kablade`) — a Forge 1.20.1 addon for **SlashBlade Resharped (拔刀剑重锋)**. Java 17, official Mojang mappings. The current development build is pinned to SlashBlade Resharped 1.9.65; the runtime dependency range starts at 1.8.

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

# (Re)generate SlashBladeDefinition JSON files from BladeLoader::bootstrap
# Required after adding/changing a blade definition
./gradlew runData

# IDE run config generation
./gradlew genIntellijRuns   # IntelliJ
./gradlew genEclipseRuns    # Eclipse
```

## Key Architecture

### Dependency: SlashBlade Resharped
- **Current development dependency**: CurseMaven coordinate `curse.maven:slashblade-resharped-1022428:8090912` (semantic version 1.9.65).
- **Runtime compatibility range**: `[1.8,)`, declared in `gradle.properties` and expanded into `mods.toml`.
- `mixin.env.remapRefMap=true` is required in dev runs because SlashBlade ships mixins with SRG refmaps that need remapping to official mappings at runtime.

### Entry Point: `Main.java`
- `@Mod("kablade")` — wires up DeferredRegisters for items, slash arts, creative tabs.
- Constants `MODID`/`MOD_NAME`/`VERSION` are synced from `gradle.properties` by the `syncMainConstants` Gradle task before compile — edit properties, not Java strings.

### Adding a New Blade
1. **Material items** — if the recipe needs new materials, add `RegistryObject<Item>` fields in `ModItems.java` and place them in the appropriate creative tab. Use `registerItemBase` for simple items.
2. **Blade definition class** — extend `BladeDefineBase` in the relevant `blades/` subpackage. Select one of `BaseBladeType.ORDINARY`, `HONKAI`, `SP_LIGHT`, or `ALL_WEAPON`. Register a `SlashBladeDefinition` with the carrier item, render definition, and properties definition. Override `getKey()` with a plain path such as `"greatsword"` or `"nuclear_pri_ex"`; do not use the old `wjx/ordinary/...` example.
3. **Register blade** — add the definition field and a `new YourBlade(context)` call in `BladeLoader.bootstrap()`.
4. **Data gen** — run `./gradlew runData` to produce the JSON under `src/generated/resources/data/kablade/slashblade/named_blades/`.
5. **Assets** — add the required model/texture assets under the relevant `assets/kablade/model/` subdirectory and update `en_us.json` / `zh_cn.json`. Some definitions intentionally reuse a SlashBlade model, so an OBJ is not required for every blade.

### Blade Carrier Item
- `KbladeBladeItem` extends `ItemSlashBlade` and is used by four carrier items:
  - `kablade:kablade_blade_named` — ordinary named blades
  - `kablade:kablade_honkai_named` — Honkai blades
  - `kablade:kablade_sl_named` — SP Light blades
  - `kablade:kablade_aw_named` — AllWeapon blades
- Each carrier field in `ModItems.java` is annotated with `@CustomBladeModel`; `KbladeClientEvents` swaps its inventory model to SlashBlade's 3D `BladeModel`.

### Blade System
- **`BladeLoader`** — owns the static `bootstrap(BootstapContext<SlashBladeDefinition>)` method called by the `InitializeEvent` data generator. It currently registers the ordinary, Honkai, SP Light, and AllWeapon blade definitions, including the Honkai greatsword series.
- **`BladeDefineBase`** — abstract base with `BaseBladeType` values `ORDINARY`, `HONKAI`, `SP_LIGHT`, and `ALL_WEAPON`, which map to the four carrier items above.
- Definitions are loaded from datapack JSON at runtime by SlashBlade. `BladeLoader` provides separate creative-tab fillers for ordinary, Honkai katana, Honkai greatsword, SP Light, and AllWeapon definitions.

### Mixins
- Config: `kablade.mixins.json` → `com.wjx.kablade.mixin` package.
- Common mixin: `SlashBladeShapedRecipeMixin`.
- Client mixins: `client.IngredientMixin` and `client.LayerMainBladeMixin`.
- Add new mixins by creating a class in the appropriate mixin package and listing it in `kablade.mixins.json`; there is no current `ItemSlashBladeMixin`.

### Slash Arts
- Register via `ModSlashArts.REGISTRY` (DeferredRegister of `SlashArts`).
- The registry is defined in `blades/ModSlashArts.java`; individual implementations live in `slasharts/` and may spawn custom entities, special effects, particles, or client render state.

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
- Six tabs are registered in `Main.java`: `tab_1_kablade`, `tab_2_noted`, `tab_3_honkai`, `tab_4_honkai_greatsword`, `tab_5_sp_light`, and `tab_6_allweapon`.
- Use `CreativeTabBuilder` to build tabs, add items via `addItem()`/`addStack()`, or add dynamic blade display via `addDisplayItems()`.

### Configuration
- Common config: `config/kablade-common.toml`, with `blade_multiplier` and `slash_art_targeting` sections.
- Client config: `config/kablade-client.toml`, with `skill_shader`, `raiden_cyclone`, `raizan_cleave`, `thunderbolt_call`, and `narukami_divinity` sections.
- Keep SA/SE harmful targeting routed through `SaTargeting` and preserve the owner on delayed or summoned damage paths. See the targeting rules below before adding a new harmful effect.

### Source Layout
```
src/main/java/com/wjx/kablade/
  api/          — @CustomBladeModel annotation
  blades/       — BladeLoader, ModSlashArts, base/, ordinary/, honkai/, honkai/claymore/, splight/, allweapon/
  client/       — model baking, entity renderers, client state, shaders, and effect pipelines
  compat/       — JEI and EMI integrations
  config/       — common and client ForgeConfigSpec definitions
  data/         — player data providers and capability-backed data
  entity/       — server-side summoned entities/projectiles
  event/        — lifecycle, update, attribute, drop, and friendly-fire handlers
  init/         — items, blocks, entities, combo states, slash arts, special effects, mob effects
  mixin/        — common and client mixins listed in kablade.mixins.json
  mobeffect/    — custom mob effects
  network/      — packets for server/client effect state
  object/item/  — item implementations, including KbladeBladeItem
  property/     — blade/player property systems
  slasharts/    — Slash Arts and their timelines
  specialeffect/ — SlashBlade special effects
  update/       — remote version checker
  util/         — ResourceUtil, SaTargeting, creative_tab/, and shared helpers
  worldcraft/   — world-crafting and related gameplay events
src/generated/resources/  — datagen output (blade JSON definitions)
src/main/resources/       — lang, model/models, textures, effects, shaders, data, mixins.json, mods.toml
```

### Resource Paths
- Blade data JSONs are generated under `data/kablade/slashblade/named_blades/`.
- Blade model/texture paths depend on the blade series. Common roots are `assets/kablade/model/named/{name}/`, `assets/kablade/model/honkai_claymore/{name}/`, `assets/kablade/model/splight/`, and `assets/kablade/model/allweapon/{name}/`. A definition may also reuse a model from the `slashblade` namespace.
- `ResourceUtil.getLocation(path)` creates a `ResourceLocation` under the `kablade` namespace.
