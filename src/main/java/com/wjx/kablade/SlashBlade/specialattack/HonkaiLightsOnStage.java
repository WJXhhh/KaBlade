package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityStageLight;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** 1.12.2 port of Galactic Nova's Lights on Stage slash art. */
public class HonkaiLightsOnStage extends SpecialAttackBase {

    private static final float BASE_DAMAGE = 6.0F;
    private static final float ATTACK_FACTOR = 1.15F;

    @Override
    public String toString() {
        return "lights_on_stage";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);

        World world = entityPlayer.world;
        if (world.isRemote) {
            return;
        }

        float bladeAttack = ItemSlashBlade.BaseAttackModifier.get(tag);
        float damage = BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        EntityStageLight stage = new EntityStageLight(world, entityPlayer, damage);
        world.spawnEntity(stage);

        if (world instanceof WorldServer) {
            openingSparkles((WorldServer) world, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ);
        }
        world.playSound(null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.15F, 1.75F);
        world.playSound(null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 1.1F);
    }

    private static void openingSparkles(WorldServer world, double x, double y, double z) {
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0D * i / 18.0D;
            double radius = 0.9D + i * 0.12D;
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    x + Math.cos(angle) * radius,
                    y + 0.12D,
                    z + Math.sin(angle) * radius,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
