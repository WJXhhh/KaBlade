package com.wjx.kablade;

import com.mojang.authlib.GameProfile;
import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.entity.SunfireImpactEntity;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.slasharts.SunfireImpactArts;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaCombatScaling;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class SunfireImpactGameTests {
    private SunfireImpactGameTests() { }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 130)
    public static void realCastHurtsLaunchesAndBurnsExactlyFourTimes(GameTestHelper h) {
        var owner = owner(h);
        var pig = pig(h, 3, 7);
        lock(owner, pig);
        float expected = SaCombatScaling.scaleDamage(owner,
                (16F + MathFunc.amplifierCalc(14F, 4.5F)) * 1.5F * (8F / 6.5F));
        cast(owner);
        var effect = effect(h, owner);
        h.runAfterDelay(18, () -> {
            h.assertTrue(pig.getHealth() == 200F, "no damage before the actual landing");
            h.assertTrue(owner.getY() > pig.getY() + 2.5, "caster must visibly rise above the target");
        });
        h.runAfterDelay(23, () -> {
            h.assertTrue(effect.getImpactAge() >= 19 && effect.getImpactAge() <= 23, "real ground contact must commit impact");
            close(h, 200F - pig.getHealth(), expected, "one full main hit");
            h.assertTrue(pig.getDeltaMovement().y > 0.6, "the victim must receive an upward impulse");
            h.assertTrue(pig.hurtTime > 0, "the native hurt animation must remain visible");
            h.assertTrue(pig.getLastHurtByMob() == owner, "the attacker must retain aggro/kill attribution");
            h.assertTrue(java.util.Arrays.stream(effect.getBurningIds()).anyMatch(id -> id == pig.getId()), "neutral mobs must receive owned ignition; ids="+java.util.Arrays.toString(effect.getBurningIds())+" pig="+pig.getId()+" rain="+pig.isInWaterRainOrBubble());
        });
        h.runAfterDelay(38, () -> {
            close(h, 200F - pig.getHealth(), expected, "main impact must not repeat during recovery");
            h.assertFalse(owner.isNoGravity(), "normal gravity must resume after recovery");
        });
        h.runAfterDelay(110, () -> {
            close(h, 200F - pig.getHealth(), expected + 4F, "exactly four owned fire pulses");
            h.assertTrue(effect.isRemoved(), "burn and cast state must expire");
            owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 55)
    public static void switchWeaponCancelsBeforeLandingAndRestoresGravity(GameTestHelper h) {
        var owner = owner(h); var pig = pig(h, 3, 7); lock(owner, pig); cast(owner);
        var effect = effect(h, owner);
        h.runAfterDelay(10, () -> owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        h.runAfterDelay(30, () -> {
            h.assertTrue(effect.isRemoved(), "switching weapons must cancel the cast");
            h.assertFalse(owner.isNoGravity(), "cancellation restores gravity");
            h.assertTrue(pig.getHealth() == 200, "a cancelled leap must not leave a ghost hit");
            owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 60)
    public static void noTargetAirCastAndDuplicateStart(GameTestHelper h) {
        var owner = owner(h);
        owner.setPos(owner.getX(), owner.getY() + 2, owner.getZ());
        cast(owner); var effect = effect(h, owner); cast(owner);
        h.assertTrue(h.getLevel().getEntitiesOfClass(SunfireImpactEntity.class, owner.getBoundingBox().inflate(20),
                e -> e.getOwnerId() == owner.getId()).size() == 1, "duplicate start must be rejected");
        h.assertTrue(Math.abs(effect.getLanding().z - owner.getZ() - 4) < 0.1, "no lock uses four blocks forward");
        h.runAfterDelay(28, () -> {
            h.assertTrue(effect.getImpactAge() >= 0, "an airborne start must still confirm a landing");
            h.assertFalse(SunfireImpactEntity.isCasting(owner), "control ends independently of afterglow");
            cast(owner);
            h.assertTrue(SunfireImpactEntity.isCasting(owner), "a new cast is allowed after recovery");
            owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire")
    public static void lowCeilingRejectsUnsafeArc(GameTestHelper h) {
        var owner = owner(h);
        for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) h.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
        cast(owner);
        h.assertFalse(SunfireImpactEntity.isCasting(owner), "a low roof must not be bypassed");
        h.assertFalse(owner.isNoGravity(), "a rejected cast must not change gravity");
        owner.discard(); h.succeed();
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 50)
    public static void wallShortensArcAndMovingTargetDoesNotMoveLanding(GameTestHelper h) {
        var owner = owner(h); var pig = pig(h, 3, 8); lock(owner, pig);
        for (int x = 0; x < 14; x++) for (int y = 1; y <= 7; y++) h.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
        cast(owner); var effect = effect(h, owner);
        Vec3 landing = effect.getLanding();
        h.assertTrue(landing.z < h.absoluteVec(new Vec3(3, 1, 5)).z, "wall must shorten the complete swept arc");
        pig.setPos(pig.getX() + 4, pig.getY(), pig.getZ());
        h.runAfterDelay(28, () -> {
            h.assertTrue(effect.getLanding().distanceToSqr(landing) < 0.001, "landing is captured at cast time");
            h.assertTrue(owner.getZ() < h.absoluteVec(new Vec3(3, 1, 5)).z, "caster must remain before the wall");
            owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 80)
    public static void fireResistanceAndOwnerLossStopDelayedDamage(GameTestHelper h) {
        var owner = owner(h); var pig = pig(h, 3, 7); lock(owner, pig); cast(owner);
        var effect = effect(h, owner);
        h.runAfterDelay(25, () -> {
            float health = pig.getHealth();
            pig.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60));
            h.runAfterDelay(23, () -> {
                close(h, pig.getHealth(), health, "new fire resistance cancels pending burn");
                h.assertTrue(effect.getBurningIds().length == 0, "extinguished targets lose attached effects");
                owner.discard();
                h.runAfterDelay(3, () -> { h.assertTrue(effect.isRemoved(), "missing owner cancels remaining state"); h.succeed(); });
            });
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 70)
    public static void teamProtectionCancelsBurnAndProtectsPets(GameTestHelper h) {
        var owner = owner(h); var pig = pig(h, 3, 7); lock(owner, pig);
        var wolf = h.spawn(EntityType.WOLF, new Vec3(4, 2, 7));
        wolf.tame(owner); wolf.setNoAi(true);
        cast(owner); var effect = effect(h, owner);
        h.runAfterDelay(25, () -> {
            h.assertTrue(wolf.getHealth() == wolf.getMaxHealth(), "own pets must receive no damage or launch");
            h.assertTrue(wolf.getDeltaMovement().y <= 0.01, "own pet must not be launched");
            var scoreboard = h.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("sf_" + owner.getId());
            team.setAllowFriendlyFire(false);
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(pig.getScoreboardName(), team);
            float health = pig.getHealth();
            h.runAfterDelay(23, () -> {
                close(h, pig.getHealth(), health, "new friendly-fire protection must cancel pending burn");
                h.assertTrue(java.util.Arrays.stream(effect.getBurningIds()).noneMatch(id -> id == pig.getId()), "protected targets must lose ignition");
                scoreboard.removePlayerTeam(team); owner.discard(); h.succeed();
            });
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 55)
    public static void removedFloorUsesActualLowerGroundForImpact(GameTestHelper h) {
        var owner = owner(h); cast(owner); var effect = effect(h, owner);
        double oldFloor = effect.getLanding().y;
        h.runAfterDelay(19, () -> {
            for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
            }
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(effect.getImpactAge() > 22, "removed ground must delay the hit until real contact");
            h.assertTrue(effect.getImpactCenter().y < oldFloor - 0.5, "impact must follow the lower floor");
            h.assertFalse(owner.isNoGravity(), "the later impact must also release movement control");
            owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 65)
    public static void recastAtRecoveryBoundaryDoesNotLeakGravityLock(GameTestHelper h) {
        var owner = owner(h); cast(owner);
        h.runAfterDelay(26, () -> cast(owner));
        h.runAfterDelay(56, () -> {
            h.assertFalse(owner.isNoGravity(), "a recovery-boundary recast must restore the original gravity");
            h.assertFalse(SunfireImpactEntity.isCasting(owner), "all movement controllers must be released");
            owner.discard(); h.succeed();
        });
    }

    private static ServerPlayer owner(GameTestHelper h) {
        var owner = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "SF" + UUID.randomUUID().toString().substring(0, 8)));
        Vec3 p = h.absoluteVec(new Vec3(3, 2, 3)); owner.setPos(p.x, p.y, p.z);
        owner.setYRot(0); owner.setXRot(0);
        var blade = new ItemStack(ModItems.KABLADE_HONKAI_BLADE.get());
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setBaseAttackModifier(14); state.setSlashArtsKey(ModSlashArts.SUNFIRE_IMPACT.getId());
        });
        owner.setItemInHand(InteractionHand.MAIN_HAND, blade);
        h.getLevel().addNewPlayer(owner);
        return owner;
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 55)
    public static void ascentDoesNotSendTeleportResetsAndStaleReportsCannotRewindIt(GameTestHelper h) {
        var owner = owner(h);
        var listener = new RecordingMovementListener(h.getLevel().getServer(), owner);
        owner.connection = listener;
        Vec3 origin = owner.position();
        cast(owner);
        for (int tick : new int[]{5, 8, 11}) h.runAfterDelay(tick, () -> {
            Vec3 before = owner.position();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    origin.x, origin.y, origin.z, 17F, 8F, true));
            h.assertTrue(owner.position().distanceToSqr(before) < 1.0E-8,
                    "a delayed client position must not rewind the authoritative leap");
            close(h, owner.getYRot(), 17, "camera rotation must remain available while casting");
        });
        h.runAfterDelay(15, () -> {
            h.assertTrue(owner.getY() > origin.y + 2.5, "movement must still reach the apex");
            h.assertTrue(listener.positionPackets == 0, "ascent must never reset client render interpolation via teleport packets");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(listener.positionPackets == 1, "only the final release may send a teleport correction");
            listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(listener.teleportId));
            Vec3 position = owner.position();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    position.x + 0.1, position.y, position.z, 23F, 0F, true));
            h.assertTrue(Math.abs(owner.getX() - position.x - 0.1) < 1.0E-5,
                    "normal client position reports must resume after recovery");
            owner.discard(); h.succeed();
        });
    }

    private static final class RecordingMovementListener extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        private int positionPackets;
        private int teleportId;
        RecordingMovementListener(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
            super(server, new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position) {
                positionPackets++;
                teleportId = position.getId();
            }
        }
    }

    @GameTest(template = "sunfire_impact", batch = "sunfire", timeoutTicks = 65)
    public static void multipartDamageIsDeduplicatedAndPlayersAreFiltered(GameTestHelper h) {
        var owner = owner(h);
        var target = new MultipartPig(h.getLevel());
        Vec3 p = h.absoluteVec(new Vec3(3, 2, 7)); target.setPos(p.x, p.y, p.z);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200); target.setNoAi(true);
        h.getLevel().addFreshEntity(target);
        for (var part : target.parts) part.setPos(p.x, p.y + 0.3, p.z);
        var protectedPlayer = owner(h);
        protectedPlayer.setPos(p.x + 1, p.y, p.z);
        lock(owner, target.parts[0]); cast(owner); var effect = effect(h, owner);
        h.runAfterDelay(25, () -> {
            h.assertTrue(target.mainHits == 1, "multipart bodies and parts must share one impact hit");
            h.assertTrue(target.getHealth() < 200, "damage must reach a physical multipart hitbox");
            h.assertTrue(protectedPlayer.getHealth() == protectedPlayer.getMaxHealth(), "default player filtering must protect players");
            h.assertTrue(java.util.Arrays.stream(effect.getBurningIds()).filter(id -> id == target.getId()).count() == 1,
                    "multipart targets receive exactly one burn record");
            // Water must cancel a previously accepted ignition before its first pulse.
            float health = target.getHealth();
            Vec3 water = h.absoluteVec(new Vec3(10, 2, 10));
            h.setBlock(new BlockPos(10, 2, 10), Blocks.WATER);
            target.setPos(water.x + 0.5, water.y, water.z + 0.5);
            h.runAfterDelay(23, () -> {
                close(h, target.getHealth(), health, "water must extinguish owned burn");
                h.assertTrue(java.util.Arrays.stream(effect.getBurningIds()).noneMatch(id -> id == target.getId()),
                        "water must clear the visual ignition record");
                owner.discard(); protectedPlayer.discard(); h.succeed();
            });
        });
    }

    private static final class MultipartPig extends Pig {
        private final net.minecraftforge.entity.PartEntity<?>[] parts;
        private int mainHits;
        MultipartPig(net.minecraft.server.level.ServerLevel level) {
            super(EntityType.PIG, level);
            parts = new net.minecraftforge.entity.PartEntity<?>[]{new PigPart(this), new PigPart(this)};
        }
        @Override public boolean isMultipartEntity() { return true; }
        @Override public net.minecraftforge.entity.PartEntity<?>[] getParts() { return parts; }
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
            if (amount > 1) mainHits++;
            return super.hurt(source, amount);
        }
        @Override public void setId(int id) {
            super.setId(id);
            if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
        }
    }
    private static final class PigPart extends net.minecraftforge.entity.PartEntity<MultipartPig> {
        PigPart(MultipartPig parent) { super(parent); }
        @Override public boolean isPickable() { return true; }
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
            return getParent().hurt(source, amount);
        }
        @Override protected void defineSynchedData() { }
        @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
        @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
    }
    private static Pig pig(GameTestHelper h, double x, double z) {
        var pig = h.spawn(EntityType.PIG, new Vec3(x, 2, z));
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); pig.setHealth(200); pig.setNoAi(true);
        return pig;
    }
    private static void lock(ServerPlayer owner, net.minecraft.world.entity.Entity target) {
        owner.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setTargetEntityId(target));
    }
    private static void cast(ServerPlayer owner) {
        new SunfireImpactArts(e -> ComboStateRegistry.NONE.getId()).doArts(SlashArts.ArtsType.Success, owner);
    }
    private static SunfireImpactEntity effect(GameTestHelper h, ServerPlayer owner) {
        var found = h.getLevel().getEntitiesOfClass(SunfireImpactEntity.class, owner.getBoundingBox().inflate(24),
                e -> e.getOwnerId() == owner.getId() && e.isControlling());
        h.assertTrue(found.size() == 1, "the real SA entrypoint must create one cast entity");
        return found.get(0);
    }
    private static void close(GameTestHelper h, float actual, float expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.02F, message + ": expected " + expected + ", got " + actual);
    }
}
