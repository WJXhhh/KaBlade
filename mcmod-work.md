# MC 百科工作记录

## 项目对应关系

- 项目：KBlade2
- MC 百科模组：斩无不断2 (Ka Blades 2)
- MC 百科 ID：`28013`
- 模组页面：https://www.mcmod.cn/class/28013.html
- 最后核对（本地源码与工作记录）：2026-08-24

## 已确认事项

- 游戏设定分类 `SA` 与 `SE` 已建立；`SE` 分类 ID 为 `1043995`。
- 2026-08-02：82 把拔刀剑资料均已直接发布攻击速度与耐久修正，公开页逐条核验通过。攻击速度统一为 `-2.4`，耐久值按源码 `.maxDamage` 定义写入攻击伤害下一行。
- 2026-08-18：对照本地 84 个生成拔刀定义与 MC 百科物品列表，补齐并直接发布 `大剑`（Greatsword，耐久 300，资料 ID `980257`）：https://www.mcmod.cn/item/980257.html，以及 `融核动力剑初型`（Nuclear PRI，耐久 350，资料 ID `980258`）：https://www.mcmod.cn/item/980258.html。两条资料均归入 `斩无不断` / `崩坏`，图标取自本地 `IconR` 导出。资料 `959081`「御灵刀·寒狱冰天」此前已按本地语言文件改名为「御灵刀 寒狱冰天」，并在公开页核验已直接发布：https://www.mcmod.cn/item/959081.html。
- 2026-08-18：为 `大剑` 添加并发布工作台有序合成表（合成表 ID `336641`，3 个钻石块 + 2 把钻石剑，形状沿用本地 `greatsword.json`）：https://www.mcmod.cn/item/tab/edit/336641/；为 `融核动力剑初型` 添加并发布工作台有序合成表（合成表 ID `336642`，2 个铁块 + 2 个红石粉 + 2 根烈焰棒 + 1 把大剑，形状沿用本地 `nuclear_pri.json`）：https://www.mcmod.cn/item/tab/edit/336642/。公开页已回读核验物品 ID、槽位、数量与输出均正确。
- 2026-08-24：对照本地 87 个生成拔刀定义与 MC 百科物品列表，已直接发布缺失的三把拔刀资料：`融核动力剑·改`（Fusion Sword EX，生成键 `nuclear_pri_ex`，攻击 11，耐久 450，资料 ID `984248`）：https://www.mcmod.cn/item/984248.html；`超重剑·冲锋`（Vanguard，生成键 `vanguard`，攻击 13，耐久 550，资料 ID `984249`）：https://www.mcmod.cn/item/984249.html；`超重剑·王蛇`（King Cobra，生成键 `king_cobra`，攻击 15，耐久 550，资料 ID `984250`）：https://www.mcmod.cn/item/984250.html。三把均使用 `kablade:kablade_honkai_named`，图标取自本地 `IconR` 导出，资料正文已链接相关 SA 与前置拔刀。
- 2026-08-24：补充并直接发布本地缺失的 SA：`核能震动`（Nuclear Shock，资料 ID `984246`）：https://www.mcmod.cn/item/984246.html；`女武神冲击`（Valkyrie Impact，资料 ID `984247`）：https://www.mcmod.cn/item/984247.html。两条 SA 正文已反向链接对应拔刀页。
- 2026-08-24：按用户要求修订上述两条 SA 正文，移除“对数”公式描述，改为对应拔刀剑的实际伤害：`核能震动`使用融核动力剑·改约 40 点；`女武神冲击`使用超重剑·冲锋时落点冲击约 41.18 点、地裂波约 31.30 点。公开页已核验无“对数”字样且链接仍有效。
- 2026-08-24：为三把新拔刀直接发布工作台有序合成表：`融核动力剑·改` 合成表 ID `337372`（https://www.mcmod.cn/item/tab/edit/337372/），`超重剑·冲锋` 合成表 ID `337373`（https://www.mcmod.cn/item/tab/edit/337373/），`超重剑·王蛇` 合成表 ID `337374`（https://www.mcmod.cn/item/tab/edit/337374/）。公开页逐张回读，输出、槽位、材料 ID 和数量均与本地配方一致。
