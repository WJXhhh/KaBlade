package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageRaidenCycloneEnd;
import com.wjx.kablade.network.MessageRaidenCycloneStart;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** 重磁暴·斩的五秒磁力连击。 */
public class RaidenCyclone extends SpecialAttackBase {
    private static final Map<UUID,Cast> ACTIVE=new HashMap<UUID,Cast>(); private static final AtomicLong IDS=new AtomicLong(1);
    public RaidenCyclone(){MinecraftForge.EVENT_BUS.register(this);} public String toString(){return "raiden_cyclone";}
    public void doSpacialAttack(ItemStack blade,EntityPlayer player){if(player.world.isRemote||ACTIVE.containsKey(player.getUniqueID()))return;Entity target=TargetingUtil.resolveTarget(player,blade,8,8,5);Vec3d origin=player.getPositionVector(),point=target==null?origin.add(flatLook(player).scale(1.6)):target.getEntityBoundingBox().getCenter();double ref=Math.atan2(-.40,-1.54),worldAngle=Math.atan2(origin.z-point.z,origin.x-point.x);float rotation=(float)(worldAngle-ref);float extra=MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()),10);float total=3F*1.35F*(60+2*extra);long id=IDS.getAndIncrement(),seed=player.world.rand.nextLong()^player.getUniqueID().getMostSignificantBits()^id;Cast cast=new Cast(id,player.dimension,player.getUniqueID(),target==null?null:target.getUniqueID(),target==null?-1:target.getEntityId(),player.world.getTotalWorldTime(),seed,origin,point,rotation,total);ACTIVE.put(player.getUniqueID(),cast);BladeProxy.EMPulsar.activate(player,blade);Main.PACKET_HANDLER.sendToDimension(new MessageRaidenCycloneStart(id,player.getEntityId(),cast.targetId,cast.start,seed,origin.x,origin.y,origin.z,point.x,point.y,point.z,rotation),player.dimension);player.world.playSound(null,origin.x,origin.y,origin.z,SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,SoundCategory.PLAYERS,.85F,1.85F);player.world.playSound(null,origin.x,origin.y,origin.z,SoundEvents.ENTITY_ENDERDRAGON_FLAP,SoundCategory.PLAYERS,.72F,1.35F);((WorldServer)player.world).spawnParticle(EnumParticleTypes.END_ROD,origin.x,origin.y+player.height*.55,origin.z,28,.65,.75,.65,.035);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||ACTIVE.isEmpty())return;MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();if(server==null)return;Iterator<Map.Entry<UUID,Cast>> it=ACTIVE.entrySet().iterator();while(it.hasNext()){Map.Entry<UUID,Cast> en=it.next();if(tick(server,en.getValue()))it.remove();}}
    private boolean tick(MinecraftServer server,Cast c){WorldServer world=server.getWorld(c.dimension);EntityPlayer user=server.getPlayerList().getPlayerByUUID(c.owner);if(world==null||user==null||!user.isEntityAlive()||user.world!=world||!holding(user)){if(user!=null){release(user,c);end(c,MessageRaidenCycloneEnd.OWNER_LOST);}return true;}long elapsed=world.getTotalWorldTime()-c.start;if(elapsed>RaidenCycloneTimeline.DURATION_TICKS){release(user,c);end(c,MessageRaidenCycloneEnd.COMPLETE);return true;}if(elapsed>84)release(user,c);Entity target=null;if(c.targetUuid!=null){target=world.getEntityByID(c.targetId);if(target==null||!target.isEntityAlive()||!c.targetUuid.equals(target.getUniqueID())||!TargetingUtil.canSelectForDamage(user,target)||target.getPositionVector().squareDistanceTo(c.lastTarget)>144){release(user,c);end(c,MessageRaidenCycloneEnd.TARGET_LOST);return true;}c.lastTarget=target.getEntityBoundingBox().getCenter();}float seconds=elapsed/20F;if(elapsed<=84)thrust(world,user,target==null?c.virtualTarget:target.getEntityBoundingBox().getCenter(),c.rotation,seconds);while(c.next<RaidenCycloneTimeline.HIT_TICKS.length&&elapsed>=RaidenCycloneTimeline.HIT_TICKS[c.next]){if(target!=null)hit(world,user,target,c,c.next);c.next++;}return false;}
    private void thrust(WorldServer world,EntityPlayer user,Vec3d target,float rotation,float seconds){RaidenCycloneTimeline.Pose p=RaidenCycloneTimeline.sample(seconds);double cos=Math.cos(rotation),sin=Math.sin(rotation),ox=p.x*cos-p.z*sin,oz=p.x*sin+p.z*cos;Vec3d error=new Vec3d(target.x+ox-user.posX,0,target.z+oz-user.posZ).scale(.45);if(error.lengthSquared()>.65*.65)error=error.normalize().scale(.65);double vx=user.motionX+error.x,vz=user.motionZ+error.z,h=Math.sqrt(vx*vx+vz*vz);if(h>1.15){vx=vx/h*1.15;vz=vz/h*1.15;}AxisAlignedBB box=user.getEntityBoundingBox();if(!world.getCollisionBoxes(user,box.offset(vx,0,0)).isEmpty())vx=user.motionX;if(!world.getCollisionBoxes(user,box.offset(0,0,vz)).isEmpty())vz=user.motionZ;user.motionX=vx;user.motionZ=vz;user.velocityChanged=true;}
    private void release(EntityPlayer user,Cast c){if(c.released)return;user.motionX=user.motionZ=0;user.velocityChanged=true;c.released=true;}
    private void hit(WorldServer world,EntityPlayer user,Entity primary,Cast c,int index){boolean main=index>=5;Vec3d center=main?primary.getEntityBoundingBox().getCenter():user.getPositionVector().add(0,user.height*.48,0);double radius=main?4.5:3.25;if(main&&primary.getDistanceSq(user)>100)return;AxisAlignedBB box=new AxisAlignedBB(center.x-radius,center.y-3,center.z-radius,center.x+radius,center.y+3,center.z+radius);List<Entity> candidates=world.getEntitiesInAABBexcluding(user,box,e->TargetingUtil.canSelectForDamage(user,e)),selected=new ArrayList<Entity>();for(Entity e:TargetingUtil.getDistinctDamageTargets(candidates)){Vec3d pos=e.getEntityBoundingBox().getCenter();double dx=pos.x-center.x,dz=pos.z-center.z;if(dx*dx+dz*dz<=radius*radius)selected.add(e);}if(selected.isEmpty())return;float damage=c.damage*RaidenCycloneTimeline.DAMAGE_WEIGHTS[index];for(Entity e:selected){EntityLivingBase living=TargetingUtil.getSelectionTarget(e);if(living==null)continue;living.hurtResistantTime=0;e.attackEntityFrom(DamageSource.causePlayerDamage(user).setDamageBypassesArmor(),damage);living.hurtResistantTime=0;living.addPotionEffect(new PotionEffect(PotionInit.PARALY,100,3));user.onCriticalHit(living);world.spawnParticle(EnumParticleTypes.END_ROD,living.posX,living.posY+living.height*.55,living.posZ,main?18:10,living.width*.42,living.height*.32,living.width*.42,.085);}if(main?!c.mainPaid:!c.orbitPaid){user.getHeldItemMainhand().damageItem(1,user);if(main)c.mainPaid=true;else c.orbitPaid=true;}world.playSound(null,center.x,center.y,center.z,main?SoundEvents.ENTITY_LIGHTNING_THUNDER:SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,main?.72F:.52F,1.42F+index*.035F);}
    private boolean holding(EntityPlayer p){ItemStack s=p.getHeldItemMainhand();return s.getItem() instanceof ItemSlashBlade&&s.hasTagCompound()&&ItemSlashBlade.SpecialAttackType.get(s.getTagCompound())==467;}
    private void end(Cast c,byte reason){Main.PACKET_HANDLER.sendToDimension(new MessageRaidenCycloneEnd(c.id,reason),c.dimension);}
    private static Vec3d flatLook(EntityPlayer p){Vec3d v=p.getLookVec(),f=new Vec3d(v.x,0,v.z);return f.lengthSquared()<1E-6?new Vec3d(0,0,1):f.normalize();}
    private static final class Cast{final long id,start,seed;final int dimension,targetId;final UUID owner,targetUuid;final Vec3d origin,virtualTarget;Vec3d lastTarget;final float rotation,damage;int next;boolean orbitPaid,mainPaid,released;Cast(long id,int dim,UUID owner,UUID targetUuid,int targetId,long start,long seed,Vec3d origin,Vec3d target,float rotation,float damage){this.id=id;dimension=dim;this.owner=owner;this.targetUuid=targetUuid;this.targetId=targetId;this.start=start;this.seed=seed;this.origin=origin;virtualTarget=target;lastTarget=target;this.rotation=rotation;this.damage=damage;}}
}
