package com.wjx.kablade.util.slash;

import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class SETranscend implements ISpecialEffect, IRemovable {
    private static final String EffectKey = "SETranscend";
    private int interval=0;

    public int getDefaultRequiredLevel(){
        return 1000;
    }

    public boolean canCopy(ItemStack stack){
        return false;
    }

    public boolean canRemoval(ItemStack stack){
        return false;
    }

    public String getEffectKey(){
        return EffectKey;
    }

    public void register(){
        SlashBladeHooks.EventBus.register(this);
    }

    private boolean useBlade(ItemSlashBlade.ComboSequence sequence){
        if(sequence.useScabbard) return false;
        if(sequence == ItemSlashBlade.ComboSequence.None) return false;
        if(sequence == ItemSlashBlade.ComboSequence.Noutou) return false;
        return true;
    }

    @SubscribeEvent
    public void onImpactEffectEvent(SlashBladeEvent.ImpactEffectEvent event){

        if(!useBlade(event.sequence)) return;

        if(!SpecialEffects.isPlayer(event.user)) return;
        EntityPlayer player = (EntityPlayer) event.user;

        switch (SpecialEffects.isEffective(player, event.blade, this)){
            case None:
                if(interval>0) interval--;
                return;
            case Effective:
                if(interval<=0) {
                    ItemSlashBlade.damageItem(event.blade, 5, player);
                    float damage=5.0f+ ItemSlashBladeNamed.RepairCount.get(event.blade.getTagCompound())/10;
                    player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,150,3));
                    player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION,100,3));
                    if(damage>12) damage=12;
                    if(event.target.getRNG().nextInt(2) != 0) return;
                    if(event.target!=null) {
                        event.target.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (event.target.getRNG().nextFloat() - event.target.getRNG().nextFloat()) * 0.4F);
                        event.target.world.createExplosion(event.user, event.target.posX, event.target.posY,  event.target.posZ, damage, false);
                    }
                    interval+=7;
                }else {
                    interval--;
                }
                aoeAttack(player, 10086);
                break;
            case NonEffective:
                if(interval>0) interval--;
                if(player.getRNG().nextInt(4) != 0){
                    player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,2));
                    player.addPotionEffect(new PotionEffect(MobEffects.SPEED,100,2));
                    return;
                }
                break;
        }
        player.onEnchantmentCritical(event.target);

    }

    @SubscribeEvent
    public void onUpdateItemSlashBlade(SlashBladeEvent.OnUpdateEvent event){

        if(!SpecialEffects.isPlayer(event.entity)) return;
        EntityPlayer player = (EntityPlayer) event.entity;

        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(event.blade);
        if(!useBlade(ItemSlashBlade.getComboSequence(tag))) return;

        switch (SpecialEffects.isEffective(player,event.blade,this)){
            case None:
                return;
            case NonEffective:
                if(player.getRNG().nextInt(4) != 0) return;
                break;
            case Effective:
                if(player.getHeldItemMainhand().getItem() instanceof TranscendSlash){
                    int rankPoint = StylishRankManager.getTotalRankPoint(player);
                    int aRankPoint = (int) (StylishRankManager.RankRange * 7D);
                    int rankAmount = aRankPoint - rankPoint;
                    StylishRankManager.addRankPoint(player,rankAmount);
                }
            return;
        }
    }

    public void aoeAttack(EntityPlayer player, int damage) {
        World world = player.world;
        for (int dist = 2; dist < 14; dist += 2) {
            AxisAlignedBB bb = player.getEntityBoundingBox();
            Vec3d vec = player.getLookVec();
            vec = vec.normalize();
            bb = bb.expand(2.0f, 0.25f, 2.0f);
            bb = bb.offset(vec.x * (float) dist, vec.y * (float) dist, vec.z * (float) dist);
            List<Entity> list = world.getEntitiesInAABBexcluding(player, bb, EntitySelectorAttackable.getInstance());
            for (Entity curEntity : list) {
                if (curEntity instanceof EntityLivingBase) {
                    //world.spawnEntity(new EntityLightningRainbow(world,curEntity.posX,curEntity.posY,curEntity.posZ,false));
                    ((EntityLivingBase) curEntity).setHealth(((EntityLivingBase) curEntity).getHealth() / 2);
                    ((EntityLivingBase) curEntity).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 250, 4));
                }
                curEntity.setFire(damage);
            }
        }
    }
}
