# Kablade (斩无不断)

**Kablade** 是一个 Minecraft Forge 1.20.1 模组，作为 **[SlashBlade Resharped (拔刀剑重锋)](https://github.com/0999312/SlashBlade-Resharped)** 的附属（Addon），添加命名拔刀剑、合成材料、专属刀技、特殊效果和对应的客户端演出。

> **Mod ID:** `kablade`<br>
> **当前版本:** `2.8.0-b`<br>
> **作者:** JDJades<br>
> **运行环境:** Minecraft 1.20.1，Forge 47.x<br>
> **SlashBlade 依赖:** 运行时最低 `1.8`；当前开发构建锁定 `1.9.65`（CurseForge 文件 `8090912`）

---

## 特性

### 命名拔刀剑与创造模式分页

当前仓库包含普通命名刀、铭刀、崩坏系列、龙一文字改系列和万物皆刃系列；数据生成目录中当前有 85 个命名刀定义。

模组提供 6 个创造模式分页：

- `tab_1_kablade`：普通物品、材料和方块
- `tab_2_noted`：普通命名拔刀剑
- `tab_3_honkai`：崩坏太刀
- `tab_4_honkai_greatsword`：崩坏大剑
- `tab_5_sp_light`：龙一文字改系列
- `tab_6_allweapon`：万物皆刃系列

### JEI 与 EMI 集成

- JEI：通过 `KbladeJeiPlugin` 为 4 类命名刀载体注册 SlashBlade subtype 解释器，使不同刀名在 JEI 中独立显示，并使用正确的模型、贴图和颜色预览。
- EMI：通过 `KbladeEmiPlugin` 注册命名刀堆叠、刀类合成表和对应的刀身预览。

两个兼容模块都是可选的；未安装 JEI 或 EMI 时，模组仍可正常运行。

### 全局配置

通用配置文件为 `config/kablade-common.toml`，启动游戏后生成，可在下次启动前编辑。配置分为两个区段：

| 配置路径 | 类型 | 默认值 | 说明 |
|---|---|---:|---|
| `blade_multiplier.attack_multiplier` | double | `1.0` | 所有命名刀基础攻击力倍率 |
| `blade_multiplier.durability_multiplier` | double | `1.0` | 所有命名刀最大耐久倍率 |
| `slash_art_targeting.filter_players` | boolean | `true` | SA 目标选择是否过滤玩家 |
| `slash_art_targeting.protect_tamed_pets` | boolean | `true` | SA/SE 伤害是否保护玩家自己的驯服宠物；盟友宠物遵循队伍友伤规则 |
| `slash_art_targeting.sa_all_use_targets` | boolean | `false` | 是否让 `SaTargeting` 全部使用 SlashBlade 的 `TargetSelector` |

攻击力和耐久倍率会在刀被创建时写入其属性，仅影响之后新创建的刀；存档中已有的刀不会自动改变。

客户端演出配置文件为 `config/kablade-client.toml`，包括以下区段：

- `skill_shader.mode`：`AUTO`、`FORCE_VANILLA_CUSTOM`、`FORCE_OCULUS_POST`，默认 `AUTO`
- `raiden_cyclone`：质量、镜头震动和闪光控制，默认质量 `HIGH`
- `raizan_cleave`：质量、镜头震动和闪光控制，默认质量 `HIGH`
- `thunderbolt_call`：镜头震动、闪光和调试锚点
- `narukami_divinity`：镜头震动和闪光控制

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
- **`syncMainConstants`** 任务会在编译前将 `gradle.properties` 中的 Mod ID、名称和版本同步到 `Main.java`；请修改 `gradle.properties`，不要直接改 Java 常量
- 开发环境需要 `mixin.env.remapRefMap=true`（已内置于 `build.gradle`），否则 SlashBlade 的 Mixin 在官方映射下可能无法找到目标
- `mods.toml` 和 `pack.mcmeta` 通过独立的 `Copy` 任务展开模板变量，而非嵌入 `processResources`
- `runClient` 默认加载 Embeddium + Oculus 作为客户端着色器测试环境；如需关闭，可运行 `./gradlew runClient -PenableShaderMods=false`

---

## 依赖

| 依赖 | 类型 | 版本/范围 | 来源 |
|---|---|---|---|
| Minecraft | 必需 | `1.20.1` | — |
| Minecraft Forge | 必需 | `47.x`（开发环境为 `47.4.0`） | [Forge](https://files.minecraftforge.net/) |
| SlashBlade Resharped | 必需 | 运行时最低 `1.8`；开发构建 `1.9.65` | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/slashblade-resharped) |
| JEI | 可选 | 1.20.1，开发运行时 `15.20.0.116` | [BlameJared](https://maven.blamejared.com) |
| EMI | 可选 | `1.1+` | [Terraformers](https://maven.terraformersmc.com/) |

---

## 致谢

- **[flammpfeil](https://github.com/flammpfeil)** — 原版 SlashBlade 的创造者
- **[0999312](https://github.com/0999312)** — SlashBlade Resharped (拔刀剑重锋) 的维护者
- **Minecraft Forge 团队** — 模组加载框架
- 所有在开发过程中提供反馈与建议的朋友们

---

## 许可

本模组基于 **[CC BY-NC-SA 4.0（署名 - 非商业性使用 - 相同方式共享）](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh)** 协议发布。法律全文见 [LICENSE.txt](LICENSE.txt)，附加条款见 [ADDITIONAL_TERMS.md](ADDITIONAL_TERMS.md)。

简而言之：

- **BY 署名** — 转载、二改必须公开标明原作者（JDJades）。
- **NC 非商业** — 禁止任何人将本模组及其衍生版本用于商业牟利。
- **SA 相同方式共享** — 二改版本必须以相同协议免费开源，不得闭源售卖。

### ⚠️ 特别附加条款 (Anti-Commercial & NetEase Clause)

> 本模组（Mod）完全免费且开源，严禁任何人将本模组或其修改版本、衍生版本用于任何形式的商业牟利行为（包括但不限于付费下载、代练收费、捆绑商业服务端销售等）。
>
> 未经原作者（JDJades）书面授权，严禁将本模组或其任何修改/衍生版本上传至网易《我的世界》中国版（游戏内组件中心 / 开发者平台）。
>
> 一经发现上述侵权行为，原作者保留依法追究侵权者法律责任、要求下架及索赔的权利。
