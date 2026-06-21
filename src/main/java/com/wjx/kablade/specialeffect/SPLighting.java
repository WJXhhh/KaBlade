package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.WeakHashMap;

/**
 * SP Light 线特殊效果「圣光」。
 * <p>
 * 从 1.12.2 {@code SPLighting} 简化移植：
 * 持有者挥刀时向锁定/视线目标发射一道金色召唤剑，并伴随女巫粒子。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SPLighting extends SpecialEffect {

    private static final int COLOR = 0xFFD700;
    private static final float DAMAGE = 6.0F;
    private static final double COST = 2.0;

    /** 记录玩家是否已经过了本次挥刀的触发点，避免同一挥刀多次触发。 */
    private static final WeakHashMap<Player, Integer> LAST_SWING = new WeakHashMap<>();

    public SPLighting() {
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

        // 客户端 swingTime 在 1.20.1 中于每次挥刀时递增；这里检测从非零回到零或首次挥刀
        if (currentSwing == 0 && lastSwing > 0) {
            LAST_SWING.put(player, currentSwing);
            trigger((ServerLevel) player.level(), player);
        } else if (currentSwing > 0) {
            LAST_SWING.put(player, currentSwing);
        }

        spawnParticles(player);
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

            Entity target = SATool.getEntityToWatch(player);
            if (target == null || !target.isAlive()) {
                return;
            }

            Vec3 pos = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
            EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(
                    SlashBlade.RegistryEvents.SummonedSword, level);
            sword.setPos(pos.x, pos.y, pos.z);
            sword.setShooter(player);
            sword.setDamage(DAMAGE);
            sword.setColor(COLOR);
            sword.setTargetEntityId(target.getId());
            sword.setMaxHitCount(1);
            sword.setLifeTime(40);
            level.addFreshEntity(sword);
        });
    }

    private static void spawnParticles(Player player) {
        if (player.level().isClientSide() && player.level().random.nextInt(4) == 0) {
            double ox = (player.level().random.nextDouble() - 0.5) * player.getBbWidth();
            double oy = player.level().random.nextDouble() * player.getBbHeight();
            double oz = (player.level().random.nextDouble() - 0.5) * player.getBbWidth();
            player.level().addParticle(ParticleTypes.WITCH,
                    player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                    0.0, 0.02, 0.0);
        }
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.SP_LIGHTING.getId()))
                .orElse(false);
    }
}
