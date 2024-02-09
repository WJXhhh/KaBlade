package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AL_YanjiFZ extends SpecialAttackBase {
    @Override
    public String toString() {
        return "fuzhan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        World world = entityPlayer.world;
        if(!world.isRemote)
        {
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0) {
                Entity tmp = world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(entityPlayer) < 100.0F && tmp instanceof EntityLivingBase) {
                    target = tmp;
                }
            }
            if (target==null){
                target=SATool.getEntityToWatch(entityPlayer);
                if (target!=null){

                    ItemSlashBlade blade = (ItemSlashBlade) itemStack.getItem();
                    float baseAttack= blade.getBaseAttackModifiers(tag);
                    EntityDriveAdd entityDrive = new EntityDriveAdd(world,entityPlayer,1.5f*baseAttack,false,0f-ItemSlashBlade.ComboSequence.Battou.swingDirection);
                    entityDrive.colors=0xFF0000;
                    entityDrive.scaleX=40f;
                    entityDrive.scaleY=10.25f;
                    entityDrive.scaleZ=10f;
                    entityDrive.setInitialSpeed(1.5f);
                    entityDrive.setLifeTime(50);
                    entityDrive.particleO="flame";
                    world.spawnEntity(entityDrive);
                }
            }
        }
    }
}
