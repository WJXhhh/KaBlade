package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityStageLight;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.IJustSpecialAttack;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 聚光舞台（Lights on Stage）——银河追光专属 SA。
 * <p>
 * 从 1.20 的 {@code LightsOnStageArts} 移植：起手旋斩后在脚下展开金白色舞台光环，
 * 第 5 tick 对舞台范围内可命中目标结算一次环形斩击，光环随后淡出。
 * 1.12.2 没有 {@code SlashArts/ArtsType}，Just Attack 通过 {@link IJustSpecialAttack} 获得 1.25 倍伤害。
 */
public class HonkaiLightsOnStage extends SpecialAttackBase implements IJustSpecialAttack {

    private static final int STAGE_LIFETIME = 80;
    private static final float BASE_DAMAGE = 6.0F;
    private static final float ATTACK_FACTOR = 1.15F;
    private static final float JUST_TIMING_BONUS = 1.25F;

    @Override
    public String toString() {
        return "lights_on_stage";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer player) {
        this.trigger(itemStack, player, 1.0F);
    }

    @Override
    public void doJustSpacialAttack(ItemStack itemStack, EntityPlayer player) {
        this.trigger(itemStack, player, JUST_TIMING_BONUS);
    }

    private void trigger(ItemStack itemStack, EntityPlayer player, float timingBonus) {
        World world = player.world;
        if (world.isRemote) {
            return;
        }

        NBTTagCompound tag = itemStack.getTagCompound();
        float bladeAttack = tag != null ? ItemSlashBlade.BaseAttackModifier.get(tag) : 4.0F;
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * timingBonus;

        EntityStageLight stage = new EntityStageLight(world, player, damage, STAGE_LIFETIME);
        stage.setPosition(player.posX, player.posY + 0.06D, player.posZ);
        stage.rotationYaw = player.rotationYaw;
        world.spawnEntity(stage);

        openingSparkles((WorldServer) world, player);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.15F, 1.75F);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 1.1F);
    }

    private static void openingSparkles(WorldServer world, EntityPlayer player) {
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0D * i / 18.0D;
            double radius = 0.9D + i * 0.12D;
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    player.posX + Math.cos(angle) * radius,
                    player.posY + 0.12D,
                    player.posZ + Math.sin(angle) * radius,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
