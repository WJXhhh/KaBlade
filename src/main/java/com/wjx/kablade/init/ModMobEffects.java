package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.mobeffect.ConfinementMobEffect;
import com.wjx.kablade.mobeffect.FreezeMobEffect;
import com.wjx.kablade.mobeffect.ParalysisMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * KBlade2 的自定义状态效果注册中心。
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Main.MODID);

    /** 麻痹 —— 1.12.2 PotionParaly 的 1.20.1 等价效果，大幅降低移动速度。 */
    public static final RegistryObject<MobEffect> PARALYSIS = REGISTRY.register(
            "paralysis", ParalysisMobEffect::new);

    public static final RegistryObject<MobEffect> FREEZE = REGISTRY.register(
            "freeze", FreezeMobEffect::new);

    /** 禁锢 —— 「高频坍缩」力场的伤害放大标记。 */
    public static final RegistryObject<MobEffect> CONFINEMENT = REGISTRY.register(
            "confinement", ConfinementMobEffect::new);

    private ModMobEffects() {
    }
}
