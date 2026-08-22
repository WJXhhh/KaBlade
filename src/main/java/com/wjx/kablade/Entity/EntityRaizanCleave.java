package com.wjx.kablade.Entity;

import com.wjx.kablade.SlashBlade.blades.honkaip2.DomainOfSanction;
import com.wjx.kablade.SlashBlade.specialattack.RaizanCleaveTimeline;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 天殛之境服务端伤害时间轴及客户端渲染锚点。 */
public class EntityRaizanCleave extends Entity {
    private static final DataParameter<Integer> OWNER=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> SEED_HIGH=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> SEED_LOW=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> START_HIGH=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> START_LOW=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.VARINT);
    private static final DataParameter<Float> TX=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.FLOAT);
    private static final DataParameter<Float> TY=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.FLOAT);
    private static final DataParameter<Float> TZ=EntityDataManager.createKey(EntityRaizanCleave.class,DataSerializers.FLOAT);
    private static final Set<UUID> ACTIVE=new HashSet<UUID>();
    private UUID ownerUuid; private float totalDamage; private double castY; private int nextHit;

    public EntityRaizanCleave(World world){super(world);setSize(.2F,.2F);noClip=true;}
    public EntityRaizanCleave(World world,EntityPlayer owner,Vec3d target,float yaw,float damage){this(world);ownerUuid=owner.getUniqueID();totalDamage=damage;castY=owner.posY;setPosition(owner.posX,owner.posY,owner.posZ);rotationYaw=yaw;prevRotationYaw=yaw;dataManager.set(OWNER,owner.getEntityId());long seed=world.rand.nextLong()^ownerUuid.getMostSignificantBits(),start=world.getTotalWorldTime();dataManager.set(SEED_HIGH,(int)(seed>>>32));dataManager.set(SEED_LOW,(int)seed);dataManager.set(START_HIGH,(int)(start>>>32));dataManager.set(START_LOW,(int)start);dataManager.set(TX,(float)(target.x-posX));dataManager.set(TY,(float)(target.y-posY));dataManager.set(TZ,(float)(target.z-posZ));ACTIVE.add(ownerUuid);}
    public static boolean isCasting(EntityLivingBase owner){return owner!=null&&ACTIVE.contains(owner.getUniqueID());}
    protected void entityInit(){dataManager.register(OWNER,-1);dataManager.register(SEED_HIGH,0);dataManager.register(SEED_LOW,0);dataManager.register(START_HIGH,0);dataManager.register(START_LOW,0);dataManager.register(TX,0F);dataManager.register(TY,0F);dataManager.register(TZ,0F);}
    public int getOwnerId(){return dataManager.get(OWNER);} public long getSeed(){return ((long)dataManager.get(SEED_HIGH)<<32)|(dataManager.get(SEED_LOW)&0xFFFFFFFFL);}
    public Vec3d getTargetAnchor(){return new Vec3d(posX+dataManager.get(TX),posY+dataManager.get(TY),posZ+dataManager.get(TZ));}
    public long getStartGameTime(){return ((long)dataManager.get(START_HIGH)<<32)|(dataManager.get(START_LOW)&0xFFFFFFFFL);}
    public float getRenderAge(float partial){long start=getStartGameTime();return start==0?ticksExisted+partial:(float)(world.getTotalWorldTime()-start)+partial;}
    public float getReferenceFrame(float partial){return RaizanCleaveTimeline.referenceFrame(getRenderAge(partial));}

    @Override public void onUpdate(){super.onUpdate();if(world.isRemote)return;EntityPlayer owner=getOwner();
        if(owner==null||!owner.isEntityAlive()||!stillHolding(owner)||Math.abs(owner.posY-castY)>3){setDead();return;}
        setPosition(owner.posX,owner.posY,owner.posZ);motionX=motionZ=0;owner.motionX=owner.motionZ=0;owner.rotationYaw=rotationYaw;owner.rotationYawHead=rotationYaw;owner.renderYawOffset=rotationYaw;owner.velocityChanged=true;
        playTimelineSounds((WorldServer)world);
        while(nextHit<RaizanCleaveTimeline.HIT_TICKS.length&&ticksExisted>=RaizanCleaveTimeline.HIT_TICKS[nextHit])resolveHit((WorldServer)world,owner,nextHit++);
        if(ticksExisted>=RaizanCleaveTimeline.DURATION_TICKS)setDead();
    }
    private EntityPlayer getOwner(){Entity e=world.getEntityByID(getOwnerId());if(e instanceof EntityPlayer&&(ownerUuid==null||ownerUuid.equals(e.getUniqueID()))){ownerUuid=e.getUniqueID();return(EntityPlayer)e;}return null;}
    private boolean stillHolding(EntityPlayer owner){ItemStack stack=owner.getHeldItemMainhand();if(!(stack.getItem() instanceof ItemSlashBlade)||!stack.hasTagCompound())return false;return ItemSlashBlade.SpecialAttackType.get(stack.getTagCompound())==466&&DomainOfSanction.NAME.equals(ItemSlashBladeNamed.CurrentItemName.get(stack.getTagCompound()));}
    private void resolveHit(WorldServer server,EntityPlayer owner,int index){Vec3d center=getTargetAnchor();Vec3d forward=forward(),left=new Vec3d(forward.z,0,-forward.x);double scan=index<2?6.5:15;AxisAlignedBB box=new AxisAlignedBB(center.x-scan,center.y-2.5,center.z-scan,center.x+scan,center.y+6.5,center.z+scan);
        List<Entity> raw=world.getEntitiesInAABBexcluding(this,box,e->TargetingUtil.canSelectForDamage(owner,e));List<Entity> targets=new ArrayList<Entity>();
        for(Entity e:TargetingUtil.getDistinctDamageTargets(raw)){Vec3d p=e.getEntityBoundingBox().getCenter(),off=p.subtract(center);boolean inside=index<2?off.x*off.x+off.z*off.z<=42.25:Math.abs(off.dotProduct(left))<=10&&Math.abs(off.dotProduct(forward))<=5;if(inside)targets.add(e);}
        if(targets.isEmpty()){server.spawnParticle(EnumParticleTypes.END_ROD,center.x,center.y,center.z,index<2?8:5,.65,.6,.65,.05);return;}
        float damage=totalDamage*RaizanCleaveTimeline.DAMAGE_WEIGHTS[index];
        for(Entity receiver:targets){EntityLivingBase target=TargetingUtil.getSelectionTarget(receiver);if(target==null)continue;target.hurtResistantTime=0;receiver.attackEntityFrom(DamageSource.causePlayerDamage(owner).setDamageBypassesArmor(),damage);target.hurtResistantTime=0;target.addPotionEffect(new PotionEffect(PotionInit.PARALY,Math.max(1,RaizanCleaveTimeline.DURATION_TICKS-ticksExisted+1),12));target.getEntityData().setBoolean("dizui",true);target.getEntityData().setInteger("dizuitime",300);owner.onCriticalHit(target);if(index==8){target.motionX=target.motionX*.35+left.x*.35;target.motionY=target.motionY*.35+.12;target.motionZ=target.motionZ*.35+left.z*.35;target.velocityChanged=true;}}
        server.spawnParticle(EnumParticleTypes.END_ROD,center.x,center.y,center.z,index<2?28:18,index<2?1.25:2.2,1.1,index<2?1.25:2.2,.05);server.spawnParticle(EnumParticleTypes.CRIT_MAGIC,center.x,center.y,center.z,index<2?14:9,1.25,.9,1.25,.09);
        world.playSound(null,center.x,center.y,center.z,index<2?SoundEvents.ENTITY_LIGHTNING_IMPACT:SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,index<2?1.08F:.82F,1.24F+index*.055F);
        if(index==8)world.playSound(null,center.x,center.y,center.z,SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.PLAYERS,.82F,1.62F);
    }
    private void playTimelineSounds(WorldServer server){Vec3d a=getTargetAnchor();if(ticksExisted==6||ticksExisted==45)world.playSound(null,posX,posY+1.2,posZ,SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,.62F,ticksExisted==6?1.48F:1.18F);else if(ticksExisted==14||ticksExisted==50)world.playSound(null,posX,posY+1.2,posZ,SoundEvents.ENTITY_ENDERDRAGON_FLAP,SoundCategory.PLAYERS,.78F,1.62F);else if(ticksExisted==89||ticksExisted==96)world.playSound(null,posX,posY+1.1,posZ,SoundEvents.ITEM_ARMOR_EQUIP_IRON,SoundCategory.PLAYERS,.55F,ticksExisted==89?1.52F:1.82F);else if(ticksExisted==69)world.playSound(null,a.x,a.y,a.z,SoundEvents.ENTITY_LIGHTNING_THUNDER,SoundCategory.PLAYERS,1.08F,1.28F);}
    private Vec3d forward(){float y=rotationYaw*.017453292F;return new Vec3d(-MathHelper.sin(y),0,MathHelper.cos(y)).normalize();}
    @Override public void setDead(){if(ownerUuid!=null)ACTIVE.remove(ownerUuid);super.setDead();}
    protected void readEntityFromNBT(NBTTagCompound n){if(n.hasUniqueId("Owner"))ownerUuid=n.getUniqueId("Owner");totalDamage=n.getFloat("Damage");castY=n.getDouble("CastY");}
    protected void writeEntityToNBT(NBTTagCompound n){if(ownerUuid!=null)n.setUniqueId("Owner",ownerUuid);n.setFloat("Damage",totalDamage);n.setDouble("CastY",castY);}
    @Override public AxisAlignedBB getRenderBoundingBox(){return getEntityBoundingBox().union(new AxisAlignedBB(getPositionVector(),getTargetAnchor())).grow(14,8,14);}
    @Override public boolean canBeCollidedWith(){return false;}
}
