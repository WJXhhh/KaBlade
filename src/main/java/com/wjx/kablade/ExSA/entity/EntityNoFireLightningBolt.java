package com.wjx.kablade.ExSA.entity;

import com.wjx.kablade.ExSA.ability.EnderTeleportCanceller;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;

public class EntityNoFireLightningBolt extends EntityLightningBolt {

    private int lightningState;
    private int boltLivingTime;
    private Entity target;
    private EntitySelectors selector;
    private static String doFireTickTmp = "true";
    protected float AttackLevel = 0.0f;
    protected Entity thrower;

    public static World StoreDoFireTick(World world) {
        doFireTickTmp = world.getGameRules().getString("doFireTick");
        world.getGameRules().setOrCreateGameRule("doFireTick", "false");
        return world;
    }

    public static World RestoreDoFireTick(World world) {
        world.getGameRules().setOrCreateGameRule("doFireTick", doFireTickTmp);
        return world;
    }

    public EntityNoFireLightningBolt(World p_i1703_1_, double p_i1703_2_, double p_i1703_4_, double p_i1703_6_, Entity target, EntitySelectors selector, Entity thrower) {
        super(EntityNoFireLightningBolt.StoreDoFireTick(p_i1703_1_), p_i1703_2_, p_i1703_4_, p_i1703_6_,false);
        EntityNoFireLightningBolt.RestoreDoFireTick(p_i1703_1_);
        this.lightningState = 2;
        this.boltLivingTime = this.rand.nextInt(3) + 1;
        this.target = target;
        this.selector = selector;
        this.thrower = thrower;
    }

    @Override
    public void onUpdate() {
        if (this.thrower == null) {
            this.setDead();
            return;
        }
        this.onEntityUpdate();
        if (this.lightningState == 2) {

            this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.AMBIENT,5000f,0.8f + this.rand.nextFloat() * 0.2f,true);
            this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,2f,0.5f + this.rand.nextFloat() * 0.2f,true);
        }
        --this.lightningState;
        if (this.lightningState < 0) {
            if (this.boltLivingTime == 0) {
                this.setDead();
            } else if (this.lightningState < -this.rand.nextInt(10)) {
                --this.boltLivingTime;
                this.lightningState = 1;
                this.boltVertex = this.rand.nextLong();
            }
        }
        if (this.lightningState >= 0) {
            ItemStack stack;
            double d0 = 3.0;
            List<Entity> list = this.world.getEntitiesInAABBexcluding(this,new AxisAlignedBB(this.posX - d0, this.posY - d0, this.posZ - d0, this.posX + d0, this.posY + 6.0 + d0, this.posZ + d0), EntitySelectorAttackable.getInstance());
            if (this.target.isEntityAlive()) {
                list.add(this.target);
            }
            if ((stack = ((EntityPlayer)this.thrower).getHeldItem(EnumHand.MAIN_HAND)) != null && stack.getItem() instanceof ItemSlashBlade) {
                NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
                int rank = StylishRankManager.getStylishRank(this.thrower);
                int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
                float magicDamage = 1.0f + ItemSlashBlade.AttackAmplifier.get(tag).floatValue() * (0.3f + (float)level / 10.0f + 0.06f * (float)rank);
                for (int l = 0; l < list.size(); ++l) {
                    Entity entity;
                    if (this.world.isRemote || (entity = (Entity)list.get(l)) == null || entity.isDead) continue;
                    if (entity.hurtResistantTime != 3) {
                        entity.hurtResistantTime = 3;
                    }
                    EnderTeleportCanceller.setTeleportCancel(entity, 600);
                    if (ForgeEventFactory.onEntityStruckByLightning(entity, this)) continue;
                    entity.attackEntityFrom(DamageSource.IN_FIRE, magicDamage);
                    entity.setFire(10);
                }
            }
        }
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
    }
}
