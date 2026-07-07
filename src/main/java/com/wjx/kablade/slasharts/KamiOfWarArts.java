package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KamiOfWarArts extends SlashArts {

    public static final String PROP_KEY = "kami_of_war_count";

    private static final Map<UUID, KamiState> ACTIVE = new ConcurrentHashMap<>();
    private static final int PULSE_COUNT = 6;
    private static final int FIRST_DELAY = 1;
    private static final int PULSE_INTERVAL = 20;
    private static final int RESISTANCE_DURATION = 40;
    private static final int STRENGTH_DURATION = 140;
    private static final int PARALYSIS_DURATION = 40;
    private static final int PARALYSIS_AMPLIFIER = 2;
    private static final float BASE_DAMAGE = 8.0F;
    private static final float EXTRA_DAMAGE_FACTOR = 3.0F;
    private static final double RADIUS = 5.0D;
    private static final double VERTICAL_RADIUS = 4.0D;
    private static final Vector3f GOLD_SMOKE = new Vector3f(1.0F, 0.945F, 0.333F);

    public KamiOfWarArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, EXTRA_DAMAGE_FACTOR);
        long now = level.getServer().getTickCount();

        ACTIVE.put(user.getUUID(), new KamiState(
                level.dimension(), user.getUUID(), extraDamage, PULSE_COUNT, now + FIRST_DELAY));
        setCounter(user, PULSE_COUNT);

        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                RESISTANCE_DURATION, 1, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                STRENGTH_DURATION, 2, false, true));

        return super.doArts(type, user);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }

        long now = event.getServer().getTickCount();
        ACTIVE.entrySet().removeIf(entry -> {
            KamiState state = entry.getValue();
            ServerLevel level = event.getServer().getLevel(state.dimension);
            if (level == null) {
                return true;
            }

            Entity entity = level.getEntity(state.ownerUUID);
            if (!(entity instanceof LivingEntity user) || !user.isAlive()) {
                clearCounter(entity);
                return true;
            }

            if (now < state.nextPulseAt) {
                return false;
            }

            pulse(level, user, state.extraDamage);
            state.pulsesLeft--;
            setCounter(user, state.pulsesLeft);

            if (state.pulsesLeft <= 0) {
                setCounter(user, 0);
                return true;
            }

            state.nextPulseAt = now + PULSE_INTERVAL;
            return false;
        });
    }

    private static void pulse(ServerLevel level, LivingEntity user, float extraDamage) {
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setVisualOnly(true);
        bolt.setPos(user.getX(), user.getY(), user.getZ());
        level.addFreshEntity(bolt);

        for (int i = 0; i < 40; i++) {
            double sx = level.random.nextBoolean() ? 1.0D : -1.0D;
            double sz = level.random.nextBoolean() ? 1.0D : -1.0D;
            double x = user.getX() + level.random.nextDouble() * 2.0D * sx;
            double y = user.getY() + level.random.nextDouble() * user.getBbHeight();
            double z = user.getZ() + level.random.nextDouble() * 2.0D * sz;
            level.sendParticles(ParticleTypes.POOF, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(new DustParticleOptions(GOLD_SMOKE, 2.0F),
                    x, y, z, 1, 0.08D, 0.08D, 0.08D, 0.0D);
        }

        AABB area = user.getBoundingBox()
                .inflate(RADIUS, VERTICAL_RADIUS, RADIUS)
                .move(user.getDeltaMovement());
        ItemStack blade = user.getMainHandItem();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamage(user, target))) {
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                    level.damageSources().explosion(user, user), BASE_DAMAGE + extraDamage);
            if (blade.getItem() instanceof ItemSlashBlade && user instanceof Player player) {
                blade.hurtEnemy(target, player);
            }
            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                    PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
        }
    }

    private static void setCounter(LivingEntity user, int value) {
        if (user instanceof Player player) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(data -> data.set(PROP_KEY, value));
        }
    }

    private static void clearCounter(Entity entity) {
        if (entity instanceof Player player) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(data -> data.remove(PROP_KEY));
        }
    }

    private static final class KamiState {
        private final ResourceKey<Level> dimension;
        private final UUID ownerUUID;
        private final float extraDamage;
        private int pulsesLeft;
        private long nextPulseAt;

        private KamiState(ResourceKey<Level> dimension, UUID ownerUUID,
                          float extraDamage, int pulsesLeft, long nextPulseAt) {
            this.dimension = dimension;
            this.ownerUUID = ownerUUID;
            this.extraDamage = extraDamage;
            this.pulsesLeft = pulsesLeft;
            this.nextPulseAt = nextPulseAt;
        }
    }
}
