package com.wjx.kablade.Entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.Timer;
import java.util.TimerTask;

public class EntityCrimsonSakuraAttack extends Entity implements IThrowableEntity {

    //public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityThunderEdgeAttack.class, DataSerializers.VARINT);

    public Timer timer;
    public Timer timer1;

    public float alpha = 1f;

    public int progress = 117;

    public int counter = 0;

    public double thickness = 1.75;

    public float ang = 15f;



    public double thick2 = 3;

    public int tC = 0;



    public TimerTask taskCounter = new TimerTask() {
        @Override
        public void run() {
            counter++;
            int count = counter;
            progress = 117;
            if(count <10){
                progress =117 - (int) Math.round(117d/10d*((double) count));
                thickness = 1.75 + (1.25/10*count);
            }
            else {
                progress = 0;
                thickness = 3d;
            }
            if(count<=40){
                ang = 15 - (30f/40f)*count;
                int k;
            }
            else{
                ang = -15;
            }
            if(counter>20&&counter<=40){
                alpha = 1 - (1/20f)*(counter-20f);
            }
        }
    };

    public TimerTask taskThick = new TimerTask() {
        @Override
        public void run() {
            tC++;
            if(tC >20&&tC<=40){
                thick2 = 3 - ((1.25/20d) * (tC - 20));
                double l = thick2;
            }

        }
    };

    public EntityLivingBase owner = null;

    public EntityCrimsonSakuraAttack(World world){
        super(world);
        ticksExisted = 0;
        timer = new Timer();
        timer.schedule(taskCounter,0,20);
        timer1 = new Timer();
        timer1.schedule(taskThick,0,20);
    }

    public EntityCrimsonSakuraAttack(World world, EntityLivingBase ownerIn){
        this(world);
        owner = ownerIn;
        this.setPositionAndUpdate(owner.posX,owner.posY,owner.posZ);
        this.rotationYaw = ownerIn.rotationYaw;
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public Entity getThrower() {
        return owner;
    }

    @Override
    public void setThrower(Entity entity) {
        owner = (EntityLivingBase) entity;
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if(this.ticksExisted > 20){
            this.setDead();
            if(timer != null){
                timer.cancel();
            }
        }
    }
}
