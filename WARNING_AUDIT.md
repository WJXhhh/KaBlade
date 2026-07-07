# KaBlade 黄色警告审计与修复报告

> 项目：Minecraft Forge 1.12.2 模组，Java 8
> 检测方式：`javac -Xlint:all` 编译（经 `lint-init.gradle` 注入）
> 最终状态：**项目代码警告全部清零** ✅

---

## 一、修复成果总览

| 类别 | 修复前 | 修复后 | 说明 |
|------|--------|--------|------|
| `[cast]` 冗余强制转换 | 22 处 | **0** ✅ | 删除冗余 cast |
| `[rawtypes]` 原始类型 | ~35 处 | **0** ✅ | 泛型化或 @SuppressWarnings |
| `[unchecked]` 未检查操作 | ~10 处 | **0** ✅ | 泛型化或 @SuppressWarnings |
| `[deprecation]` 已弃用 API | ~53 处 | **0** ✅ | I18n 封装、API 替换、@SuppressWarnings |
| 编译错误 | 1 处 | **0** ✅ | Iterator 泛型化修复 |
| 依赖 jar classfile 警告 | 18 处 | 18 处 | 不可修复（见下文说明） |

**编译结果：BUILD SUCCESSFUL，0 error，0 warning（项目代码）**

---

## 二、修复详情

### 1. `[cast]` 冗余强制转换（22 处 → 0）

**集合泛型化后 cast 冗余**（Iterator/List 泛型化后删除 cast）：
- `EntityDriveAdd.java`、`EntitySlashDimensionAdd.java`、`EntitySummonedButterfly.java`、`EntitySummonedSwordBasePlus.java`、`EntitySummonHedra.java`、`EntitySummonSwordFree.java`
- `EntityLightningSword.java`、`EntityPhantomSwordEx.java`、`ExSaEntityDrive.java`、`EntityNoFireLightningBolt.java`
- `KillEvent.java`、`Overslash.java`、`RenderDriveEx.java`、`RenderEntityDriveAdd.java`、`RenderEntityRainUmbrella.java`、`WorldEvent.java`
- `AL_Yueyatianchong.java`

**Recipe 文件 NBTTagCompound.copy() cast**（4 处）：
- `SlashBladeRecipeModding.java`、`SlashBladeThreeRecipeModding.java`、`SlashBladeTwoRecipeModding.java`、`SlashBladeTwoRecipeModdingT.java`
  - `oldTag = (NBTTagCompound)oldTag.copy()` → `oldTag = oldTag.copy()`

**RecipeBlade.java Integer cast**（2 处）：
- `Map<Enchantment, Integer>.get()` 已返回 Integer，cast 冗余

**specialattack 文件 float cast**（14 处）：
- 13 个 `Honkai*.java` / `SaAuroraShining.java` 文件中 `(float)MathFunc.amplifierCalc(...)` 冗余（方法已返回 float）

**SaAuroraShining.java EntityLivingBase cast**（1 处）：
- `ee` 已声明为 `EntityLivingBase`，cast 冗余

### 2. `[rawtypes]` 原始类型（~35 处 → 0）

**Iterator 泛型化**：
- `Iterator var6` → `Iterator<Entity> var6`（EntityDriveAdd、ExSaEntityDrive 等）
- `Iterator i$` → `Iterator<Entity> i$`（EntityLightningSword、EntityPhantomSwordEx 等）
- `RecipeBlade.java`：`Iterator var9/var15` → `Iterator<Enchantment>`

**Class 泛型化**：
- `CommonProxy.java`：`Class[][]` → `Class<?>[][]`（14 处）
- `ArrayLib.java`：`Class[][]` → `Class<?>[][]`
- `ParticleManager.java`：`new Class[]` → `new Class<?>[]`（3 处）

**Render 类泛型化**：
- `RenderTest.java`、`RenderSlashDimensionAdd.java`：`extends Render` → `extends Render<Entity>`

**Mixin LocalCapture 签名（不可改泛型）**：
- `MixinItemSlashBladeDoAttack1.java`、`MixinItemSlashBladeDoAttack2.java`：LocalCapture 必须匹配目标方法 raw Iterator，加 `@SuppressWarnings("rawtypes")`

**Override 父类 raw List（不可改泛型）**：
- `Item_AwNamed.java`、`Item_Caijue.java`、`MagicBlade.java`：父类 `ItemSlashBlade` 用 raw List，子类不能改 `List<String>`，加 `@SuppressWarnings({"rawtypes", "unchecked"})`

### 3. `[unchecked]` 未检查操作（~10 处 → 0）

与 rawtype 同源（raw List 上调用 `add(E)`），通过 @SuppressWarnings 一并消除。

**反射 cast 不可消除**：
- `EntityAquaEdge.java`、`EntityFlareEdge.java`：`field.get(null)` cast 到 `DataParameter<Boolean>`，加 `@SuppressWarnings("unchecked")`

### 4. `[deprecation]` 已弃用 API（~53 处 → 0）

#### I18n 弃用迁移（核心修复）

**方案**：创建 `com.wjx.kablade.util.I18nUtil` 工具类，用全限定名调用 `net.minecraft.util.text.translation.I18n.translateToLocal`（避免 import 警告），方法加 `@SuppressWarnings("deprecation")`。所有调用方改为 `I18nUtil.translate(key)`。

**涉及文件**（7 个）：
- `KillEvent.java`（2 处聊天消息）
- `WorldEvent.java`（4 处 tooltip，已有 @SideOnly）
- `ItemInit.java`（8 处 tooltip）
- `Item_Caijue.java`（5 处 tooltip）
- `MagicBlade.java`（多处 tooltip + getItemStackDisplayName）
- `EntityRaikiriBlade.java`（2 处聊天消息）

**行为保持**：服务端可调用（与原 I18n.translateToLocal 行为一致）。

#### ReflectionHelper 弃用
- `EntityAquaEdge.java`：删除未使用的 `import ReflectionHelper`

#### CapabilityManager.register 弃用
- `CapabilityLoader.java`：方法加 `@SuppressWarnings("deprecation")`（Forge 1.12.2 无替代 API）

#### Block.getCollisionBoundingBox 弃用
- `EntityLightningSword.java:148`：`nBlock.getCollisionBoundingBox(state, world, pos)` → `state.getCollisionBoundingBox(world, pos)`（用 IBlockState 方法替代 Block 方法）

#### Potion.renderInventoryEffect/renderHUDEffect 弃用
- `PotionParaly.java`：override 父类弃用方法（1.12.2 无替代事件），加 `@SuppressWarnings("deprecation")`

#### ForgeHooksClient.registerTESRItemStack 弃用
- `ItemSlashUtil.java`：提取为 `registerTESRForBlade` 方法加 `@SuppressWarnings("deprecation")`（替代方案复杂且改变渲染行为）

### 5. 编码错误修复

- `build.gradle`：`updateMainClassVersion` task 的 `file.text`（默认 GBK）→ `file.getText('UTF-8')` / `file.write(text, 'UTF-8')`
- `Main.java:280`：修复被 GBK 损坏的中文注释

### 6. 集合泛型化重构

- `EntitySummonedButterfly.java`、`EntitySummonedSwordBasePlus.java`、`EntitySummonHedra.java`、`EntitySummonSwordFree.java`：
  - `Predicate<Entity>[] selectors = new Predicate[]{...}` → `List<Predicate<Entity>> selectors = Arrays.asList(...)`
  - 数组+索引循环 → List + for-each

---

## 三、不可修复的警告（依赖 jar classfile）

**18 个 "未知的枚举常量 cpw.mods.fml.relauncher.Side.CLIENT" 警告**：
- 来源：依赖 jar（SlashBlade 等）的 class 文件使用旧路径 `@cpw.mods.fml.relauncher.SideOnly(Side.CLIENT)` 注解
- 原因：Forge 1.12.2 后期将 `cpw.mods.fml.relauncher.Side` 迁移到 `net.minecraftforge.fml.relauncher.Side`，旧路径在编译 classpath 中不存在
- 分布：6 处 ×3（Mixin AP 多轮处理放大）= 18 个
- 修复方式：无法通过项目代码修改消除（需依赖 jar 重新编译）
- IDE 表现：IntelliJ/Eclipse 通常不显示此类 classfile 警告（仅 javac -Xlint 报告）

---

## 四、验证方法

```bash
# 使用 lint-init.gradle 注入 -Xlint:all 编译
cmd /c "chcp 65001 >nul & gradlew.bat clean compileJava --init-script lint-init.gradle --offline --console=plain"
```

`lint-init.gradle` 内容：
```groovy
allprojects {
    tasks.withType(JavaCompile).configureEach {
        options.compilerArgs << '-Xlint:all'
        options.compilerArgs << '-Xlint:-processing'
        options.compilerArgs << '-Xlint:-classfile'
        options.compilerArgs << '-Xlint:-path'
    }
}
```

最终编译输出：`BUILD SUCCESSFUL`，项目代码 0 warning。
