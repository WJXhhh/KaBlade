package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import mods.flammpfeil.slashblade.specialattack.SakuraEnd;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class HonKaiVorpalHole extends SpecialAttackBase {
    @Override
    public String toString() {
        return "vorpal_hoe";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        SakuraEnd end = new SakuraEnd();
        end.doSpacialAttack(itemStack,entityPlayer);
        World world = entityPlayer.getEntityWorld();
        EntityVorpalBlackHole hole = new EntityVorpalBlackHole(world,entityPlayer,entityPlayer.posX,entityPlayer.posY,entityPlayer.posZ);
        world.spawnEntity(hole);
    }
}
