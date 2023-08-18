package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.event.KillEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;

public class Qi extends SpecialAttackBase {
    public String toString() {
        return "magic";
    }

    public void doSpacialAttack(ItemStack stack, EntityPlayer player) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        World world = player.world;
        boolean op = false;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
        Vec3d vec3 = player.getLook(1.0F);
        double dx = vec3.x * 3.0D;
        double dy = (double)player.getEyeHeight() + vec3.y * 3.0D;
        double dz = vec3.z * 3.0D;
        List<Entity> list11 = player.world.getEntitiesInAABBexcluding(player, player.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D).offset(dx, dy, dz), input -> input != player && input instanceof  EntityLivingBase );
        //list11.remove(player);
        if (!list11.isEmpty()) {
            for (Entity entity : list11) {
                if(entity instanceof EntityPlayer){
                    KillEvent.killplayer((EntityPlayer) entity, player);
                }
                else {
                   // server.getPlayerList().sendMessage(new TextComponentString(entity.toString()));
                    KillEvent.killutil((EntityLivingBase) entity, player);
                }
            }
        }else {
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0) {
                Entity tmp = world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(player) < 100.0F && tmp instanceof EntityLivingBase) {
                    target = tmp;
                }
            }
            if(target != null)
            {
                if(target instanceof EntityPlayer){
                    KillEvent.killplayer((EntityPlayer) target, player);
                }
                else {
                    // server.getPlayerList().sendMessage(new TextComponentString(entity.toString()));
                    KillEvent.killutil((EntityLivingBase) target, player);
                }
            }
        }

    }

    private void spawnParticle(World world, Entity target) {
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + (double)target.height, target.posZ, 3.0D, 3.0D, 3.0D);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX + 1.0D, target.posY + (double)target.height + 1.0D, target.posZ, 3.0D, 3.0D, 3.0D);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + (double)target.height + 0.5D, target.posZ + 1.0D, 3.0D, 3.0D, 3.0D);
    }
}
