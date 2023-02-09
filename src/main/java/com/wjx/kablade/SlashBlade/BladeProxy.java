package com.wjx.kablade.SlashBlade;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SEDivinePenalty;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SEPhoenix;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SETurbulence;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.SlashBlade.specialattack.*;
import com.wjx.kablade.proxy.CommonProxy;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;

import static mods.flammpfeil.slashblade.SlashBlade.InitEventBus;

public class BladeProxy {
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
            SpecialEffects.register(new SETurbulence());
            SpecialEffects.register(new SEDivinePenalty());
            SpecialEffects.register(new SEPhoenix());
        }
        ItemSlashBlade.specialAttacks.put(288,new RockHit_I());
        ItemSlashBlade.specialAttacks.put(289,new SaBreakTheDawn());
        ItemSlashBlade.specialAttacks.put(290,new SaCutMetal());

        ItemSlashBlade.specialAttacks.put(294,new SaAuroraShining());
        ItemSlashBlade.specialAttacks.put(295,new SaWineBind());
    }
    public static void ClientLoader(){
        new ItemSlashUtil();
    }
}
