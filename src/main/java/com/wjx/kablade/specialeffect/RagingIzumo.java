package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RagingIzumo extends SpecialEffect {

    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final double PROC_CHANCE = 0.1D;
    private static final int COOLDOWN_TICKS = 20;
    private static final float BASE_DAMAGE = 8.0F;
    private static final float EXTRA_DAMAGE_FACTOR = 3.0F;
    private static final double RADIUS = 5.0D;
    private static final double VERTICAL_RADIUS = 4.0D;
    private static final Vector3f GOLD_SMOKE = new Vector3f(1.0F, 0.945F, 0.333F);

    public RagingIzumo() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!hasEffect(player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getServer().getTickCount();
        if (COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {
            return;
        }
        if (level.random.nextDouble() >= PROC_CHANCE) {
            return;
        }

        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        float bladeAttack = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        trigger(level, player, MathFunc.amplifierCalc(bladeAttack, EXTRA_DAMAGE_FACTOR));
    }

    private static void trigger(ServerLevel level, Player player, float extraDamage) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setVisualOnly(true);
        bolt.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(bolt);

        for (int i = 0; i < 40; i++) {
            double sx = level.random.nextBoolean() ? 1.0D : -1.0D;
            double sz = level.random.nextBoolean() ? 1.0D : -1.0D;
            double x = player.getX() + level.random.nextDouble() * 2.0D * sx;
            double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
            double z = player.getZ() + level.random.nextDouble() * 2.0D * sz;
            level.sendParticles(ParticleTypes.POOF, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(new DustParticleOptions(GOLD_SMOKE, 2.0F),
                    x, y, z, 1, 0.08D, 0.08D, 0.08D, 0.0D);
        }

        AABB area = player.getBoundingBox()
                .inflate(RADIUS, VERTICAL_RADIUS, RADIUS)
                .move(player.getDeltaMovement());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamage(player, target))) {
            target.hurt(level.damageSources().explosion(player, player), BASE_DAMAGE + extraDamage);
            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), 40, 2));
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2, false, true));
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.RAGING_IZUMO.getId()))
                .orElse(false);
    }
}
