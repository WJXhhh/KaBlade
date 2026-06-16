# Kablade (斩无不断)

**Kablade** 是一个 Minecraft Forge 1.20.1 模组，作为 **[SlashBlade Resharped (拔刀剑重锋)](https://github.com/0999312/SlashBlade-Resharped)** 的附属（Addon），添加命名拔刀剑、合成材料与专属刀技。

> **Mod ID:** `kablade`  
> **作者:** JDJades  
> **依赖:** Forge ≥47, Minecraft 1.20.1, SlashBlade Resharped ≥1.8

---

## 特性

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

### 构建须知

- **Gradle Daemon 已禁用** (`org.gradle.daemon=false`)，每次构建从头启动
- **`syncMainConstants`** 任务会在编译前将 `gradle.properties` 中的版本号同步到 `Main.java`，单一起源
- 开发环境需要 `mixin.env.remapRefMap=true`（已内置于 `build.gradle`），否则 SlashBlade 的 Mixin 在官方映射下会崩溃
- `mods.toml` 和 `pack.mcmeta` 通过独立的 `Copy` 任务展开模板变量，而非嵌入 `processResources`

---

## 依赖

| 依赖 | 类型 | 来源 |
|------|------|------|
| Minecraft Forge 1.20.1 | 必需 | [forge](https://files.minecraftforge.net/) |
| SlashBlade Resharped | 必需 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/slashblade-resharped) |
| JEI | 可选 | [BlameJared](https://maven.blamejared.com) |

---

## 致谢

- **[flammpfeil](https://github.com/flammpfeil)** — 原版 SlashBlade 的创造者
- **[0999312](https://github.com/0999312)** — SlashBlade Resharped (拔刀剑重锋) 的维护者
- **Minecraft Forge 团队** — 模组加载框架
- 所有在开发过程中提供反馈与建议的朋友们

---

## 许可

MPL-2.0
