package com.wjx.kablade.SlashBlade;

import com.wjx.kablade.AllWeapon.blade.specialattack.AL_Xingbao;
import com.wjx.kablade.AllWeapon.blade.specialattack.AL_YanjiFZ;
import com.wjx.kablade.AllWeapon.blade.specialattack.AL_Yuqi;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.SpeacialEffects.*;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.SlashBlade.specialattack.*;
import com.wjx.kablade.proxy.CommonProxy;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.item.Item;

import static com.wjx.kablade.Main.EnableAllWeapon;
import static mods.flammpfeil.slashblade.SlashBlade.InitEventBus;

public class BladeProxy {
    public static ISpecialEffect DivinePenalty = SpecialEffects.register(new SEDivinePenalty());
    public static ISpecialEffect Phoenix = SpecialEffects.register(new SEPhoenix());
    public static ISpecialEffect Turbulence = SpecialEffects.register(new SETurbulence());
    public static ISpecialEffect Oripursuit = SpecialEffects.register(new SEOripursuit());
    public static ISpecialEffect EMInduction = SpecialEffects.register(new SEEMInduction());
    public static ISpecialEffect GlacialBane = SpecialEffects.register(new SEGlacialBane());
    public static ISpecialEffect RagingIzumo = SpecialEffects.register(new SERagingIzumo());
    public static ISpecialEffect PowerOfWind = SpecialEffects.register(new SEPowerOfWind());
    public static ISpecialEffect ThunderBlitz = SpecialEffects.register(new SEThunderBlitz());


    public static void CommonLoader(CommonProxy proxy){
        InitEventBus.register(proxy);
        InitEventBus.register(Main.instance);
        new BladeLoader();
        if(Main.GALoaded)
        {
            ItemSlashBlade.specialAttacks.put(280, new Qi());
            ItemSlashBlade.specialAttacks.put(281, new HonkaiLacerateBlade());
            ItemSlashBlade.specialAttacks.put(282, new HonkaiDizuiSA());
            ItemSlashBlade.specialAttacks.put(283, new HonKaiFireOfSin());
            ItemSlashBlade.specialAttacks.put(284, new HonKaiChopWillow());
            ItemSlashBlade.specialAttacks.put(285, new HonKaiFrostBlade());
            ItemSlashBlade.specialAttacks.put(286, new HonKaiMoltenBlade());
            ItemSlashBlade.specialAttacks.put(287, new HonKaiFrostComet());
            ItemSlashBlade.specialAttacks.put(291,new HonKaiVorpalHole());
            ItemSlashBlade.specialAttacks.put(292,new HonkaiBladeWard());
            ItemSlashBlade.specialAttacks.put(293,new HonkaiAbsoluteZero());
            ItemSlashBlade.specialAttacks.put(294,new HonkaiShockImpact());
            ItemSlashBlade.specialAttacks.put(298,new HonkaiMagChaosBlade());
            ItemSlashBlade.specialAttacks.put(299,new HonkaiSnowDance());
            ItemSlashBlade.specialAttacks.put(300,new HonkaiKamiOfWar());
            ItemSlashBlade.specialAttacks.put(301,new HonkaiWindEnchantment());
            ItemSlashBlade.specialAttacks.put(302,new HonKaiZaizan());
            ItemSlashBlade.specialAttacks.put(303,new HonkaiFallingPetals());
            ItemSlashBlade.specialAttacks.put(304,new HonkaiThunderEdge());

            if(EnableAllWeapon){
                ItemSlashBlade.specialAttacks.put(400,new AL_YanjiFZ());
                ItemSlashBlade.specialAttacks.put(401,new AL_Yuqi());
                ItemSlashBlade.specialAttacks.put(402,new AL_Xingbao());
            }
            //AW

        }
        ItemSlashBlade.specialAttacks.put(288,new RockHit_I());
        ItemSlashBlade.specialAttacks.put(289,new SaBreakTheDawn());
        ItemSlashBlade.specialAttacks.put(290,new SaCutMetal());

        ItemSlashBlade.specialAttacks.put(295,new SaAuroraShining());
        ItemSlashBlade.specialAttacks.put(296,new SaWineBind());
        ItemSlashBlade.specialAttacks.put(297,new SaDomainSuppression());
    }
    public static void ClientLoader(){
        new ItemSlashUtil();
    }
}
