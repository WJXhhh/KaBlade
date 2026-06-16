package com.wjx.kablade.compat.jei;

import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.util.ResourceUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import mods.flammpfeil.slashblade.compat.jei.JEICompat;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 让 JEI 按「刀名」区分本模组的命名刀。
 *
 * <p>拔刀剑的所有命名刀共用同一个载体物品（我们的是 {@code kablade:kablade_blade_named}）。JEI 默认
 * 按物品 id 归并同一物品，于是不同刀的合成表会挤在同一个条目里，预览还会渲染成默认外观（之前看到的
 * 夯土刀串色就是这个原因）。SlashBlade 自带的 {@code JEICompat} 只为它自家的 {@code slashblade:slashblade}
 * 注册了 subtype 解释器，不覆盖我们的载体——这正是 NegoreRouse（用 slashblade 原生载体）正常、而我们
 * 不正常的根因。
 *
 * <p>这里为我们的载体注册同样的解释器，直接复用 SlashBlade 的 {@link JEICompat#syncSlashBlade}
 * （UID = 刀名）。JEI 拿到正确区分的堆叠后：不同刀的合成表各自分开，预览也由 {@code BladeModel} 按刀名
 * 解析出正确的模型/贴图/颜色。
 *
 * <p>{@code @JeiPlugin} 由 JEI 自动发现，仅在 JEI 存在时加载，所以对 JEICompat / JEI API 的引用是安全的。
 */
@JeiPlugin
public class KbladeJeiPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceUtil.getLocation("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // registerItemSubtypes 在注册表冻结后由 JEI 调用，此处 .get() 安全。
        registration.registerSubtypeInterpreter(ModItems.KABLADE_BLADE.get(), JEICompat::syncSlashBlade);
    }
}
