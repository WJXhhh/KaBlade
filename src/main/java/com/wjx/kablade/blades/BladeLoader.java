package com.wjx.kablade.blades;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.honkai.ByoDen;
import com.wjx.kablade.blades.honkai.ByoRai;
import com.wjx.kablade.blades.honkai.CrystalCutter;
import com.wjx.kablade.blades.honkai.DawnBreaker;
import com.wjx.kablade.blades.honkai.FairySword;
import com.wjx.kablade.blades.honkai.FloridSakura;
import com.wjx.kablade.blades.honkai.FrozenNaraka;
import com.wjx.kablade.blades.honkai.FutsunushiTo;
import com.wjx.kablade.blades.honkai.FuheLiuye;
import com.wjx.kablade.blades.honkai.FuheZhuque;
import com.wjx.kablade.blades.honkai.GalacticNova;
import com.wjx.kablade.blades.honkai.IceEpiphyllum;
import com.wjx.kablade.blades.honkai.KeyOfCastigation;
import com.wjx.kablade.blades.honkai.KeyOfLimpidity;
import com.wjx.kablade.blades.honkai.MagStorm;
import com.wjx.kablade.blades.honkai.MagTyphoon;
import com.wjx.kablade.blades.honkai.MuraHori;
import com.wjx.kablade.blades.honkai.MuraSeshu;
import com.wjx.kablade.blades.honkai.MuraUson;
import com.wjx.kablade.blades.honkai.MuraYoto;
import com.wjx.kablade.blades.honkai.Nue;
import com.wjx.kablade.blades.honkai.OneSaltyTuna;
import com.wjx.kablade.blades.honkai.Osahoko;
import com.wjx.kablade.blades.honkai.PledgeOfRain;
import com.wjx.kablade.blades.honkai.Phoenix;
import com.wjx.kablade.blades.honkai.PlasmaKagehide;
import com.wjx.kablade.blades.honkai.PulseKatanaType17;
import com.wjx.kablade.blades.honkai.PulseKatanaType19;
import com.wjx.kablade.blades.honkai.Raikiri;
import com.wjx.kablade.blades.honkai.RuinousSakura;
import com.wjx.kablade.blades.honkai.SakuraBlossom;
import com.wjx.kablade.blades.honkai.SkyBreaker;
import com.wjx.kablade.blades.honkai.ThermalCutter;
import com.wjx.kablade.blades.honkai.ThirdSacredRelic;
import com.wjx.kablade.blades.honkai.VibroCutter;
import com.wjx.kablade.blades.honkai.VorpalSword;
import com.wjx.kablade.blades.honkai.XuanYuanKatana;
import com.wjx.kablade.blades.allweapon.AwBailan;
import com.wjx.kablade.blades.allweapon.AwBaiQiYue;
import com.wjx.kablade.blades.allweapon.AwBaiShouJianWang;
import com.wjx.kablade.blades.allweapon.AwBaiYueDing;
import com.wjx.kablade.blades.allweapon.AwChanShiZhe;
import com.wjx.kablade.blades.allweapon.AwFengShen;
import com.wjx.kablade.blades.allweapon.AwFengZhiYing;
import com.wjx.kablade.blades.allweapon.AwGuangJian;
import com.wjx.kablade.blades.allweapon.AwHtkg;
import com.wjx.kablade.blades.allweapon.AwHuami;
import com.wjx.kablade.blades.allweapon.AwJie;
import com.wjx.kablade.blades.allweapon.AwJiyi;
import com.wjx.kablade.blades.allweapon.AwLiuRRHuo;
import com.wjx.kablade.blades.allweapon.AwLvluo;
import com.wjx.kablade.blades.allweapon.AwShangguYizhi;
import com.wjx.kablade.blades.allweapon.AwShangguYujin;
import com.wjx.kablade.blades.allweapon.AwXuezou;
import com.wjx.kablade.blades.allweapon.AwYingyue;
import com.wjx.kablade.blades.allweapon.AwYouming;
import com.wjx.kablade.blades.allweapon.AwZhanYue;
import com.wjx.kablade.blades.ordinary.ArcLight;
import com.wjx.kablade.blades.ordinary.AuroraBlade;
import com.wjx.kablade.blades.ordinary.BambooBattler;
import com.wjx.kablade.blades.ordinary.BambooIron;
import com.wjx.kablade.blades.ordinary.BambooLumi;
import com.wjx.kablade.blades.ordinary.CutIron;
import com.wjx.kablade.blades.ordinary.NotedVine;
import com.wjx.kablade.blades.ordinary.Originyer;
import com.wjx.kablade.blades.ordinary.RimmedEarth;
import com.wjx.kablade.blades.ordinary.RockyAnshan;
import com.wjx.kablade.blades.ordinary.RockyEX;
import com.wjx.kablade.blades.ordinary.RockyHuagang;
import com.wjx.kablade.blades.ordinary.RockyShanchang;
import com.wjx.kablade.blades.splight.SL_Blackwatch;
import com.wjx.kablade.blades.splight.SL_Initial;
import com.wjx.kablade.blades.splight.SL_Normal;
import com.wjx.kablade.blades.splight.SL_Origin;
import com.wjx.kablade.blades.splight.SL_Senta;
import com.wjx.kablade.blades.splight.SL_Young1;
import com.wjx.kablade.blades.splight.SL_Young2;
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
    public static BladeDefineBase BAMBOO_BATTLER;
    public static BladeDefineBase BAMBOO_LUMI;
    public static BladeDefineBase ARC_LIGHT;
    public static BladeDefineBase AURORA_BLADE;
    public static BladeDefineBase CUT_IRON;
    public static BladeDefineBase ORIGINYER;
    // 崩坏线·村正系列
    public static BladeDefineBase MURA_SESHU;
    public static BladeDefineBase MURA_HORI;
    public static BladeDefineBase MURA_USON;
    public static BladeDefineBase MURA_YOTO;
    public static BladeDefineBase ONE_SALTY_TUNA;
    // 崩坏线·苗刀系列（雷妖/电魂，由村正 Lv3 升级而来）
    public static BladeDefineBase BYO_RAI;
    public static BladeDefineBase BYO_DEN;
    // 崩坏线·复合系列（柳叶/朱雀，由势州村正分出）
    public static BladeDefineBase FUHE_LIUYE;
    public static BladeDefineBase FUHE_ZHUQUE;
    // 崩坏线·复合系列 Lv2/Lv3（柳叶→结晶逆刃刀，朱雀→热能切割刃→凰剑，结晶逆刃刀→等离子影秀）
    public static BladeDefineBase CRYSTAL_CUTTER;
    public static BladeDefineBase THERMAL_CUTTER;
    public static BladeDefineBase PLASMA_KAGEHIDE;
    public static BladeDefineBase PHOENIX;
    // 崩坏线·脉冲太刀系列（村正分支，T17/T19，T17 是等离子影秀的合成前置之一）
    public static BladeDefineBase PULSE_KATANA_T17;
    public static BladeDefineBase PULSE_KATANA_T19;
    // 崩坏线·银河新星线（T17 + T19 + 铬钼钢剑 → 银河追光 → 反力场打刀11式）
    public static BladeDefineBase GALACTIC_NOVA;
    public static BladeDefineBase VORPAL_SWORD;
    public static BladeDefineBase DAWN_BREAKER;
    public static BladeDefineBase THIRD_SACRED;
    public static BladeDefineBase SAKURA_BLOSSOM;
    public static BladeDefineBase FLORID_SAKURA;
    public static BladeDefineBase RUINOUS_SAKURA;
    public static BladeDefineBase PLEDGE_OF_RAIN;
    public static BladeDefineBase NUE;
    // 崩坏线·脉冲太刀分支
    public static BladeDefineBase XUANYUAN_KATANA;
    // 崩坏线·苗刀系列 Lv5/分支
    public static BladeDefineBase RAIKIRI;
    public static BladeDefineBase OSAHOKO;
    public static BladeDefineBase SKY_BREAKER;
    public static BladeDefineBase FUTSUNUSHI_TO;
    public static BladeDefineBase VIBRO_CUTTER;
    public static BladeDefineBase MAG_STORM;
    public static BladeDefineBase MAG_TYPHOON;
    public static BladeDefineBase KEY_OF_CASTIGATION;
    public static BladeDefineBase KEY_OF_LIMPIDITY;
    public static BladeDefineBase ICE_EPIPHYLLUM;
    public static BladeDefineBase FROZEN_NARAKA;
    // 崩坏线·妖精剑
    public static BladeDefineBase FAIRY_SWORD;
    // 万物皆刃线（首批 5 把招牌刀，世界合成获取）
    public static BladeDefineBase AW_LIURRH;
    public static BladeDefineBase AW_CHANSHIZHE;
    public static BladeDefineBase AW_GUANGJIAN;
    public static BladeDefineBase AW_FENGSHEN;
    public static BladeDefineBase AW_ZHANYUE;
    // 万物皆刃线（第一批扩展 6 把）
    public static BladeDefineBase AW_LVLUO;
    public static BladeDefineBase AW_XUEZOU;
    public static BladeDefineBase AW_JIE;
    public static BladeDefineBase AW_HTKG;
    public static BladeDefineBase AW_BAILAN;
    public static BladeDefineBase AW_YINGYUE;
    public static BladeDefineBase AW_YOUMING;
    public static BladeDefineBase AW_JIYI;
    public static BladeDefineBase AW_HUAMI;
    public static BladeDefineBase AW_FENGZHIYING;
    public static BladeDefineBase AW_BAIYUEDING;
    public static BladeDefineBase AW_BAIQIYUE;
    public static BladeDefineBase AW_BAISHOUJIANWANG;
    public static BladeDefineBase AW_SHANGGUYUJIN;
    public static BladeDefineBase AW_SHANGGUYIZHI;
    // 龙一文字线
    public static BladeDefineBase SPLIGHT_INITIAL;
    public static BladeDefineBase SPLIGHT_NORMAL;
    public static BladeDefineBase SPLIGHT_YOUNG1;
    public static BladeDefineBase SPLIGHT_YOUNG2;
    public static BladeDefineBase SPLIGHT_ORIGIN;
    public static BladeDefineBase SPLIGHT_BLACKWATCH;
    public static BladeDefineBase SPLIGHT_SENTA;

    /**
     * 创造模式物品栏的显示顺序（按合成链路排列）。
     * 新刀加进来时在对应位置插入其 getKey() 返回值即可。
     *
     * 排序策略：每个 tab 独立排序——只取当前 tab 载体对应的条目，
     * 按本列表出现顺序排列，不在列表中的刀排在末尾。
     */
    private static final List<String> BLADE_ORDER = List.of(
            // 崩坏线·起点：势州村正（所有村正分支的根）
            "muraseshu",
            // 岩石线：夯土 → 矍铄 → 嶙峋 → 明磬 → 千岩之锋
            "hangtu",
            "rocky_anshan",
            "rocky_huagang",
            "rocky_shanchang",
            "rocky_ex",
            // 自然线：夯土 → 青藤
            "noted_vine",
            // 竹线：铁刃竹光 → 战刃竹光 → 流萤竹光 → 弧光刃流芒
            "bamboo_iron",
            "bamboo_battler",
            "bamboo_lumi",
            "arc_light",
            // 极光线：映天
            "aurora_blade",
            // 断铁线：斩铁 → 碎钢
            "cut_iron",
            "originyer",
            // 崩坏线·村正系列：势州村正 → 堀川国广 → 妖刀雨村 / 妖刀村正
            "murahori",
            "murauson",
            "murayoto",
            "tuna",
            // 崩坏线·村正分支 → 复合系列（柳叶/朱雀）
            "fuheliuye",
            "fuhezhuque",
            // 复合系列 Lv2：结晶逆刃刀 / 热能切割刃
            "crystal_cutter",
            "thermal_cutter",
            // 复合系列 Lv3：等离子影秀 / 凰剑
            "plasma_kagehide",
            "phoenix",
            // 崩坏线·脉冲太刀系列：T17/T19 → 轩辕
            "pulse_katana_t17",
            "pulse_katana_t19",
            "xuanyuan_katana",
            // 崩坏线·银河新星线：银河追光 → 反力场打刀11式 / 破晓者 / 妖精剑 / 3rd圣遗物 / 影鵺
            "galactic",
            "vorpal_sword",
            "dawn_breaker",
            "sakura_blossom",
            "fairy_sword",
            "third_sacred",
            "florid_sakura",
            "ruinous_sakura",
            "pledge_of_rain",
            "nue",
            // 崩坏线·苗刀系列：雷妖/电魂 → 雷切/藏锋/开天剑/高周波切割刀
            "byorai",
            "byoden",
            "raikiri",
            "osahoko",
            "sky_breaker",
            "futsunushi_to",
            "vibro_cutter",
            "mag_storm",
            "mag_typhoon",
            "key_of_castigation",
            "key_of_limpidity",
            "ice_epiphyllum",
            "frozen_naraka",
            // 龙一文字线
            "splight_initial",
            "splight_normal",
            "splight_young1",
            "splight_young2",
            "splight_origin",
            "splight_blackwatch",
            "splight_senta",
            // 万物皆刃线
            "liurrh",
            "chanshizhe",
            "guangjian",
            "fengshen",
            "zhanyue",
            "lvluo",
            "xuezou",
            "jie",
            "htkg",
            "bailan",
            "yingyue",
            "youming",
            "jiyi",
            "huami",
            "fengzhiying",
            "baiyueding",
            "baiqiyue",
            "baishoujianwang",
            "shangguyujin",
            "shangguyizhi"
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
        // 竹系列 Lv2：铁刃「竹光」+ 铁锭 / 萤石粉
        BAMBOO_BATTLER = new BambooBattler(context);
        BAMBOO_LUMI = new BambooLumi(context);
        // 竹系列 Lv3：流萤「竹光」+ 萤石粉 + 铁块 → 弧光刃「流芒」
        ARC_LIGHT = new ArcLight(context);
        // 极光线终极：弧光刃「流芒」+ 钻石 + 极光金属剑 → 极光刃「映天」
        AURORA_BLADE = new AuroraBlade(context);
        // 断铁：战刃「竹光」+ 铬锭 + 钻石
        CUT_IRON = new CutIron(context);
        // 源能刃「碎钢」：斩铁 + 重力微粒 + 钻石 + 红石块
        ORIGINYER = new Originyer(context);
        // 崩坏线·村正系列
        MURA_SESHU = new MuraSeshu(context);
        MURA_HORI = new MuraHori(context);
        MURA_USON = new MuraUson(context);
        MURA_YOTO = new MuraYoto(context);
        ONE_SALTY_TUNA = new OneSaltyTuna(context);
        // 崩坏线·脉冲太刀系列
        PULSE_KATANA_T17 = new PulseKatanaType17(context);
        PULSE_KATANA_T19 = new PulseKatanaType19(context);
        XUANYUAN_KATANA = new XuanYuanKatana(context);
        // 崩坏线·银河新星线
        GALACTIC_NOVA = new GalacticNova(context);
        VORPAL_SWORD = new VorpalSword(context);
        DAWN_BREAKER = new DawnBreaker(context);
        SAKURA_BLOSSOM = new SakuraBlossom(context);
        FLORID_SAKURA = new FloridSakura(context);
        RUINOUS_SAKURA = new RuinousSakura(context);
        THIRD_SACRED = new ThirdSacredRelic(context);
        PLEDGE_OF_RAIN = new PledgeOfRain(context);
        NUE = new Nue(context);
        // 崩坏线·苗刀系列
        BYO_RAI = new ByoRai(context);
        BYO_DEN = new ByoDen(context);
        RAIKIRI = new Raikiri(context);
        OSAHOKO = new Osahoko(context);
        SKY_BREAKER = new SkyBreaker(context);
        FUTSUNUSHI_TO = new FutsunushiTo(context);
        VIBRO_CUTTER = new VibroCutter(context);
        MAG_STORM = new MagStorm(context);
        MAG_TYPHOON = new MagTyphoon(context);
        KEY_OF_CASTIGATION = new KeyOfCastigation(context);
        KEY_OF_LIMPIDITY = new KeyOfLimpidity(context);
        ICE_EPIPHYLLUM = new IceEpiphyllum(context);
        FROZEN_NARAKA = new FrozenNaraka(context);
        // 崩坏线·妖精剑
        FAIRY_SWORD = new FairySword(context);
        // 崩坏线·复合系列
        FUHE_LIUYE = new FuheLiuye(context);
        FUHE_ZHUQUE = new FuheZhuque(context);
        CRYSTAL_CUTTER = new CrystalCutter(context);
        THERMAL_CUTTER = new ThermalCutter(context);
        PLASMA_KAGEHIDE = new PlasmaKagehide(context);
        PHOENIX = new Phoenix(context);
        // 万物皆刃线（首批 5 把招牌刀）
        AW_LIURRH = new AwLiuRRHuo(context);
        AW_CHANSHIZHE = new AwChanShiZhe(context);
        AW_GUANGJIAN = new AwGuangJian(context);
        AW_FENGSHEN = new AwFengShen(context);
        AW_ZHANYUE = new AwZhanYue(context);
        // 万物皆刃线（第一批扩展 6 把）
        AW_LVLUO = new AwLvluo(context);
        AW_XUEZOU = new AwXuezou(context);
        AW_JIE = new AwJie(context);
        AW_HTKG = new AwHtkg(context);
        AW_BAILAN = new AwBailan(context);
        AW_YINGYUE = new AwYingyue(context);
        AW_YOUMING = new AwYouming(context);
        AW_JIYI = new AwJiyi(context);
        AW_HUAMI = new AwHuami(context);
        AW_FENGZHIYING = new AwFengZhiYing(context);
        AW_BAIYUEDING = new AwBaiYueDing(context);
        AW_BAIQIYUE = new AwBaiQiYue(context);
        AW_BAISHOUJIANWANG = new AwBaiShouJianWang(context);
        AW_SHANGGUYUJIN = new AwShangguYujin(context);
        AW_SHANGGUYIZHI = new AwShangguYizhi(context);
        // 龙一文字线
        SPLIGHT_INITIAL = new SL_Initial(context);
        SPLIGHT_NORMAL = new SL_Normal(context);
        SPLIGHT_YOUNG1 = new SL_Young1(context);
        SPLIGHT_YOUNG2 = new SL_Young2(context);
        SPLIGHT_ORIGIN = new SL_Origin(context);
        SPLIGHT_BLACKWATCH = new SL_Blackwatch(context);
        SPLIGHT_SENTA = new SL_Senta(context);
    }

    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                       CreativeModeTab.Output output) {
        fillBladeTab(parameters, output, ModItems.KABLADE_BLADE.getId());
    }

    /** 崩坏线拔刀剑专用创造分页：只列出载体为 kablade_honkai_named 的刀。 */
    public static void fillCreativeTabHonkai(CreativeModeTab.ItemDisplayParameters parameters,
                                             CreativeModeTab.Output output) {
        fillBladeTab(parameters, output, ModItems.KABLADE_HONKAI_BLADE.getId());
    }

    /** 龙一文字线拔刀剑专用创造分页：只列出载体为 kablade_sl_named 的刀。 */
    public static void fillCreativeTabSPLight(CreativeModeTab.ItemDisplayParameters parameters,
                                              CreativeModeTab.Output output) {
        fillBladeTab(parameters, output, ModItems.KABLADE_SL_BLADE.getId());
    }

    /** 万物皆刃线拔刀剑专用创造分页：只列出载体为 kablade_aw_named 的刀。 */
    public static void fillCreativeTabAllWeapon(CreativeModeTab.ItemDisplayParameters parameters,
                                                CreativeModeTab.Output output) {
        fillBladeTab(parameters, output, ModItems.KABLADE_AW_BLADE.getId());
    }

    private static void fillBladeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                     CreativeModeTab.Output output,
                                     net.minecraft.resources.ResourceLocation carrierItemId) {
        if (carrierItemId == null) {
            return;
        }
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders());
        definitions.listElements()
                .filter(definition -> carrierItemId.equals(definition.value().getItemName()))
                .sorted(Comparator.comparingInt(ref -> {
                    String path = ref.value().getName().getPath();
                    int idx = BLADE_ORDER.indexOf(path);
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .map(Holder.Reference::value)
                .map(SlashBladeDefinition::getBlade)
                .filter(stack -> !stack.isEmpty())
                .forEach(output::accept);
    }
}
