package com.wjx.kablade.SlashBlade;

import com.wjx.kablade.AllWeapon.blade.specialattack.*;
import com.wjx.kablade.ExSA.special_attack.AquaEdgeEx;
import com.wjx.kablade.ExSA.special_attack.LaveDriveEx;
import com.wjx.kablade.ExSA.special_attack.LightningSwordsEx;
import com.wjx.kablade.ExSA.special_attack.OverSlash;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.SpeacialEffects.*;
import com.wjx.kablade.SlashBlade.SpeacialEffects.Kirisaya.BurstDrive;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SPLight.SPLighting;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.SlashBlade.specialattack.*;
import com.wjx.kablade.SlashBlade.specialattack.p2.HonkaiLoveIsWar;
import com.wjx.kablade.config.ModConfig;
import com.wjx.kablade.proxy.CommonProxy;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;

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
    public static ISpecialEffect BurstDrive = SpecialEffects.register(new BurstDrive());
    public static ISpecialEffect UndyingSaltiness = SpecialEffects.register(new SEUndyingSaltiness());

    //SPLight
    public static ISpecialEffect SPLighting = SpecialEffects.register(new SPLighting());


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
            if (ModConfig.GeneralConf.Enable_New_SA_Id){
                ItemSlashBlade.specialAttacks.put(451,new HonkaiWindEnchantment());
                ItemSlashBlade.specialAttacks.put(452,new HonKaiZaizan());
                ItemSlashBlade.specialAttacks.put(453,new HonkaiFallingPetals());
                ItemSlashBlade.specialAttacks.put(454,new HonkaiThunderEdge());
                ItemSlashBlade.specialAttacks.put(455,new HonkaiCrimsonSakura());

            }
            else {
                ItemSlashBlade.specialAttacks.put(301,new HonkaiWindEnchantment());
                ItemSlashBlade.specialAttacks.put(302,new HonKaiZaizan());
                ItemSlashBlade.specialAttacks.put(303,new HonkaiFallingPetals());
                ItemSlashBlade.specialAttacks.put(304,new HonkaiThunderEdge());
                ItemSlashBlade.specialAttacks.put(305,new HonkaiCrimsonSakura());

            }
            ItemSlashBlade.specialAttacks.put(456,new HonkaiLethalThrust());
            ItemSlashBlade.specialAttacks.put(457,new HonkaiLoveIsWar());

            if(EnableAllWeapon){
                ItemSlashBlade.specialAttacks.put(400,new AL_YanjiFZ());
                ItemSlashBlade.specialAttacks.put(401,new AL_Yuqi());
                ItemSlashBlade.specialAttacks.put(402,new AL_Xingbao());
                ItemSlashBlade.specialAttacks.put(403,new AL_WeiZhan());
                ItemSlashBlade.specialAttacks.put(404,new AL_Liedi());
                ItemSlashBlade.specialAttacks.put(405,new AL_Fengxuan());
                ItemSlashBlade.specialAttacks.put(406,new AL_Qicai());
                ItemSlashBlade.specialAttacks.put(407,new AL_Huanyingdie());
                ItemSlashBlade.specialAttacks.put(408,new AL_Xuepo());
                ItemSlashBlade.specialAttacks.put(409,new AL_Yueyatianchong());
                ItemSlashBlade.specialAttacks.put(410,new AL_Zhuixing());
                ItemSlashBlade.specialAttacks.put(411,new AL_RandomSA());
                ItemSlashBlade.specialAttacks.put(412,new AL_HuanyingdieS());
            }
            //AW

        }
        ItemSlashBlade.specialAttacks.put(288,new RockHit_I());
        ItemSlashBlade.specialAttacks.put(289,new SaBreakTheDawn());
        ItemSlashBlade.specialAttacks.put(290,new SaCutMetal());

        ItemSlashBlade.specialAttacks.put(295,new SaAuroraShining());
        ItemSlashBlade.specialAttacks.put(296,new SaWineBind());
        ItemSlashBlade.specialAttacks.put(297,new SaDomainSuppression());


        ItemSlashBlade.specialAttacks.put(350,new OverSlash());
        ItemSlashBlade.specialAttacks.put(351,new LightningSwordsEx());
        ItemSlashBlade.specialAttacks.put(352,new LaveDriveEx());
        ItemSlashBlade.specialAttacks.put(353,new AquaEdgeEx());

    }
    public static void ClientLoader(){
        new ItemSlashUtil();
    }
}
