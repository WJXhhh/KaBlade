package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
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

import java.util.List;

/** 剑体始觉/念相了绝共用的服务端伤害时间轴与客户端渲染锚点。 */
public class EntityConceptualField extends Entity {
    public static final int LIFETIME = 62;
    public static final int MODE_ENLIGHTENMENT = 0;
    public static final int MODE_UNITY = 1;

    private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(
            EntityConceptualField.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(
            EntityConceptualField.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> MODE = EntityDataManager.createKey(
            EntityConceptualField.class, DataSerializers.VARINT);

    public EntityConceptualField(World world) {
        super(world);
        setSize(0.2F, 0.2F);
        noClip = true;
        setNoGravity(true);
        ignoreFrustumCheck = true;
    }

    public static EntityConceptualField spawn(World world, EntityPlayer owner, Vec3d center,
                                               float damage, int mode) {
        EntityConceptualField field = new EntityConceptualField(world);
        field.dataManager.set(OWNER_ID, owner.getEntityId());
        field.dataManager.set(DAMAGE, damage);
        field.dataManager.set(MODE, mode);
        field.rotationYaw = owner.rotationYaw;
        field.setPosition(center.x, center.y, center.z);
        world.spawnEntity(field);
        return field;
    }

    @Override protected void entityInit() {
        dataManager.register(OWNER_ID, -1);
        dataManager.register(DAMAGE, 1.0F);
        dataManager.register(MODE, MODE_ENLIGHTENMENT);
    }

    public int getMode() { return dataManager.get(MODE); }
    public float getBaseDamage() { return dataManager.get(DAMAGE); }
    public EntityLivingBase getOwner() {
        Entity entity = world.getEntityByID(dataManager.get(OWNER_ID));
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        EntityLivingBase owner = getOwner();
        if (owner == null || !owner.isEntityAlive()) { setDead(); return; }
        WorldServer server = (WorldServer) world;
        playSounds(server);
        spawnParticles(server);
        if (ticksExisted >= 8 && ticksExisted <= 32 && ((ticksExisted - 8) & 3) == 0) {
            float weight = ticksExisted == 8 ? 0.38F : ticksExisted == 32 ? 1.08F : 0.56F;
            hit(owner, getBaseDamage() * weight, ticksExisted == 32);
        }
        if (getMode() == MODE_UNITY && ticksExisted == 36) {
            hit(owner, getBaseDamage() * 1.20F, true);
        }
        if (ticksExisted >= LIFETIME) setDead();
    }

    private void hit(EntityLivingBase owner, float damage, boolean finisher) {
        AxisAlignedBB area = new AxisAlignedBB(posX - 8.2D, posY - 0.65D, posZ - 8.2D,
                posX + 8.2D, posY + 4.8D, posZ + 8.2D);
        List<Entity> found = world.getEntitiesInAABBexcluding(this, area,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        Vec3d forward = flatForward();
        Vec3d pullCenter = getPositionVector().add(forward.scale(2.15D)).add(0, 1.1D, 0);
        Vec3d fieldCenter = getPositionVector().add(0, 1.1D, 0);
        for (Entity receiver : TargetingUtil.getDistinctDamageTargets(found)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            Vec3d center = TargetingUtil.getClosestPointOnDamageBounds(receiver, fieldCenter);
            double horizontal = Math.sqrt(center.squareDistanceTo(new Vec3d(posX, center.y, posZ)));
            float falloff = MathHelper.clamp((float) (1.0D - horizontal / 11.07D), 0.62F, 1.0F);
            target.hurtResistantTime = 0;
            if (!receiver.attackEntityFrom(damageSource(owner), damage * falloff)) continue;
            spawnHitFeedback((WorldServer)world,target,finisher,
                    getMode()==MODE_UNITY&&ticksExisted==36);
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, finisher ? 38 : 26,
                    finisher ? 4 : 3, false, false));
            Vec3d motion;
            if (finisher) motion = forward.scale(0.52D).add(0, 0.32D, 0);
            else {
                Vec3d pull = pullCenter.subtract(center);
                motion = pull.lengthSquared() > 1.0E-6D ? pull.normalize().scale(0.13D) : Vec3d.ZERO;
                motion = motion.add(0, ticksExisted >= 20 ? 0.22D : 0.13D, 0);
            }
            target.motionX = target.motionX * (finisher ? 0.36D : 0.52D) + motion.x;
            target.motionY = target.motionY * (finisher ? 0.36D : 0.52D) + motion.y;
            target.motionZ = target.motionZ * (finisher ? 0.36D : 0.52D) + motion.z;
            target.velocityChanged = true;
        }
    }

    private void playSounds(WorldServer server) {
        if(ticksExisted==4){sound(server,SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,.86F,1.62F);sound(server,SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,.58F,1.94F);}
        else if(ticksExisted==8){sound(server,SoundEvents.ENTITY_ENDERDRAGON_FLAP,1.02F,1.74F);sound(server,SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,.82F,1.35F);}
        else if(ticksExisted==16||ticksExisted==24){sound(server,SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,.82F,1.18F);sound(server,SoundEvents.BLOCK_GLASS_BREAK,.70F,1.62F);}
        else if(ticksExisted==32){sound(server,SoundEvents.BLOCK_GLASS_BREAK,.94F,1.20F);sound(server,SoundEvents.ENTITY_ENDERDRAGON_FLAP,.86F,1.88F);}
        if(getMode()==MODE_UNITY&&ticksExisted==30){sound(server,SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,.72F,2F);sound(server,SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,.48F,1.72F);}
        if(getMode()==MODE_UNITY&&ticksExisted==36){sound(server,SoundEvents.BLOCK_END_PORTAL_SPAWN,.74F,1.82F);sound(server,SoundEvents.ENTITY_LIGHTNING_IMPACT,.66F,1.54F);sound(server,SoundEvents.BLOCK_GLASS_BREAK,.82F,1.96F);}
    }

    private void spawnParticles(WorldServer server) {
        if(ticksExisted>=6&&ticksExisted<=38){float open=smoother(MathHelper.clamp((ticksExisted-6F)/18F,0,1));int count=8+MathHelper.floor(open*22);
            for(int i=0;i<count;i++){double angle=rand.nextDouble()*Math.PI*2+ticksExisted*.36D,radius=.62D+open*4.85D+rand.nextDouble()*.48D,y=.10D+rand.nextDouble()*(.55D+open*2.20D);double tangent=.055D+open*.08D;
                redstone(server,posX+Math.cos(angle)*radius,posY+y,posZ+Math.sin(angle)*radius,.55F+.45F*open,.20F+.66F*open,1F);
                if((i&3)==0)server.spawnParticle(EnumParticleTypes.END_ROD,posX+Math.cos(angle)*radius,posY+y,posZ+Math.sin(angle)*radius,0,-Math.sin(angle)*tangent,.018D,Math.cos(angle)*tangent,.035D);}}
        if(ticksExisted>=8&&ticksExisted<=34&&(ticksExisted&1)==0){Vec3d f=flatForward(),right=new Vec3d(-f.z,0,f.x);for(int i=0;i<4;i++){double ahead=.8D+rand.nextDouble()*5.4D,side=(rand.nextDouble()-.5D)*5.8D,y=.45D+rand.nextDouble()*2.35D;Vec3d p=getPositionVector().add(f.scale(ahead)).add(right.scale(side)).add(0,y,0),motion=f.scale(.04D+rand.nextDouble()*.09D).add(right.scale((rand.nextDouble()-.5D)*.10D));server.spawnParticle(EnumParticleTypes.END_ROD,p.x,p.y,p.z,0,motion.x,.06D,motion.z,.035D);}}
        if(ticksExisted==8||ticksExisted==12||ticksExisted==16||ticksExisted==20||ticksExisted==24||ticksExisted==28||ticksExisted==32){Vec3d c=getPositionVector().add(flatForward().scale(2.35D)).add(0,1.25D,0);server.spawnParticle(EnumParticleTypes.END_ROD,c.x,c.y,c.z,ticksExisted==32?34:18,.78D,.58D,.78D,.16D);int n=ticksExisted==32?42:24;for(int i=0;i<n;i++)redstone(server,c.x+(rand.nextDouble()-.5D)*1.4D,c.y+(rand.nextDouble()-.5D)*.96D,c.z+(rand.nextDouble()-.5D)*1.4D,1F,.86F,1F);}
        if(getMode()==MODE_UNITY)spawnUnityParticles(server);
    }

    private void spawnUnityParticles(WorldServer server){if(ticksExisted>=8&&ticksExisted<=36&&(ticksExisted&1)==0){float gather=MathHelper.clamp((ticksExisted-30F)/6F,0,1);double radius=4.25D+(0.42D-4.25D)*gather*gather,spin=ticksExisted*(.24D+gather*.20D);Vec3d center=getPositionVector().add(flatForward().scale(2.15D)).add(0,1.1D,0);for(int i=0;i<3;i++){double angle=spin+i*Math.PI*2/3,x=center.x+Math.cos(angle)*radius,z=center.z+Math.sin(angle)*radius,y=center.y+Math.sin(angle*2)*.22D,tangent=.08D+gather*.12D;redstone(server,x,y,z,.95F,.86F,.63F);redstone(server,x,y,z,.74F,.76F,1F);server.spawnParticle(EnumParticleTypes.END_ROD,x,y,z,0,-Math.sin(angle)*tangent,.015D,Math.cos(angle)*tangent,.02D);}}
        if(ticksExisted==36){Vec3d c=getPositionVector().add(flatForward().scale(2.15D)).add(0,1.1D,0);for(int i=0;i<72;i++)redstone(server,c.x+(rand.nextDouble()-.5D)*1.84D,c.y+(rand.nextDouble()-.5D)*1.4D,c.z+(rand.nextDouble()-.5D)*1.84D,i%2==0?.95F:.74F,i%2==0?.86F:.76F,i%2==0?.63F:1F);server.spawnParticle(EnumParticleTypes.END_ROD,c.x,c.y,c.z,46,.78D,.58D,.78D,.20D);}}

    private void spawnHitFeedback(WorldServer server,EntityLivingBase target,boolean finisher,boolean unity){Vec3d c=target.getPositionVector().add(0,target.height*.56D,0);int crit=unity?22:finisher?18:9,dust=unity?30:finisher?24:12;server.spawnParticle(EnumParticleTypes.CRIT,c.x,c.y,c.z,crit,.32D,.28D,.32D,unity?.24D:finisher?.22D:.12D);for(int i=0;i<dust;i++)redstone(server,c.x+(rand.nextDouble()-.5D)*.64D,c.y+(rand.nextDouble()-.5D)*.56D,c.z+(rand.nextDouble()-.5D)*.64D,i%2==0?.55F:1F,i%2==0?.20F:.88F,1F);}
    private void sound(WorldServer server,net.minecraft.util.SoundEvent sound,float volume,float pitch){server.playSound(null,posX,posY+1.1D,posZ,sound,SoundCategory.PLAYERS,volume,pitch);}
    private static void redstone(WorldServer server,double x,double y,double z,float r,float g,float b){server.spawnParticle(EnumParticleTypes.REDSTONE,x,y,z,0,Math.max(.001F,r),Math.max(.001F,g),Math.max(.001F,b),1D);}
    private static float smoother(float t){t=MathHelper.clamp(t,0,1);return t*t*t*(t*(t*6-15)+10);}

    private Vec3d flatForward() {
        float yaw = rotationYaw * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw), 0, MathHelper.cos(yaw)).normalize();
    }

    private static DamageSource damageSource(EntityLivingBase owner) {
        return owner instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) owner)
                : DamageSource.causeMobDamage(owner);
    }

    @Override public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().grow(18, 7, 18); }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {
        dataManager.set(OWNER_ID, tag.getInteger("OwnerId"));
        dataManager.set(DAMAGE, tag.getFloat("Damage"));
        dataManager.set(MODE, tag.getInteger("Mode"));
    }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("OwnerId", dataManager.get(OWNER_ID));
        tag.setFloat("Damage", getBaseDamage()); tag.setInteger("Mode", getMode());
    }
    @Override public boolean canBeCollidedWith() { return false; }
}
