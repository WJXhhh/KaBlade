package com.wjx.kablade.compat;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.slasharts.BreakTheDawnArts;
import com.wjx.kablade.slasharts.LiediArts;
import com.wjx.kablade.slasharts.NuclearShockArts;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Runs both without sridr and with the real optional mod on the test classpath. */
@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class SridrCompatibilityGameTests {
    private static final float BASE_ATTACK = 20.0F;

    private SridrCompatibilityGameTests() {
    }

    @GameTest(template = "multipart_targeting", batch = "sridr")
    public static void optionalRefineMultiplierAndGuards(GameTestHelper helper) {
        ItemStack blade = blade();
        close(helper, SridrCompat.damageMultiplier(blade), expectedMultiplier(blade),
                "refine 100 must follow the real sridr weapon modifier, or stay at 1 without sridr; "
                        + blade.getCapability(ItemSlashBlade.BLADESTATE).map(state ->
                        "base=" + state.getBaseAttackModifier() + ", refine=" + state.getRefine()
                                + ", broken=" + state.isBroken()).orElse("no blade state")
                        + ", modifiers=" + blade.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE));
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setRefine(0));
        close(helper, SridrCompat.damageMultiplier(blade), 1.0F, "unrefined blades must keep the old SA damage");
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setRefine(100);
            state.setBaseAttackModifier(0.0F);
        });
        close(helper, SridrCompat.damageMultiplier(blade), 1.0F, "zero base attack must not divide by zero");
        close(helper, SridrCompat.damageMultiplier(new ItemStack(Items.DIAMOND_SWORD)), 1.0F,
                "ordinary weapons must not activate compatibility");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting", batch = "sridr")
    public static void fixedExtraDamageDoesNotScaleManagedHitsTwice(GameTestHelper helper) {
        Zombie owner = owner(helper);
        DamageProbe target = target(helper, owner, new Vec3(2.0D, 2.0D, 3.0D));
        float multiplier = expectedMultiplier(owner.getMainHandItem());
        new LiediArts(entity -> ComboStateRegistry.NONE.getId()).doArts(SlashArts.ArtsType.Success, owner);
        helper.assertTrue(target.hits.stream().anyMatch(hit -> hit.source.is(SaDamage.SLASH_ART)
                        && near(hit.amount, 93.75F * multiplier)),
                "Liedi's fixed extra hit must receive exactly one refine multiplier");
        // ATTACK_DAMAGE-based callers share SaDamage with fixed damage. The helper
        // itself must leave their already calculated amount alone.
        target.hits.clear();
        SaDamage.hurt(target, owner.damageSources().mobAttack(owner), 37.0F);
        close(helper, target.hits.get(0).amount, 37.0F, "already calculated damage must not be scaled again");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting", batch = "sridr", timeoutTicks = 60)
    public static void delayedNuclearDamageSurvivesSwitchingWeapons(GameTestHelper helper) {
        Zombie owner = owner(helper);
        DamageProbe target = target(helper, owner, new Vec3(2.0D, 2.0D, 3.5D));
        float expected = (17.0F + MathFunc.amplifierCalc(BASE_ATTACK, 6.0F))
                * expectedMultiplier(owner.getMainHandItem());
        new NuclearShockArts(entity -> ComboStateRegistry.NONE.getId())
                .doArts(SlashArts.ArtsType.Success, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.runAfterDelay(28, () -> {
            helper.assertTrue(target.hits.stream().anyMatch(hit -> hit.source.is(SaDamage.SLASH_ART)
                            && near(hit.amount, expected)),
                    "the nuclear impact must retain the cast's multiplier after switching weapons");
            helper.succeed();
        });
    }

    @GameTest(template = "multipart_targeting", batch = "sridr", timeoutTicks = 30)
    public static void nativeSwordAndDriveUseDifferentDamagePaths(GameTestHelper helper) {
        Zombie owner = owner(helper);
        float expectedSword = (0.94F + MathFunc.amplifierCalc(BASE_ATTACK, 0.112F))
                * expectedMultiplier(owner.getMainHandItem());
        float expectedDrive = 0.56F + MathFunc.amplifierCalc(BASE_ATTACK, 0.112F);
        new BreakTheDawnArts(entity -> ComboStateRegistry.NONE.getId())
                .doArts(SlashArts.ArtsType.Success, owner);
        List<EntityBlisteringSwords> swords = helper.getLevel().getEntitiesOfClass(
                EntityBlisteringSwords.class, owner.getBoundingBox().inflate(4.0D),
                sword -> sword.getShooter() == owner);
        helper.assertTrue(!swords.isEmpty(), "the SA must spawn its native summoned swords");
        close(helper, (float) swords.get(0).getDamage(), expectedSword,
                "native summoned swords use fixed damage and need one multiplier");
        helper.runAfterDelay(7, () -> {
            List<EntityDrive> drives = helper.getLevel().getEntitiesOfClass(
                    EntityDrive.class, owner.getBoundingBox().inflate(20.0D),
                    drive -> drive.getShooter() == owner);
            helper.assertTrue(!drives.isEmpty(), "the delayed native drives must spawn");
            close(helper, (float) drives.get(0).getDamage(), expectedDrive,
                    "native drives already multiply by ATTACK_DAMAGE, so their coefficient must stay unchanged");
            helper.succeed();
        });
    }

    @GameTest(template = "multipart_targeting", batch = "sridr")
    public static void shieldScalesContactDamageWithoutChangingDurability(GameTestHelper helper) {
        Zombie owner = owner(helper);
        DamageProbe target = target(helper, owner, new Vec3(2.0D, 2.0D, 2.4D));
        float expected = RaikiriShieldEntity.calcContactDamage(BASE_ATTACK)
                * expectedMultiplier(owner.getMainHandItem());
        RaikiriShieldEntity shield = new RaikiriShieldEntity(helper.getLevel(), owner, BASE_ATTACK);
        close(helper, shield.getShieldBlood(), RaikiriShieldEntity.calcShieldBlood(BASE_ATTACK),
                "damage compatibility must not multiply shield durability");
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        shield.tick();
        helper.assertTrue(target.hits.stream().anyMatch(hit -> near(hit.amount, expected)),
                "shield contact damage must retain the casting blade's multiplier");
        helper.succeed();
    }

    private static ItemStack blade() {
        ItemStack blade = new ItemStack(ModItems.KABLADE_BLADE.get());
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setBaseAttackModifier(BASE_ATTACK);
            state.setRefine(100);
        });
        return blade;
    }

    private static float expectedMultiplier(ItemStack blade) {
        if (!ModList.get().isLoaded("sridr")) {
            return 1.0F;
        }
        // sridr 1.1's refine-100 branch is independent of all three config values.
        return SwordType.from(blade).contains(SwordType.FIERCEREDGE) ? 1.328F : 1.164F;
    }

    private static Zombie owner(GameTestHelper helper) {
        Zombie owner = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        owner.setNoAi(true);
        owner.setNoGravity(true);
        Vec3 position = helper.absoluteVec(new Vec3(2.0D, 2.0D, 2.0D));
        owner.setPos(position.x, position.y, position.z);
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        owner.setItemInHand(InteractionHand.MAIN_HAND, blade());
        helper.getLevel().addFreshEntity(owner);
        return owner;
    }

    private static DamageProbe target(GameTestHelper helper, LivingEntity owner, Vec3 relative) {
        DamageProbe target = new DamageProbe(helper.getLevel(), owner);
        Vec3 position = helper.absoluteVec(relative);
        target.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static void close(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(near(actual, expected), message + ": expected " + expected + ", got " + actual);
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < 0.001F;
    }

    private record Hit(DamageSource source, float amount) {
    }

    /** Record damage before armor/i-frames so assertions measure the SA formula. */
    private static final class DamageProbe extends Zombie {
        private final LivingEntity owner;
        private final List<Hit> hits = new ArrayList<>();

        private DamageProbe(ServerLevel level, LivingEntity owner) {
            super(EntityType.ZOMBIE, level);
            this.owner = owner;
            setNoAi(true);
            setNoGravity(true);
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.getEntity() == owner) {
                hits.add(new Hit(source, amount));
            }
            return true;
        }
    }
}
