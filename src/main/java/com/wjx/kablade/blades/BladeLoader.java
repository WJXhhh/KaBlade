package com.wjx.kablade.blades;

import com.wjx.kablade.blades.base.BladeDefineBase;
import com.wjx.kablade.blades.honkai.ByoDen;
import com.wjx.kablade.blades.honkai.ByoRai;
import com.wjx.kablade.blades.honkai.CrystalCutter;
import com.wjx.kablade.blades.honkai.DawnBreaker;
import com.wjx.kablade.blades.honkai.FuheLiuye;
import com.wjx.kablade.blades.honkai.FuheZhuque;
import com.wjx.kablade.blades.honkai.GalacticNova;
import com.wjx.kablade.blades.honkai.MuraHori;
import com.wjx.kablade.blades.honkai.MuraSeshu;
import com.wjx.kablade.blades.honkai.MuraUson;
import com.wjx.kablade.blades.honkai.MuraYoto;
import com.wjx.kablade.blades.honkai.Nue;
import com.wjx.kablade.blades.honkai.Osahoko;
import com.wjx.kablade.blades.honkai.Phoenix;
import com.wjx.kablade.blades.honkai.PlasmaKagehide;
import com.wjx.kablade.blades.honkai.PulseKatanaType17;
import com.wjx.kablade.blades.honkai.PulseKatanaType19;
import com.wjx.kablade.blades.honkai.Raikiri;
import com.wjx.kablade.blades.honkai.SkyBreaker;
import com.wjx.kablade.blades.honkai.ThermalCutter;
import com.wjx.kablade.blades.honkai.ThirdSacredRelic;
import com.wjx.kablade.blades.honkai.VibroCutter;
import com.wjx.kablade.blades.honkai.VorpalSword;
import com.wjx.kablade.blades.honkai.XuanYuanKatana;
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
    public static BladeDefineBase NUE;
    // 崩坏线·脉冲太刀分支
    public static BladeDefineBase XUANYUAN_KATANA;
    // 崩坏线·苗刀系列 Lv5/分支
    public static BladeDefineBase RAIKIRI;
    public static BladeDefineBase OSAHOKO;
    public static BladeDefineBase SKY_BREAKER;
    public static BladeDefineBase VIBRO_CUTTER;

    /**
     * 创造模式物品栏的显示顺序（按合成链路排列）。
     * 新刀加进来时在对应位置插入其 getKey() 返回值即可。
     */
    private static final List<String> BLADE_ORDER = List.of(
            // 起点
            "hangtu",              // 基础：无铭「夯土」
            // 岩石线：矍铄 → 嶙峋 → 明磬 → 千岩之锋
            "rocky_anshan",        // 岩石Lv1：自铭「矍铄」
            "rocky_huagang",       // 岩石Lv1：自铭「嶙峋」
            "rocky_shanchang",     // 岩石Lv1：自铭「明磬」
            "rocky_ex",            // 岩石Lv2：千岩之锋
            // 自然线：夯土 → 青藤
            "noted_vine",          // 自然Lv1：铭刀「青藤」
            // 竹线：铁刃「竹光」→ 战刃「竹光」→ 流萤「竹光」→ 弧光刃「流芒」
            "bamboo_iron",         // 竹Lv1：铁刃「竹光」
            "bamboo_battler",      // 竹Lv2：战刃「竹光」
            "bamboo_lumi",         // 竹Lv2：流萤「竹光」
            "arc_light",           // 竹Lv3：弧光刃「流芒」
            // 极光线：映天
            "aurora_blade",        // 极光Lv1：极光刃「映天」
            // 断铁线：竹光战刃 → 断铁
            "cut_iron",            // 复合刃「斩铁」：由战刃「竹光」+ 铬锭 + 钻石合成
            "originyer",            // 源能刃「碎钢」：由斩铁 + 重力微粒 + 钻石 + 红石块合成
            // 崩坏线·村正系列：势州村正 → 堀川国广 → 妖刀雨村 / 妖刀村正
            "muraseshu",            // 村正Lv1：势州村正（钻石剑+铁块）
            "murahori",             // 村正Lv2：堀川国广（势州村正+红石块）
            "murauson",             // 村正Lv3：妖刀雨村（堀川国广+铁刃竹光+铁块）
            "murayoto",              // 村正Lv3：妖刀村正（势州村正+自铭嶙峋+钻石块）
            // 崩坏线·脉冲太刀系列：T17/T19（由村正 Lv3 双分支升级，T17 是等离子影秀前置）
            "pulse_katana_t17",      // 脉冲太刀17式（妖刀雨村+红石块+活塞，SA=寒霜灵刃）
            "pulse_katana_t19",      // 脉冲太刀19式（妖刀村正+红石块+活塞，无SA）
            "xuanyuan_katana",       // 轩辕·脉冲太刀（脉冲T17+铬锭+雪，SA=寒霜灵刃）
            // 崩坏线·银河新星线
            "galactic",              // 银河追光（脉冲T17+脉冲T19+铬钼钢剑，SA=樱花终结）
            "vorpal_sword",          // 反力场打刀11式（银河追光+钻石+重力结晶，SA=时空黑洞）
            "dawn_breaker",          // 破晓者：塔尔瓦（银河追光+钻石+极光金属锭，SA=震击）
            "third_sacred",          // 3rd圣遗物（银河追光+铬斧+下界之星，SA=樱花终结）
            "nue",                   // 影鵺（破晓者+羽毛+钼剑，SA=罪斩）
            // 崩坏线·苗刀系列：雷妖/电魂（由村正 Lv3 双分支升级）
            "byorai",                // 苗刀Lv4：雷妖（妖刀雨村+红石+铁块，SA=樱花）
            "byoden",                // 苗刀Lv4：电魂（妖刀村正+红石+铁块，亡灵杀手II）
            "raikiri",               // 雷切（雷妖+电魂+钼剑，SA=刃盾）
            "osahoko",               // 藏锋（雷妖+萤石+雷电结晶，SE=乱流）
            "sky_breaker",           // 开天剑（电魂+红石+铬钼钢锭+萤石粉，SE=天罚）
            "vibro_cutter",          // 高周波切割刀（雷妖+海晶碎片+青金石块，SA=高频坍缩）
            // 崩坏线·复合系列：柳叶/朱雀（由势州村正分出）
            "fuheliuye",             // 复合Lv1：柳叶（势州村正+钻石块+树叶，SA=锋刀抚柳）
            "fuhezhuque",             // 复合Lv1：朱雀（势州村正+金块+烈焰棒，SA=罪业之火）
            // 崩坏线·复合系列 Lv2/Lv3
            "crystal_cutter",         // 复合Lv2：结晶逆刃刀（柳叶+钻石块，SA=霜冻彗星）
            "thermal_cutter",         // 复合Lv2：热能切割刃（朱雀+烈焰棒，SA=熔铁之刃）
            "plasma_kagehide",        // 复合Lv3：等离子影秀（结晶逆刃刀+脉冲T17+钼剑，SA=绝对零度）
            "phoenix"                 // 复合Lv3：凰剑（热能切割刃+熔岩桶+羽毛，SA=熔铁之刃，SE=凰）
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
        // 崩坏线·脉冲太刀系列
        PULSE_KATANA_T17 = new PulseKatanaType17(context);
        PULSE_KATANA_T19 = new PulseKatanaType19(context);
        XUANYUAN_KATANA = new XuanYuanKatana(context);
        // 崩坏线·银河新星线
        GALACTIC_NOVA = new GalacticNova(context);
        VORPAL_SWORD = new VorpalSword(context);
        DAWN_BREAKER = new DawnBreaker(context);
        THIRD_SACRED = new ThirdSacredRelic(context);
        NUE = new Nue(context);
        // 崩坏线·苗刀系列
        BYO_RAI = new ByoRai(context);
        BYO_DEN = new ByoDen(context);
        RAIKIRI = new Raikiri(context);
        OSAHOKO = new Osahoko(context);
        SKY_BREAKER = new SkyBreaker(context);
        VIBRO_CUTTER = new VibroCutter(context);
        // 崩坏线·复合系列
        FUHE_LIUYE = new FuheLiuye(context);
        FUHE_ZHUQUE = new FuheZhuque(context);
        CRYSTAL_CUTTER = new CrystalCutter(context);
        THERMAL_CUTTER = new ThermalCutter(context);
        PLASMA_KAGEHIDE = new PlasmaKagehide(context);
        PHOENIX = new Phoenix(context);
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

    private static void fillBladeTab(CreativeModeTab.ItemDisplayParameters parameters,
                                     CreativeModeTab.Output output,
                                     net.minecraft.resources.ResourceLocation carrierItemId) {
        if (carrierItemId == null) {
            return;
        }
        HolderLookup.RegistryLookup<SlashBladeDefinition> definitions =
                SlashBlade.getSlashBladeDefinitionRegistry(parameters.holders());
        definitions.listElements()
                .sorted(Comparator.comparingInt(ref -> {
                    String path = ref.value().getName().getPath();
                    int idx = BLADE_ORDER.indexOf(path);
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .map(Holder.Reference::value)
                .filter(definition -> carrierItemId.equals(definition.getItemName()))
                .map(SlashBladeDefinition::getBlade)
                .filter(stack -> !stack.isEmpty())
                .forEach(output::accept);
    }
}
