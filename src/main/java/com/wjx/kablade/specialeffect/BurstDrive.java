package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

/**
 * SP Light 线特殊效果「爆裂驱动」。
 * <p>
 * 从 1.12.2 {@code BurstDrive} 简化移植：
 * 持有者挥刀时向前方射出一道白色飞斩，并伴随女巫粒子。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BurstDrive extends SpecialEffect {

    private static final int COLOR = 0xFFFFFF;
    private static final float DAMAGE_BASE = 5.0F;
    private static final float DAMAGE_FACTOR = 1.0F;
    private static final double COST = 2.0;

    private static final WeakHashMap<Player, Integer> LAST_SWING = new WeakHashMap<>();

    public BurstDrive() {
        super(-1, false, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient()) {
            return;
        }

        Player player = event.player;
        if (!hasEffect(player)) {
            LAST_SWING.remove(player);
            return;
        }

        int lastSwing = LAST_SWING.getOrDefault(player, -1);
        int currentSwing = player.swingTime;

        if (currentSwing == 0 && lastSwing > 0) {
            LAST_SWING.put(player, currentSwing);
            trigger((ServerLevel) player.level(), player);
            spawnParticles((ServerLevel) player.level(), player);
        } else if (currentSwing > 0) {
            LAST_SWING.put(player, currentSwing);
        }
    }

    private static void trigger(ServerLevel level, Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return;
        }

        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if (state.getProudSoulCount() < COST) {
                return;
            }
            state.setProudSoulCount(state.getProudSoulCount() - (int) COST);

            Vec3 look = player.getLookAngle();
            Vec3 pos = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(pos.x, pos.y, pos.z);
            drive.setShooter(player);
            drive.setDamage(dynamicDamage(blade));
            drive.setColor(COLOR);
            drive.setLifetime(20);
            drive.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
            level.addFreshEntity(drive);
        });
    }

    private static float dynamicDamage(ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        return DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_FACTOR);
    }

    private static void spawnParticles(ServerLevel level, Player player) {
        if (level.random.nextInt(4) == 0) {
            double ox = (level.random.nextDouble() - 0.5) * player.getBbWidth();
            double oy = level.random.nextDouble() * player.getBbHeight();
            double oz = (level.random.nextDouble() - 0.5) * player.getBbWidth();
            level.sendParticles(ParticleTypes.WITCH,
                    player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                    1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.BURST_DRIVE.getId()))
                .orElse(false);
    }
}
