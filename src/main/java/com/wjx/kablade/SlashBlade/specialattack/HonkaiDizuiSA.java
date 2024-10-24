package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageRemoteLighting;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import com.wjx.kablade.Entity.EntitySummonSwordFree;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;
import java.util.Random;

public class HonkaiDizuiSA extends SpecialAttackBase {
    @Override
    public String toString() {
        return "dizuijiu";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {


            World world = entityPlayer.world;
            if(!world.isRemote)
        {
            for(int i =0;i<3;i++)
            {
                float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),25f);

                EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, 50f + extraDamage, true, 30f);
                EntityDriveAdd entityDrive2 = new EntityDriveAdd(world, entityPlayer, 50f + extraDamage, true, 60f);
                entityDrive.setPosition(entityPlayer.posX,entityPlayer.posY+entityPlayer.eyeHeight,entityPlayer.posZ);
                entityDrive2.setPosition(entityPlayer.posX,entityPlayer.posY+entityPlayer.eyeHeight,entityPlayer.posZ);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_R,0.9f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_G,0.1f);
                entityDrive.getDataManager().set(EntityDriveAdd.COLOR_B,0.9f);
                entityDrive2.getDataManager().set(EntityDriveAdd.COLOR_R,0.9f);
                entityDrive2.getDataManager().set(EntityDriveAdd.COLOR_G,0.1f);
                entityDrive2.getDataManager().set(EntityDriveAdd.COLOR_B,0.9f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_X,2f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Y,2f);
                entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Z,2f);
                entityDrive2.getDataManager().set(EntityDriveAdd.SCALE_X,2f);
                entityDrive2.getDataManager().set(EntityDriveAdd.SCALE_Y,2f);
                entityDrive2.getDataManager().set(EntityDriveAdd.SCALE_Z,2f);
                entityDrive.getDataManager().set(EntityDriveAdd.PL_PARTICAL,false);
                entityDrive2.getDataManager().set(EntityDriveAdd.PL_PARTICAL,false);
                entityDrive.setInitialSpeed(0.8f);
                entityDrive2.setInitialSpeed(0.8f);
                
                world.spawnEntity(entityDrive);
                world.spawnEntity(entityDrive2);
            }
            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
            bb = bb.grow(32.0D, 5.0D, 32.0D);
            bb = bb.offset(entityPlayer.motionX, entityPlayer.motionY, entityPlayer.motionZ);
            List<Entity> list = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, bb, input -> input != entityPlayer && input.isEntityAlive());
            if (list.size()!=0){
                for (Entity entity: list){
                    if (entity instanceof EntityLivingBase){
                        if(entity.getEntityData().getBoolean("dizui"))
                        {
                            entity.getEntityData().setBoolean("dizui",false);
                            entity.getEntityData().setBoolean("dizuialive",true);
                        }
                    }
                }

            }
            if(entityPlayer.getAttackingEntity()!=null ){
                entityPlayer.getAttackingEntity().addPotionEffect(new PotionEffect(MobEffects.WITHER,100,1));
            }
            float extraDamage = (float)MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),20f);
            EntitySummonSwordFree s1 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,0f);
            EntitySummonSwordFree s2 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,45f);
            EntitySummonSwordFree s3 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,90f);
            EntitySummonSwordFree s4 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,135f);
            EntitySummonSwordFree s5 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,180f);
            EntitySummonSwordFree s6 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,225f);
            EntitySummonSwordFree s7 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,270f);
            EntitySummonSwordFree s8 = new EntitySummonSwordFree(world,entityPlayer,20 + extraDamage,entityPlayer.posX,entityPlayer.posY + 0.7d,entityPlayer.posZ,0,315f);
            s1.setColor(0xFF1493);
            s2.setColor(0xFF1493);
            s3.setColor(0xFF1493);
            s4.setColor(0xFF1493);
            s5.setColor(0xFF1493);
            s6.setColor(0xFF1493);
            s7.setColor(0xFF1493);
            s8.setColor(0xFF1493);
            world.spawnEntity(s1);
            world.spawnEntity(s2);
            world.spawnEntity(s3);
            world.spawnEntity(s4);
            world.spawnEntity(s5);
            world.spawnEntity(s6);
            world.spawnEntity(s7);
            world.spawnEntity(s8);
        }
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
        bb = bb.grow(16.0D, 5.0D, 16.0D);
        bb = bb.offset(entityPlayer.motionX, entityPlayer.motionY, entityPlayer.motionZ);
        float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),5f);
        List<Entity> list = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, bb, input -> input != entityPlayer && input.isEntityAlive());
        if (!list.isEmpty()){
            for (Entity entity: list){
                if (entity instanceof EntityLivingBase){
                   world.spawnEntity(new EntityLightningBolt(world,entity.posX,entity.posY,entity.posZ,true));
                   entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),5 + extraDamage);
                   double x = entity.posX;
                    double y = entity.posY;
                    double z = entity.posZ;
                    for (int i = 0; i < 20; ++i)
                    {
                        Random r1 =new Random();
                        Random r2 =new Random(r1.nextLong());
                        int state1;
                        int state2;
                        if (r1.nextBoolean()){
                            state1 = 1;
                        }
                        else state1 = -1;
                        if (r2.nextBoolean()){
                            state2 = 1;
                        }
                        else state2 = -1;
                        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, x + (world.rand.nextDouble() * 2 * state1), y + world.rand.nextDouble() * (double)entityPlayer.height, z + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (world.rand.nextDouble() * 2 * state1), y + world.rand.nextDouble() * (double)entityPlayer.height, z + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
    }
}
