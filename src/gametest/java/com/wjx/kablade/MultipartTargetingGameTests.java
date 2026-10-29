package com.wjx.kablade;

import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.util.SaTarget;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class MultipartTargetingGameTests {
    private static final double REACH = 3.0D;

    private MultipartTargetingGameTests() {
    }

    @GameTest(template = "multipart_targeting")
    public static void decorativePartsDoNotHideBody(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{false, false}, 2.0D, 2.0D);
        List<Entity> selected = select(attacker);
        helper.assertTrue(selected.stream().filter(entity -> entity == target).count() == 1,
                "a selectable body with decorative parts must be selected once");
        helper.assertFalse(selected.contains(target.parts[0]) || selected.contains(target.parts[1]),
                "decorative parts must not replace the body");

        float health = target.getHealth();
        AttackManager.areaAttack(attacker, ignored -> {}, 1.0F, true, true, true);
        helper.assertTrue(target.getHealth() < health, "SlashBlade melee must damage the selected body");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void bodySelectedWhenDamageablePartIsOutOfRange(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{true, false}, 2.0D, 9.0D);
        target.parts[1].setPos(target.getX(), target.getY(), target.getZ());
        List<Entity> selected = select(attacker);
        helper.assertTrue(selected.contains(target), "an out-of-range part must not hide an in-range body");
        helper.assertFalse(selected.contains(target.parts[0]) || selected.contains(target.parts[1]),
                "out-of-range or decorative parts must not be selected");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void decorativePartCannotExtendBodyRange(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{false}, 8.0D, 2.0D);
        List<Entity> selected = select(attacker);
        helper.assertFalse(selected.contains(target) || selected.contains(target.parts[0]),
                "an in-range decorative part must not make an out-of-range body hittable");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void selectablePartKeepsItsOwnDamageRules(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{true, false}, 2.0D, 2.0D);
        List<Entity> selected = select(attacker);
        helper.assertTrue(selected.contains(target.parts[0]), "the selectable part must remain a target");
        helper.assertFalse(selected.contains(target) || selected.contains(target.parts[1]),
                "a selected part must suppress the added body and decorative parts");

        // A rejected part hit is deliberate: it must not be retried on the body.
        float health = target.getHealth();
        AttackManager.areaAttack(attacker, ignored -> {}, 1.0F, true, true, true);
        helper.assertTrue(target.parts[0].hits > 0, "the actual melee must reach the part");
        helper.assertTrue(target.getHealth() == health, "a rejected part hit must not damage its body");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void unselectableBodyKeepsOriginalParts(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, false, new boolean[]{true, false}, 2.0D, 2.0D);
        List<Entity> selected = select(attacker);
        helper.assertFalse(selected.contains(target), "an unselectable boss body must not be added");
        helper.assertTrue(selected.contains(target.parts[0]) && selected.contains(target.parts[1]),
                "the original part rules must remain intact for an unselectable body");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void lockedBodySurvivesUnselectableParts(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{false}, 2.0D, 2.0D);
        // The fallback must keep the locked body even when another target is closer.
        helper.spawn(EntityType.ZOMBIE, new Vec3(1.2D, 1.0D, 1.0D));
        SaTarget selected = SaTargeting.findTarget(attacker, target, REACH).orElse(null);
        helper.assertTrue(selected != null && selected.hitEntity() == target,
                "SA lock must fall back to its selectable multipart body");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void emptyPartsAndOrdinaryMobs(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob empty = spawn(helper, true, new boolean[0], 2.0D, 2.0D);
        Zombie ordinary = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5D, 1.0D, 2.0D));
        List<Entity> selected = select(attacker);
        helper.assertTrue(selected.contains(empty), "a selectable body with no parts must remain targetable");
        helper.assertTrue(selected.contains(ordinary), "ordinary mobs must remain targetable");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void fallbackRespectsSlashBladeTargetRules(GameTestHelper helper) {
        ServerPlayer attacker = attacker(helper);
        FixtureMob target = spawn(helper, true, new boolean[]{false}, 2.0D, 2.0D);
        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("kablade_test");
        scoreboard.addPlayerToTeam(attacker.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(target.getScoreboardName(), team);
        try {
            helper.assertFalse(select(attacker).contains(target),
                    "the added body must pass SlashBlade's normal target conditions");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void twilightGoblinWhenInstalled(GameTestHelper helper) {
        EntityType<?> type = optionalTwilightType("blockchain_goblin");
        if (type == null) {
            if (Boolean.getBoolean("kablade.gametest.requireTwilight")) {
                helper.fail("Twilight Forest is required for this integration run");
                return;
            }
            Main.LOGGER.info("Skipping Twilight Forest integration scenario: mod is not installed");
            helper.succeed();
            return;
        }

        ServerPlayer attacker = attacker(helper);
        Mob goblin = (Mob) type.create(helper.getLevel());
        helper.assertTrue(goblin != null, "Twilight Forest goblin must be constructible");
        Vec3 pos = helper.absoluteVec(new Vec3(1.0D, 1.0D, 2.0D));
        goblin.setPos(pos.x, pos.y, pos.z);
        goblin.setNoAi(true);
        helper.getLevel().addFreshEntity(goblin);
        for (PartEntity<?> part : goblin.getParts()) {
            part.setPos(pos.x, pos.y, pos.z);
        }

        List<Entity> selected = select(attacker);
        helper.assertTrue(selected.stream().filter(entity -> entity == goblin).count() == 1,
                "Twilight Forest's chain and spike must not hide its goblin");
        float health = goblin.getHealth();
        AttackManager.areaAttack(attacker, ignored -> {}, 1.0F, true, true, true);
        helper.assertTrue(goblin.getHealth() < health, "a real SlashBlade melee hit must hurt the goblin");
        helper.succeed();
    }

    @GameTest(template = "multipart_targeting")
    public static void twilightHydraKeepsPartTargeting(GameTestHelper helper) {
        EntityType<?> type = optionalTwilightType("hydra");
        if (type == null) {
            if (Boolean.getBoolean("kablade.gametest.requireTwilight")) {
                helper.fail("Twilight Forest is required for this integration run");
                return;
            }
            helper.succeed();
            return;
        }

        ServerPlayer attacker = attacker(helper);
        Mob hydra = (Mob) type.create(helper.getLevel());
        helper.assertTrue(hydra != null, "Twilight Forest hydra must be constructible");
        Vec3 pos = helper.absoluteVec(new Vec3(1.0D, 1.0D, 2.0D));
        hydra.setPos(pos.x, pos.y, pos.z);
        hydra.setNoAi(true);
        helper.getLevel().addFreshEntity(hydra);
        PartEntity<?>[] parts = hydra.getParts();
        helper.assertTrue(parts != null && parts.length > 0, "hydra must have parts");
        for (PartEntity<?> part : parts) {
            part.setPos(pos.x, pos.y, pos.z);
        }
        List<Entity> selected = select(attacker);
        helper.assertFalse(selected.contains(hydra), "the unselectable hydra body must not be added");
        helper.assertTrue(selected.stream().anyMatch(entity -> entity instanceof PartEntity<?> part
                && part.getParent() == hydra), "hydra's original parts must remain selectable");
        helper.succeed();
    }

    private static EntityType<?> optionalTwilightType(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("twilightforest", path);
        return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    private static ServerPlayer attacker(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        Vec3 pos = helper.absoluteVec(new Vec3(1.0D, 1.0D, 1.0D));
        player.setPos(pos.x, pos.y, pos.z);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KABLADE_BLADE.get()));
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0D);
        return player;
    }

    private static FixtureMob spawn(GameTestHelper helper, boolean pickable, boolean[] partPickable,
                                    double bodyZ, double partZ) {
        ServerLevel level = helper.getLevel();
        FixtureMob entity = new FixtureMob(level, pickable, partPickable);
        Vec3 body = helper.absoluteVec(new Vec3(1.0D, 1.0D, bodyZ));
        entity.setPos(body.x, body.y, body.z);
        entity.setNoAi(true);
        level.addFreshEntity(entity);
        Vec3 partPosition = helper.absoluteVec(new Vec3(1.0D, 1.0D, partZ));
        for (FixturePart part : entity.parts) {
            part.setPos(partPosition.x, partPosition.y, partPosition.z);
        }
        return entity;
    }

    private static List<Entity> select(ServerPlayer attacker) {
        AABB area = attacker.getBoundingBox().move(0.0D, 0.0D, 1.0D).inflate(REACH);
        return TargetSelector.getTargettableEntitiesWithinAABB(attacker.level(), attacker, area, REACH);
    }

    private static final class FixtureMob extends Zombie {
        private final boolean pickable;
        private final FixturePart[] parts;

        private FixtureMob(ServerLevel level, boolean pickable, boolean[] partPickable) {
            super(EntityType.ZOMBIE, level);
            this.pickable = pickable;
            this.parts = new FixturePart[partPickable.length];
            for (int i = 0; i < partPickable.length; i++) {
                this.parts[i] = new FixturePart(this, partPickable[i]);
            }
        }

        @Override
        public boolean isPickable() {
            return this.pickable;
        }

        @Override
        public boolean isMultipartEntity() {
            return true;
        }

        @Override
        public PartEntity<?>[] getParts() {
            return this.parts;
        }

        @Override
        public void setId(int id) {
            super.setId(id);
            if (this.parts != null) {
                for (int i = 0; i < this.parts.length; i++) {
                    this.parts[i].setId(id + i + 1);
                }
            }
        }
    }

    private static final class FixturePart extends PartEntity<FixtureMob> {
        private final boolean pickable;
        private int hits;

        private FixturePart(FixtureMob parent, boolean pickable) {
            super(parent);
            this.pickable = pickable;
        }

        @Override
        public boolean isPickable() {
            return this.pickable;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            this.hits++;
            return false;
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }
    }
}
