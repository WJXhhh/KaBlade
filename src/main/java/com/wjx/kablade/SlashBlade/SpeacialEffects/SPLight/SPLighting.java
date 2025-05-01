package com.wjx.kablade.SlashBlade.SpeacialEffects.SPLight;

import com.wjx.kablade.ExSA.entity.EntityLightningSword;
import com.wjx.kablade.ExSA.entity.EntityPhantomSwordEx;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class SPLighting implements ISpecialEffect {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return 1;
    }

    @Override
    public String getEffectKey() {
        return "SPLighting";
    }

    /*
    public void doAddAttack(ItemStack stack, EntityPlayer player, ItemSlashBlade.ComboSequence setCombo) {
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
        World world = player.world;
        int cost = -2;
        if (!ItemSlashBlade.ProudSoul.tryAdd(tag, Integer.valueOf(cost), false)) {
            ItemSlashBlade.damageItem(stack, 5, player);
        } else {
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0) {
                Entity tmp = world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(player) < 30.0F) {
                    target = tmp;
                }
            }

            if (target == null) {
                target = this.getEntityToWatch(player);
            }

            if (target != null && !target.isDead && !world.isRemote) {
                float baseModif = ((ItemSlashBlade)stack.getItem()).getBaseAttackModifiers(tag);
                int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
                float magicDamage = baseModif * (float)(level / 20);
                int rank = StylishRankManager.getStylishRank(player);
                if (5 <= rank) {
                    magicDamage *= 2.0F;
                }

                EntityPhantomSwordEx entityDrive = new EntityLightningSword(world, player, magicDamage, 90.0F);
                if (entityDrive != null) {
                    entityDrive.setColor(16766720);
                    entityDrive.setInterval(7);
                    entityDrive.setLifeTime(15);
                    entityDrive.setRoll(90.0F);
                    entityDrive.setTargetEntityId(target.getEntityId());
                    world.spawnEntity(entityDrive);
                }
            }

        }
    }

    private Entity getEntityToWatch(EntityPlayer player) {
        World world = player.worldObj;
        Entity target = null;

        for(int dist = 2; dist < 20; dist += 2) {
            AxisAlignedBB bb = player.boundingBox.copy();
            Vec3 vec = player.getLookVec();
            vec = vec.normalize();
            bb = bb.expand(2.0D, 0.25D, 2.0D);
            bb = bb.offset(vec.xCoord * (double)dist, vec.yCoord * (double)dist, vec.zCoord * (double)dist);
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, bb, ItemSlashBlade.AttackableSelector);
            float distance = 30.0F;
            Iterator var9 = list.iterator();

            while(var9.hasNext()) {
                Entity curEntity = (Entity)var9.next();
                float curDist = curEntity.getDistanceToEntity(player);
                if (curDist < distance) {
                    target = curEntity;
                    distance = curDist;
                }
            }

            if (target != null) {
                break;
            }
        }

        return target;
    }*/
}
