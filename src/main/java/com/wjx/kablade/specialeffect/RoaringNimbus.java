package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Roaring Nimbus, MAG-Typhoon's lightning follow-up against paralyzed targets.
 * The vanilla lightning bolt is visual-only; controlled damage is applied separately.
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoaringNimbus extends SpecialEffect {

    public static final String HUD_KEY = "roaring_nimbus_cooldown";
    public static final int COOLDOWN_TICKS = 5 * 20;

    private static final float DAMAGE_FACTOR = 3.0F;
    private static boolean triggering;

    public RoaringNimbus() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || triggering) {
            return;
        }

        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)) {
            return;
        }

        ItemStack blade = player.getMainHandItem();
        if (!hasEffect(blade)
                || !target.hasEffect(ModMobEffects.PARALYSIS.get())
                || !SaTargeting.canDamage(player, target)
                || cooldownRemaining(player) > 0) {
            return;
        }

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(data -> data.set(HUD_KEY, COOLDOWN_TICKS));

        ServerLevel level = (ServerLevel) target.level();
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setVisualOnly(true);
        bolt.setPos(target.getX(), target.getY(), target.getZ());
        level.addFreshEntity(bolt);

        triggering = true;
        try {
            SaDamage.hurtSlashArtNoIFrame(target, level, player, dynamicDamage(blade));
        } finally {
            triggering = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }
        event.player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(data -> {
                    int remaining = data.get(HUD_KEY);
                    if (remaining > 0) {
                        data.set(HUD_KEY, remaining - 1);
                    }
                });
    }

    public static boolean isHeldBy(Player player) {
        return hasEffect(player.getMainHandItem());
    }

    public static int readiness(Player player) {
        if (!isHeldBy(player)) {
            return 0;
        }
        return COOLDOWN_TICKS - Math.min(cooldownRemaining(player), COOLDOWN_TICKS);
    }

    private static int cooldownRemaining(Player player) {
        return player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .map(data -> data.get(HUD_KEY))
                .orElse(0);
    }

    private static boolean hasEffect(ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.ROARING_NIMBUS.getId()))
                .orElse(false);
    }

    private static float dynamicDamage(ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        return MathFunc.amplifierCalc(bladeAttack, DAMAGE_FACTOR);
    }
}
