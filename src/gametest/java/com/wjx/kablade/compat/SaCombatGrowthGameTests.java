package com.wjx.kablade.compat;

import com.mojang.authlib.GameProfile;
import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.slasharts.NarukamiDivinityArts;
import com.wjx.kablade.slasharts.NuclearShockArts;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaCombatScaling;
import com.wjx.kablade.util.SaDamage;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank.ConcentrationRanks;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.ModAttributes;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class SaCombatGrowthGameTests {
    private SaCombatGrowthGameTests() {
    }

    @GameTest(template = "multipart_targeting", batch = "sa-growth")
    public static void rankAndExperienceFollowTheInstalledCombatRules(GameTestHelper helper) {
        ServerPlayer player = player(helper, 20.0F);
        setRank(player, ConcentrationRanks.S);
        player.experienceLevel = 100;
        close(helper, SaCombatScaling.nativeRankBonus(player), 100.0F, "native S bonus uses experience below refine");
        close(helper, SaCombatScaling.damageMultiplier(player), SstBladeCompat.isLoaded() ? 6.033F : 6.0F,
                "S damage must use the installed melee rules");
        player.experienceLevel = 1000;
        close(helper, SaCombatScaling.nativeRankBonus(player), 768.0F, "native bonus stops at the blade's refine");
        close(helper, SaCombatScaling.damageMultiplier(player), SstBladeCompat.isLoaded() ? 6.033F : 39.4F,
                "SST replaces the native experience cap with its own refine curve");
        setRank(player, ConcentrationRanks.SSS);
        close(helper, SaCombatScaling.damageMultiplier(player), SstBladeCompat.isLoaded() ? 12.035F : 39.4F,
                "SSS must follow the corresponding sustained melee bonus");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting", batch = "sa-growth")
    public static void nativeSwordGlobalScalingIsNotAppliedTwice(GameTestHelper helper) {
        ServerPlayer player = player(helper, 20.0F);
        player.getAttribute(ModAttributes.getSlashBladeDamage()).setBaseValue(2.0D);
        close(helper, SaCombatScaling.scaleDamage(player, 10.0F), 20.0F,
                "direct/custom SA damage needs the SlashBlade attribute");
        close(helper, SaCombatScaling.scaleSummonedSwordDamage(player, 10.0F), 10.0F,
                "native swords apply that attribute themselves");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting", batch = "sa-growth", timeoutTicks = 50)
    public static void delayedDamageKeepsRankExperienceAndWeaponSnapshot(GameTestHelper helper) {
        ServerPlayer player = player(helper, 20.0F);
        helper.getLevel().addNewPlayer(player);
        setRank(player, ConcentrationRanks.S);
        player.experienceLevel = 100;
        var warden = helper.spawn(EntityType.WARDEN, new Vec3(2, 2, 3.5));
        warden.setNoAi(true);
        warden.setNoGravity(true);
        warden.setAbsorptionAmount(10_000.0F);
        float pool = warden.getAbsorptionAmount() + warden.getHealth();
        float expected = (17.0F + MathFunc.amplifierCalc(20.0F, 6.0F))
                * (SstBladeCompat.isLoaded() ? 6.033F : 6.0F);
        new NuclearShockArts(entity -> ComboStateRegistry.NONE.getId()).doArts(SlashArts.ArtsType.Success, player);
        player.experienceLevel = 0;
        setRank(player, ConcentrationRanks.NONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.runAfterDelay(28, () -> {
            float actual = pool - warden.getHealth() - warden.getAbsorptionAmount();
            player.discard();
            close(helper, actual, expected, "delayed damage must keep the complete cast snapshot");
            helper.succeed();
        });
    }

    @GameTest(template = "multipart_targeting", batch = "sa-growth-sst", timeoutTicks = 30)
    public static void realSstAffinityAppliesExactlyOnceAfterSwitchingBlades(GameTestHelper helper) throws Exception {
        if (!SstBladeCompat.isLoaded()) {
            helper.succeed();
            return;
        }
        // The local integration fixture contains SST's original, unmodified handler/API.
        MinecraftForge.EVENT_BUS.register(Class.forName("cn.star.sstmod.slashblade.BladeAffinityHandler"));
        ServerPlayer player = player(helper, 64.0F);
        helper.getLevel().addNewPlayer(player);
        ItemStack blade = player.getMainHandItem();
        blade.enchant(Enchantments.UNBREAKING, 1);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setDefaultBewitched(true));
        Class<?> api = Class.forName("cn.star.sstmod.slashblade.BladeAffinity");
        api.getMethod("setPercent", ItemStack.class, UUID.class, double.class)
                .invoke(null, blade, player.getUUID(), 50.0D);
        double percent = (double) api.getMethod("getDamageBonusPercent", ItemStack.class, UUID.class)
                .invoke(null, blade, player.getUUID());
        helper.assertTrue(percent > 0.0D, "the real SST API must recognize the blade's affinity");
        var warden = helper.spawn(EntityType.WARDEN, new Vec3(2, 2, 3.5));
        warden.setNoAi(true);
        warden.setNoGravity(true);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setTargetEntityId(warden));
        float expected = (54.0F + MathFunc.amplifierCalc(64.0F, 14.0F)) * 8.40F * 0.12F
                * (float) (1.0D + percent / 100.0D);
        new NarukamiDivinityArts(entity -> ComboStateRegistry.NONE.getId()).doArts(SlashArts.ArtsType.Success, player);
        api.getMethod("setPercent", ItemStack.class, UUID.class, double.class)
                .invoke(null, blade, player.getUUID(), 100.0D);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.runAfterDelay(7, () -> {
            float actual = 500.0F - warden.getHealth();
            player.discard();
            close(helper, actual, expected, "custom Narukami damage must receive SST affinity exactly once at cast value");
            helper.succeed();
        });
    }

    private static ServerPlayer player(GameTestHelper helper, float base) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GrowthTest"));
        Vec3 position = helper.absoluteVec(new Vec3(2, 2, 2));
        player.setPos(position.x, position.y, position.z);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0D);
        ItemStack blade = new ItemStack(ModItems.KABLADE_HONKAI_BLADE.get());
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setBaseAttackModifier(base);
            state.setRefine(768);
        });
        // Hold weapon damage constant to isolate rank, experience, and affinity
        // independently of which refine-curve mod is installed.
        blade.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(SridrCompat.weaponDamageUuid(), "Weapon modifier", base - 1,
                        AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, blade);
        setRank(player, ConcentrationRanks.NONE);
        return player;
    }

    @GameTest(template = "multipart_targeting", batch = "sa-growth-sst")
    public static void deferredAffinityIsAppliedOnceAndItsScopeIsRestored(GameTestHelper helper) throws Exception {
        if (!SstBladeCompat.isLoaded()) {
            helper.succeed();
            return;
        }
        MinecraftForge.EVENT_BUS.register(Class.forName("cn.star.sstmod.slashblade.BladeAffinityHandler"));
        ServerPlayer player = player(helper, 20.0F);
        ItemStack blade = player.getMainHandItem();
        blade.enchant(Enchantments.UNBREAKING, 1);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setDefaultBewitched(true));
        Class<?> api = Class.forName("cn.star.sstmod.slashblade.BladeAffinity");
        api.getMethod("setPercent", ItemStack.class, UUID.class, double.class)
                .invoke(null, blade, player.getUUID(), 50.0D);
        double percent = SstBladeCompat.affinityBonus(player);
        helper.assertTrue(percent > 0.0D, "the deferred test must capture nonzero affinity");
        var warden = helper.spawn(EntityType.WARDEN, new Vec3(2, 2, 3.5));
        warden.setNoAi(true);
        Runnable callback = SstBladeCompat.deferred(player, () ->
                SaDamage.hurtSlashArtNoIFrame(warden, helper.getLevel(), player, 10.0F));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        callback.run();
        close(helper, 500.0F - warden.getHealth(), (float) (10.0D * (1.0D + percent / 100.0D)),
                "the real SST handler must use the captured source once");
        var plainSource = player.damageSources().playerAttack(player);
        helper.assertTrue(SstBladeCompat.captureSource(plainSource) == plainSource,
                "the deferred affinity scope must not leak into later attacks");
        helper.succeed();
    }

    private static void setRank(ServerPlayer player, ConcentrationRanks rank) {
        player.getCapability(CapabilityConcentrationRank.RANK_POINT).ifPresent(value -> {
            value.setRawRankPoint(value.getUnitCapacity() * rank.level);
            value.setLastUpdte(player.level().getGameTime());
        });
    }

    private static void close(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.02F,
                message + ": expected " + expected + ", got " + actual);
    }
}
