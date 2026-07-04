package com.wjx.kablade.object.item;

import com.wjx.kablade.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 雷霆凝晶 —— 表面平静、内蕴雷霆之力的晶体。
 * <p>
 * 右键方块：在目标位置召唤闪电 + 小爆炸（不破坏方块），消耗1个。
 * 左键实体：给目标挂 100 tick 延迟爆炸 debuff，到期后在同一位置召唤闪电 + 爆炸，消耗1个。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThunderCrystalItem extends Item {

    private static final String TAG_DELAY = Main.MODID + ":thunder_crystal_attack";
    private static final int DELAY_TICKS = 100;
    private static final float EXPLOSION_POWER = 2.0F;

    public ThunderCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.item.thunder_crystal").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    /**
     * 右键方块：立即触发闪电 + 爆炸。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ThunderCrystalItem)) {
            return;
        }

        BlockPos pos = event.getPos();
        triggerExplosion((ServerLevel) event.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        stack.shrink(1);
        event.setCanceled(true);
    }

    /**
     * 左键实体：挂延迟 debuff。
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ThunderCrystalItem)) {
            return;
        }

        target.getPersistentData().putInt(TAG_DELAY, DELAY_TICKS);
        held.shrink(1);
    }

    /**
     * 每 tick 递减延迟，到期触发闪电 + 爆炸。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        var data = entity.getPersistentData();
        if (!data.contains(TAG_DELAY)) {
            return;
        }

        int remaining = data.getInt(TAG_DELAY) - 1;
        if (remaining <= 0) {
            data.remove(TAG_DELAY);
            ServerLevel level = (ServerLevel) entity.level();
            triggerExplosion(level, entity.getX(), entity.getY(), entity.getZ());
        } else {
            data.putInt(TAG_DELAY, remaining);
        }
    }

    private static void triggerExplosion(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setVisualOnly(true);
        bolt.setPos(x, y, z);
        level.addFreshEntity(bolt);
        level.explode(null, x, y + 1, z, EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
    }
}
