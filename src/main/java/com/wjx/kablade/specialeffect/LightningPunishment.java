package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.LightningPunishmentPacket;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaCombatScaling;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A successful melee hit can schedule an owner-bound lightning burst. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningPunishment extends SpecialEffect {
    public static final int COOLDOWN_TICKS = 140;
    private static final String KEY = "lightning_punishment_cooldown";
    private static final double RADIUS = 4.0D;
    private static final Map<UUID, Burst> PENDING = new HashMap<>();

    public LightningPunishment() { super(-1, true, true); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player || !isHeldBy(player)
                || !SaTargeting.canDamage(player, event.getEntity()) || cooldown(player) > 0
                || player.getRandom().nextFloat() >= 0.30F) return;
        // One rocket deals amplifierCalc(15, 1) * 1.5, and Treason fires six.
        float damage = SaCombatScaling.scaleDamage(player, MathFunc.amplifierCalc(15.0F, 1.0F) * 9.0F);
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> data.set(KEY, COOLDOWN_TICKS));
        PENDING.put(player.getUUID(), new Burst(event.getEntity().position(), damage));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            int remaining = data.get(KEY);
            if (remaining > 0) data.set(KEY, remaining - 1);
        });
        Burst burst = PENDING.remove(player.getUUID());
        if (burst == null || !player.isAlive() || !(player.level() instanceof ServerLevel level)) return;
        Vec3 center = burst.center();
        KabladeNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                center.x, center.y, center.z, 64, level.dimension())),
                new LightningPunishmentPacket(center.x, center.y, center.z));
        for (var target : SaTargeting.uniqueTargets(level, player, new AABB(center, center).inflate(RADIUS), true)) {
            if (target.distanceToSqr(center) > RADIUS * RADIUS) continue;
            if (SaDamage.hurtSlashArtNoIFrame(target.hitEntity(), level, player, burst.damage())) {
                target.root().addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), 80, 1));
            }
        }
    }

    public static boolean isHeldBy(Player player) {
        var blade = player.getMainHandItem();
        return blade.getItem() instanceof ItemSlashBlade && blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.LIGHTNING_PUNISHMENT.getId())).orElse(false);
    }
    private static int cooldown(Player player) {
        return player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).map(data -> data.get(KEY)).orElse(0);
    }
    public static int readiness(Player player) { return COOLDOWN_TICKS - Math.min(COOLDOWN_TICKS, cooldown(player)); }
    private record Burst(Vec3 center, float damage) { }
}
