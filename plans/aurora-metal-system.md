# 极光金属系列 — 实现计划

## 概述

添加完整的极光金属材料体系：矿石（仅寒带生物群系生成）、碎片、锭、工具（剑/镐/斧/锄）、坚固玻璃棒（工具柄）。**不包含拔刀剑**。

## 1.20.1 适配要点

- **纹理路径**：1.12.2 的 `textures/blocks/` / `textures/items/` → 1.20.1 的 `textures/block/` / `textures/item/`
- **矿石生成**：1.12.2 的代码式 OreGen → 1.20.1 的 JSON data-driven（ConfiguredFeature + PlacedFeature + BiomeModifier）
- **工具 Tier**：1.12.2 的 `EnumHelper.addToolMaterial` → 1.20.1 的 `Tier` 接口 + `TierSortingRegistry`
- **新群系**：1.20.1 新增了 Frozen Peaks、Jagged Peaks、Snowy Slopes、Grove、Deep Frozen Ocean 等寒带群系，一并纳入

---

## 文件清单

### Java（4 文件修改）

| 文件 | 改动 |
|---|---|
| `init/ModItems.java` | +7 项：aurora_fragment、aurora_metal_ingot、sturdy_glass_stick、极光金属剑/镐/斧/锄（SwordItem/PickaxeItem/AxeItem/HoeItem）+ AURORA_METAL Tier 定义 |
| `init/ModBlocks.java` | +1 项：aurora_ore（strength 3.0, requiresCorrectToolForDrops） |
| `Main.java` | commonSetup 中注册 `BiomeModifiers.addForgeWorldgenEvent()` 不需要——JSON biome modifier 自动生效，无需改 Main |
| `config/KabladeConfig.java` | 不改——KabladeConfig 专注拔刀剑倍率，矿石参数暂硬编码（vein=7, attempts=13） |

### JSON — 矿石生成（3 文件新建）

| 文件 | 说明 |
|---|---|
| `data/kablade/worldgen/configured_feature/aurora_ore.json` | OreConfiguration: target=stone, size=7（被 config 覆盖） |
| `data/kablade/worldgen/placed_feature/aurora_ore.json` | 在 Y=40~80 间放置，三角分布，每 chunk 尝试 13 次 |
| `data/kablade/forge/biome_modifier/aurora_ore.json` | `forge:add_features` 类型，biomes 引用 `kablade:has_aurora_ore` 标签 |

### JSON — 生物群系标签（1 文件新建）

| 文件 | 说明 |
|---|---|
| `data/kablade/tags/worldgen/biome/has_aurora_ore.json` | 寒带群系白名单（见下方列表） |

### JSON — 模型/方块状态（4 文件新建）

| 文件 | 说明 |
|---|---|
| `assets/kablade/blockstates/aurora_ore.json` | 单变体 → `kablade:block/aurora_ore` |
| `assets/kablade/models/block/aurora_ore.json` | cube_all → `kablade:block/aurora_ore` |
| `assets/kablade/models/item/aurora_ore.json` | 继承 block 模型 |
| `assets/kablade/models/item/sturdy_glass_stick.json` | generated, layer0 |

### JSON — 战利品表（1 文件新建）

| 文件 | 说明 |
|---|---|
| `data/kablade/loot_tables/blocks/aurora_ore.json` | 掉落 aurora_fragment 1~2 个，支持时运 |

### JSON — 配方（7 文件新建）

| 文件 | 配方 |
|---|---|
| `recipes/sturdy_glass_stick.json` | 3×(玻璃 铁锭 玻璃) → 8 坚固玻璃棒 |
| `recipes/aurora_metal_ingot.json` | 菱形：4 碎片 + 1 金锭 → 2 锭 |
| `recipes/aurora_metal_sword.json` | 标准剑型：2 锭 + 1 玻璃棒 |
| `recipes/aurora_metal_pickaxe.json` | 标准镐型：3 锭 + 2 玻璃棒 |
| `recipes/aurora_metal_axe.json` | 标准斧型：3 锭 + 2 玻璃棒 |
| `recipes/aurora_metal_hoe.json` | 标准锄型：2 锭 + 2 玻璃棒 |
| `recipes/aurora_ore.json` | smelting + blasting：矿石 → 1 碎片（作为 silk_touch 替代路径） |

### JSON — 进度（7 文件新建）

每个合成配方对应一个 advancement 文件（`data/kablade/advancements/recipes/combat/` 或 `misc/`）。

### 纹理（1 文件移动 + 7 文件路径确认）

| 操作 | 文件 |
|---|---|
| **移动** | `textures/blocks/aurora_ore.png` → `textures/block/aurora_ore.png` |
| 已就位 | `textures/item/aurora_fragment.png`、`aurora_metal_ingot.png`、`aurora_metal_sword.png`、`aurora_metal_pickaxe.png`、`aurora_metal_axe.png`、`aurora_metal_hoe.png` |
| **需新建** | `textures/item/sturdy_glass_stick.png`（从 1.12.2 复制或新建） |

### Lang（2 文件修改）

**en_us.json 新增：**
- `block.kablade.aurora_ore` = Aurora Ore
- `item.kablade.aurora_fragment` = Aurora Fragment
- `item.kablade.aurora_metal_ingot` = Aurora Metal Ingot
- `item.kablade.sturdy_glass_stick` = Sturdy Glass Stick
- `item.kablade.aurora_metal_sword` = Aurora Metal Sword
- `item.kablade.aurora_metal_pickaxe` = Aurora Metal Pickaxe
- `item.kablade.aurora_metal_axe` = Aurora Metal Axe
- `item.kablade.aurora_metal_hoe` = Aurora Metal Hoe

**zh_cn.json 新增：**
- `block.kablade.aurora_ore` = 极光矿石
- `item.kablade.aurora_fragment` = 极光残片
- `item.kablade.aurora_metal_ingot` = 极光金属锭
- `item.kablade.sturdy_glass_stick` = 坚固玻璃棒
- `item.kablade.aurora_metal_sword` = 极光金属剑
- `item.kablade.aurora_metal_pickaxe` = 极光金属镐
- `item.kablade.aurora_metal_axe` = 极光金属斧
- `item.kablade.aurora_metal_hoe` = 极光金属锄

---

## 极光金属 Tier 属性

| 属性 | 值 | 对比（钻石/下界合金） |
|---|---|---|
| Uses (耐久) | 1000 | 钻石 1561 / 下界合金 2031 |
| Speed (效率) | 7.5 | 钻石 8.0 / 下界合金 9.0 |
| AttackDamageBonus | 3.2 | 钻石 3.0 / 下界合金 4.0 |
| Enchantability | 25 | 钻石 10 / 下界合金 15 |
| 修复材料 | aurora_metal_ingot | |

**定位**：介于钻石和下界合金之间，但附魔能力极高（25，全游戏最高）。适合作为映天拔刀剑的前置材料。

工具攻击力：
- 剑：3.2 + 4 = **7.2**（基础攻击力）
- 镐：3.2 + 1 = **4.2**
- 斧：3.2 + 5.5 = **8.7**（1.12.2 原值）
- 锄：3.2（无额外加成）

---

## 寒带生物群系白名单

1.12.2 原版 + 1.20.1 新增：

| 1.12.2 名称 | 1.20.1 ResourceLocation |
|---|---|
| COLD_TAIGA | `minecraft:taiga` (默认 taiga 就是冷的) |
| COLD_TAIGA_HILLS | `minecraft:taiga` (合并) |
| MUTATED_TAIGA_COLD | `minecraft:old_growth_spruce_taiga` |
| ICE_PLAINS | `minecraft:snowy_plains` |
| MUTATED_ICE_FLATS | `minecraft:ice_spikes` |
| ICE_MOUNTAINS | `minecraft:frozen_peaks` |
| COLD_BEACH | `minecraft:snowy_beach` |
| FROZEN_RIVER | `minecraft:frozen_river` |
| FROZEN_OCEAN | `minecraft:frozen_ocean` / `minecraft:deep_frozen_ocean` |
| **1.20.1 新增** | `minecraft:jagged_peaks` |
| | `minecraft:snowy_slopes` |
| | `minecraft:grove` |
| | `minecraft:snowy_taiga` |

---

## 实现顺序

1. 纹理移动 + sturdy_glass_stick 纹理
2. ModBlocks.java + blockstate/model JSON + loot table
3. ModItems.java（Tier + 7 项物品）+ item model JSON
4. 配方 JSON ×7 + 进度 JSON ×7
5. 世界生成 JSON ×3 + biome tag
6. Lang 更新
7. `./gradlew runData`（不需要，因为没有拔刀剑定义改动）
8. 编译验证
