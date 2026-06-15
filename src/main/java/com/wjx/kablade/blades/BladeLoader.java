package com.wjx.kablade.blades;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.ordinary.BambooIron;
import com.wjx.kablade.blades.ordinary.NotedVine;
import com.wjx.kablade.blades.ordinary.RimmedEarth;
import com.wjx.kablade.blades.ordinary.RockyAnshan;
import com.wjx.kablade.blades.ordinary.RockyEX;
import com.wjx.kablade.blades.ordinary.RockyHuagang;
import com.wjx.kablade.blades.ordinary.RockyShanchang;
import com.wjx.kablade.init.ModItems;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Comparator;
import java.util.List;

/** Registers named blades that all use kablade:kablade_blade as their carrier item. */
public final class BladeLoader {
    public static BladeDefineBase RIMMED_EARTH;
    public static BladeDefineBase ROCKY_ANSHAN;
    public static BladeDefineBase ROCKY_HUAGANG;
    public static BladeDefineBase ROCKY_SHANCHANG;
    public static BladeDefineBase NOTED_VINE;
    public static BladeDefineBase BAMBOO_IRON;
    public static BladeDefineBase ROCKY_EX;

    /**
     * 创造模式物品栏的显示顺序（按合成链路排列）。
     * 新刀加进来时在对应位置插入其 getKey() 返回值即可。
     */
    private static final List<String> BLADE_ORDER = List.of(
            "hangtu",              // 基础：无铭「夯土」
            "rocky_anshan",        // 岩石Lv1：自铭「矍铄」
            "rocky_huagang",       // 岩石Lv1：自铭「嶙峋」
            "rocky_shanchang",     // 岩石Lv1：自铭「明磬」
            "noted_vine",          // 自然Lv1：铭刀「青藤」
            "bamboo_iron",         // 竹Lv1：铁刃「竹光」
            "rocky_ex"             // 岩石Lv2：千岩之锋
    );

    public static void bootstrap(BootstapContext<SlashBladeDefinition> context) {
        RIMMED_EARTH = new RimmedEarth(context);
        // 岩石线 Lv1：夯土刀 + 安山岩/花岗岩/闪长岩
        ROCKY_ANSHAN = new RockyAnshan(context);
        ROCKY_HUAGANG = new RockyHuagang(context);
        ROCKY_SHANCHANG = new RockyShanchang(context);
        // 自然线 Lv1：夯土刀 + 藤蔓 + 小麦种子
        NOTED_VINE = new NotedVine(context);
        // 竹系列 Lv1：原版竹光刀 + 铁锭
        BAMBOO_IRON = new BambooIron(context);
        // 岩石线 Lv2：三把岩石 Lv1 刀 + 铁块融合
        ROCKY_EX = new RockyEX(context);
    }

    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                       CreativeModeTab.Output output) {
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders());
        definitions.listElements()
                .sorted(Comparator.comparingInt(ref -> {
                    String path = ref.value().getName().getPath();
                    int idx = BLADE_ORDER.indexOf(path);
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .map(Holder.Reference::value)
                .filter(definition -> ModItems.KABLADE_BLADE.getId() != null
                        && ModItems.KABLADE_BLADE.getId().equals(definition.getItemName()))
                .map(SlashBladeDefinition::getBlade)
                .filter(stack -> !stack.isEmpty())
                .forEach(output::accept);
    }
}
