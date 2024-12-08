package com.wjx.kablade.SlashBlade;

import com.google.common.collect.Lists;
import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.AllWeapon.blade.ordinary.*;
import com.wjx.kablade.SPLight.blade.items.Item_SLNamed;
import com.wjx.kablade.Main;
import com.wjx.kablade.SPLight.blade.ordinary.SL_Initial;
import com.wjx.kablade.SlashBlade.blades.*;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_Caijue;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import com.wjx.kablade.SlashBlade.blades.honkai.*;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.util.List;

import static com.wjx.kablade.Main.EnableAWDemo;
import static com.wjx.kablade.Main.EnableAllWeapon;
import static com.wjx.kablade.Main.EnableSPLight;

public class BladeLoader {

    public static List<String> NamedBlades = Lists.newArrayList();
    public static List<String> AwBlades = Lists.newArrayList();
    public static List<String> SLBlades = Lists.newArrayList();
    public static List<String> NamedGod = Lists.newArrayList();
    public static List<String> NamedHonkai = Lists.newArrayList();
    public static List<String> DIZUI = Lists.newArrayList();

    public static final Item ITEM_KABLADE_NAMED = new Item_KaNamed(Item.ToolMaterial.IRON, 1.0F, "kanamed").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES).setNoRepair();
    public static Item ITEM_MAGIC = null;

    public static Item ITEM_HONKAI_NAMED = null;
    public static Item ITEM_DIZUI= null;

    public static Item ITEM_AW=null;

    public static Item ITEM_SL = null;

    public boolean autu=false;


    private void RegistOther(){
        if(Loader.isModLoaded("the_golden_autumn")) {
            autu=true;
        }

    }
    public BladeLoader() {

        RegistOther();
        if(autu){
            ITEM_MAGIC = new MagicBlade(Item.ToolMaterial.IRON, 32768.0F, "magicslashblade").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES_GOD).setNoRepair();
            ITEM_HONKAI_NAMED=new Item_HonkaiNamed(Item.ToolMaterial.IRON, 1.0F, "honkainamed").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES_HONKAI).setNoRepair();
            ITEM_DIZUI= new Item_Caijue(Item.ToolMaterial.IRON, 1.0F, "honkaidizui").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES_HONKAI).setNoRepair();
            if(EnableAllWeapon)
                ITEM_AW=new Item_AwNamed(Item.ToolMaterial.DIAMOND,1.0F,"awnamed").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES_ALLWEAPON).setNoRepair();
            if(EnableSPLight)
                ITEM_SL = new Item_SLNamed(Item.ToolMaterial.DIAMOND,1.0F,"slnamed").setMaxDamage(Integer.MAX_VALUE-32768).setCreativeTab(Main.TABKABLADE_BLADES_SP_LIGHT).setNoRepair();
        }




        loadBlade();
    }

    public void loadBlade() {
      this.loadBlade(new hangtublade());
      this.loadBlade(new BambooIron());
      this.loadBlade(new BambooBattler());
      this.loadBlade(new BambooLumi());
      this.loadBlade(new RockyAnshan());
      this.loadBlade(new RockyHuagang());
      this.loadBlade(new RockyShanchang());
      this.loadBlade(new RockyEX());
      this.loadBlade(new ArcLight());
      this.loadBlade(new CutIron());
      this.loadBlade(new AuroraBlade());
      this.loadBlade(new NotedVine());
      this.loadBlade(new BlackSteel());
      this.loadBlade(new ForestShadow());
      this.loadBlade(new Originyer());
       // Main.logger.info("RegisterHONKAI:"+(autu));
    if(autu)
        {
            this.loadBlade(new MuraSeshu());
            this.loadBlade(new MuraHori());
            this.loadBlade(new MuraUson());
            this.loadBlade(new MuraYoto());

            this.loadBlade(new ByoRai());
            this.loadBlade(new ByoDen());

            loadBlade(new FuheLiuye());
            loadBlade(new FuheZhuque());

            loadBlade(new CaiJueDizui());

            loadBlade(new PulseKatanaType19());
            loadBlade(new PulseKatanaType17());
            loadBlade(new ThermalCutter());
            loadBlade(new CrystalCutter());

            loadBlade(new GalacticNova());
            loadBlade(new VorpalSword());

            loadBlade(new Raikiri());
            loadBlade(new PlasmaKagehide());
            loadBlade(new XuanYuanKatana());

            loadBlade(new Osahoko());
            loadBlade(new DawnBreaker());
            loadBlade(new SkyBreaker());
            loadBlade(new Phoenix());

            loadBlade(new ThirdSacredRelic());
            loadBlade(new MagStorm());
            loadBlade(new IceEpiphyllum());

            loadBlade(new FutsunushiTo());
            loadBlade(new FairySword());
            loadBlade(new Nue());

            loadBlade(new SakuraBlossom());

            loadBlade(new KeyOfCastigation());

            loadBlade(new FloridSakura());



            if(EnableAllWeapon){
                loadBlade(new AL_LiuRRHuo());
                loadBlade(new AL_Bailan());
                loadBlade(new AL_Lvluo());
                loadBlade(new AL_Chanshizhe());
                loadBlade(new AL_BaiQiYue());
                loadBlade(new AL_BaiYueding());
                loadBlade(new AL_Baishoujianwang());
                loadBlade(new AL_FengZhiYing());
                loadBlade(new AL_Guangjian());
                loadBlade(new AL_Huami());
                loadBlade(new AL_Jiyi());
                loadBlade(new AL_Xuezou());
                loadBlade(new AL_Youming());
                loadBlade(new AL_Zhanyue());
                loadBlade(new AL_Htkg());
                loadBlade(new AL_Jie());
                loadBlade(new AL_Fengshen());
                loadBlade(new AL_YingYue());
                if(EnableAWDemo){
                    loadBlade(new AL_Shangguyujin());
                    loadBlade(new AL_Shangguyizhi());
                }
            }

            if(EnableSPLight){
                loadBlade(new SL_Initial());
            }





            this.loadBlade(new MoDao());
        }


    }

    public void loadBlade(Object blade) {
        SlashBlade.InitEventBus.register(blade);
    }
}
