package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Glacial Bane, Ice Epiphyllum's charged area-hit special effect from 1.12.2. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlacialBane extends SpecialEffect {

    public static final String PROP_KEY = "glacial_bane_extra_tick";

    private static final int MAX_CHARGE = 120;
    private static final int TRIGGER_CHARGE = 100;
    private static final double RANGE = 4.0D;
    private static final float DAMAGE_BASE = 5.0F;
    private static final float DAMAGE_FACTOR = 1.0F;

    public GlacialBane() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }

        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            if (!hasEffect(player)) {
                data.remove(PROP_KEY);
                return;
            }
            int current = data.get(PROP_KEY);
            if (current < MAX_CHARGE) {
                data.set(PROP_KEY, current + 1);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || !hasEffect(player)) {
            return;
        }

        int charge = player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .map(data -> data.get(PROP_KEY))
                .orElse(0);
        if (charge <= TRIGGER_CHARGE) {
            return;
        }

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(data -> data.set(PROP_KEY, 0));

        ServerLevel level = (ServerLevel) player.level();
        float damage = dynamicDamage(player.getMainHandItem());
        for (int i = 0; i < 60; i++) {
            double sx = level.random.nextBoolean() ? 1.0D : -1.0D;
            double sz = level.random.nextBoolean() ? 1.0D : -1.0D;
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX() + level.random.nextDouble() * 3.0D * sx,
                    player.getY() + level.random.nextDouble(),
                    player.getZ() + level.random.nextDouble() * 3.0D * sz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        AABB box = player.getBoundingBox()
                .inflate(RANGE, RANGE, RANGE)
                .move(player.getDeltaMovement());
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                target -> SaTargeting.canDamage(player, target));

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), damage);
        }
    }

    private static float dynamicDamage(ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        return (DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_FACTOR)) * 2.0F;
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.GLACIAL_BANE.getId()))
                .orElse(false);
    }
}
