package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
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

            }


                ItemSlashBlade blade = (ItemSlashBlade) itemStack.getItem();
                float baseAttack= blade.getBaseAttackModifiers(tag);
                float magicDamage = baseAttack;
                magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),6f);
                EntityDriveAdd entityDrive = new EntityDriveAdd(world,entityPlayer,2.5f*magicDamage,false,0f-ItemSlashBlade.ComboSequence.Battou.swingDirection);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_R,1f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_G,0f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_B,0f);
                entityDrive.scaleX=40f;
                entityDrive.scaleY=10.25f;
                entityDrive.scaleZ=10f;
                entityDrive.setInitialSpeed(1.05f);
                entityDrive.setLifeTime(50);
                entityDrive.getDataManager().set(EntityDriveAdd.PARTICLE_STYLE,"FLAME");
                world.spawnEntity(entityDrive);

        }
    }
}
