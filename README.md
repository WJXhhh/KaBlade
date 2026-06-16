# Kablade (斩无不断)

**Kablade** 是一个 Minecraft Forge 1.20.1 模组，作为 **[SlashBlade Resharped (拔刀剑重锋)](https://github.com/0999312/SlashBlade-Resharped)** 的附属（Addon），添加了一套全新的命名拔刀剑、合成材料、专属刀技（Slash Arts）与自定义实体渲染管线。

> **Mod ID:** `kablade`  
> **当前版本:** `2.0.1-a`  
> **作者:** WJX  
> **依赖:** Forge ≥47, Minecraft 1.20.1, SlashBlade Resharped ≥1.8

---

## 特性

### 拔刀剑

Kablade 采用 SlashBlade Resharped 的 **数据驱动命名刀系统**——所有刀共用同一个载体物品 (`kablade:kablade_blade_named`)，每把刀的属性、模型、贴图、SA 均由 JSON 定义（datagen 生成），可按合成链路划分为三条分支：

| 刀名 | 等级 | 攻击 | 耐久 | 特性 |
|------|------|------|------|------|
| **无铭「夯土」** | 基础 | 2.0 | 60 | 入门刀，所有分支的起点 |
| **自铭「矍铄」** | 岩石 Lv1 | 5.0 | 120 | 安山岩路线，不可妖化 |
| **自铭「嶙峋」** | 岩石 Lv1 | 5.5 | 100 | 花岗岩路线，不可妖化 |
| **自铭「明磬」** | 岩石 Lv1 | 4.5 | 140 | 闪长岩路线，不可妖化 |
| **铭刀「青藤」** | 自然 Lv1 | 6.0 | 150 | 妖刀，独立模型/贴图 |
| **铁刃「竹光」** | 竹 Lv1 | 6.0 | 190 | 妖刀，独立模型/贴图 |
| **千岩之锋** | 岩石 Lv2 | 6.0 | 400 | 妖刀，自带锋利 II / 耐久 II，专属 SA「岩石撼击」 |

### 合成链路

```
                              ┌── 夯土刀 + 安山岩 → 自铭「矍铄」
无铭「夯土」── 夯土棍 ── 夯土刀 ── 夯土刀 + 花岗岩 → 自铭「嶙峋」── 三合一 + 铁块 → 千岩之锋
                              ├── 夯土刀 + 闪长岩 → 自铭「明磬」
                              ├── 夯土刀 + 藤蔓 + 小麦 → 铭刀「青藤」
                              └── 竹光 + 铁锭 → 铁刃「竹光」
```

### 刀技 (Slash Arts)

#### 普通 SA — `hangtu`
- 发射 **3 发蓝色飞刀弹幕 (EntityDrive)**，呈 20° 扇面散开
- 由 **无铭「夯土」** 使用

#### 专属 SA — `rock_strike` (岩石撼击)
**千岩之锋** 的专属刀技，一道由内向外扩散的环形岩刺冲击波：

- **冲击波**：从脚下向外荡开 2~5 圈波纹（视蓄力等级与精炼度而定）
  - `Success`：2 圈
  - `Jackpot` / `Super`：3 圈，伤害 ×1.3，击飞更高
  - 精炼度每 10 级 +1 圈（上限 +2），精炼 ≥20 时追加爆炸轰鸣
- **岩刺实体** (`RockSpikeEntity`)：每圈圆周顶出真正的 3D 石矛，有出土 → 停留 → 缩回三阶段动画
- **伤害机制**：先 AABB 结算伤害，再将敌人掀飞——杜绝「人先飞走、伤害打空」
- **特效**：碎岩崩起、熔岩火星、升腾尘烟、贴地尘环

### 自定义实体渲染管线

本项目实现了一条完整的自定义实体渲染管线（从零手搓，无需外部模型软件）：

- **`RockSpikeEntity`** — 纯表现实体（无碰撞、无重力、不存盘），通过 `SynchedEntityData` 同步存活时间 / 体型 / 外观种子
- **`RockSpikeModel`** — 客户端手搓石矛几何（顶点数组），无外部 OBJ 依赖
- **`RockSpikeRenderer`** — 材质绑定与渲染
- **`RockSpikeClientEvents`** — 注册实体渲染器

### JEI 集成

通过 `KbladeJeiPlugin` 为 SlashBlade 的 `JEICompat` subtype 解释器注册本模组的载体物品：
- 不同命名刀在 JEI 中各自独立显示，不会挤在一个条目里
- 预览按刀名渲染正确的模型/贴图/颜色

### 全局配置

`config/kablade-common.toml`（启动前即可编辑）：

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `attack_multiplier` | float | 1.0 | 所有命名刀攻击力全局倍率 |
| `durability_multiplier` | float | 1.0 | 所有命名刀最大耐久全局倍率 |

> **注意**：倍率在刀被创建时烤入 NBT，仅影响**之后**新造的刀；存档里已有的刀不受影响。

### 在线更新检查

启动时在守护线程中从远端拉取最新版本号，进入世界后如有新版本则通过 `UpdateNotifier` 提示玩家（静默失败，不阻塞启动）。

---

## 方块与物品

### 方块

| 方块 | 英文 ID | 说明 |
|------|---------|------|
| 夯土 | `rimmed_earth` | 基础合成材料方块 |

### 物品

| 物品 | 英文 ID | 说明 |
|------|---------|------|
| Kablade | `main_mater` | 创造模式标签页图标 |
| 知名 | `noted` | 知名标签页图标 |
| Kablade 核心 | `kablade_core` | 高级合成材料 |
| 夯土棍 | `rimmed_earth_stick` | 合成夯土刀的刀棍 |

---

## 开发环境

### 前置需求

- Java 17 (JDK)
- Minecraft Forge 1.20.1 — 47.4.0

### 快速开始

```bash
# 克隆仓库
git clone https://github.com/WJXhhh/KBlade2.git
cd KBlade2

# 生成 IDE 运行配置
./gradlew genIntellijRuns   # IntelliJ IDEA
# 或
./gradlew genEclipseRuns    # Eclipse

# 构建
./gradlew build

# 运行
./gradlew runClient
./gradlew runServer
```

### 数据生成

添加或修改命名刀后，需要重新生成刀定义的 JSON 文件：

```bash
./gradlew runData
```

产物输出到 `src/generated/resources/data/kablade/slashblade/named_blades/`。

### 添加一把新刀（开发者）

1. **创建刀定义类** — 在 `blades/ordinary/` 下继承 `BladeDefineBase`，调用 `context.register()`，覆写 `getKey()`
2. **注册刀** — 在 `BladeLoader.bootstrap()` 中添加 `new YourBlade(context)`
3. **素材** — 添加 OBJ 模型 (`assets/kablade/model/named/{name}/mdl.obj`) 与 PNG 贴图 (`tex.png`)
4. **数据生成** — 执行 `./gradlew runData`
5. **合成配方** — 编写 JSON 配方（`data/kablade/recipes/`）

### 构建须知

- **Gradle Daemon 已禁用** (`org.gradle.daemon=false`)，每次构建从头启动
- **`syncMainConstants`** 任务会在编译前将 `gradle.properties` 中的版本号同步到 `Main.java`，单一起源
- 开发环境需要 `mixin.env.remapRefMap=true`（已内置于 `build.gradle`），否则 SlashBlade 的 Mixin 在官方映射下会崩溃
- `mods.toml` 和 `pack.mcmeta` 通过独立的 `Copy` 任务展开模板变量，而非嵌入 `processResources`

---

## 依赖

| 依赖 | 版本 | 类型 | 来源 |
|------|------|------|------|
| Minecraft Forge | 47.4.0 | 必需 | [forge](https://files.minecraftforge.net/) |
| SlashBlade Resharped | ≥1.8 (当前 1.8.62) | 必需 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/slashblade-resharped) |
| JEI | 15.20.0.116 | 可选(编译/运行时) | [BlameJared](https://maven.blamejared.com) |

---

## 致谢

- **[flammpfeil](https://github.com/flammpfeil)** — 原版 SlashBlade 的创造者
- **[0999312](https://github.com/0999312)** — SlashBlade Resharped (拔刀剑重锋) 的维护者
- **Minecraft Forge 团队** — 模组加载框架
- 所有在开发过程中提供反馈与建议的朋友们

---

## 许可

All Rights Reserved.
