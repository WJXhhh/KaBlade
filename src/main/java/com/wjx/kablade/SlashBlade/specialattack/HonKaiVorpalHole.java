package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 时空黑洞 SA（Vorpal Hole）。
 * <p>
 * 驱动玩家拔刀次元斩姿态（SlashDim），在前方张开时空奇点，
 * 伴随连续交错斩击飞刃、80% 开场浮空斩与 6 次 20% 能量脉冲，最终坍缩终结。
 */
public class HonKaiVorpalHole extends SpecialAttackBase {

    private static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final float OPENING_DAMAGE_RATIO = 0.8F;
    private static final float PULSE_DAMAGE_RATIO = 0.2F;
    private static final int LIFETIME = 56;

    @Override
    public String toString() {
        return "vorpal_hole";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        if (world.isRemote) {
            return;
        }

        // 驱动玩家拔刀斩击动作（SlashDim 起手式）
        if (itemStack.hasTagCompound()) {
            NBTTagCompound tag = itemStack.getTagCompound();
            ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);
        }
        entityPlayer.swingArm(EnumHand.MAIN_HAND);

        float bladeAttack = 4.0F;
        if (itemStack.hasTagCompound()) {
            bladeAttack = ItemSlashBlade.BaseAttackModifier.get(itemStack.getTagCompound()) + 4.0F;
        }

        Vec3d look = entityPlayer.getLookVec();
        Vec3d flatLook = new Vec3d(look.x, 0.0, look.z);
        if (flatLook.lengthSquared() < 1.0e-6) {
            flatLook = new Vec3d(0.0, 0.0, 1.0);
        } else {
            flatLook = flatLook.normalize();
        }

        Vec3d origin = new Vec3d(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ)
                .add(flatLook.scale(2.35))
                .add(new Vec3d(0.0, 1.35, 0.0));

        float openingDamage = bladeAttack * OPENING_DAMAGE_RATIO * DAMAGE_MULTIPLIER;
        float pulseDamage = bladeAttack * PULSE_DAMAGE_RATIO * DAMAGE_MULTIPLIER;

        EntityVorpalBlackHole.spawn(world, entityPlayer, origin, LIFETIME, openingDamage, pulseDamage);

        // 1.20 原版技能启动音效
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.15F, 0.72F);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.95F, 0.42F);
    }
}
