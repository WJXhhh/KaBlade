package com.wjx.kablade.Entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class RainBow extends Entity {
    public long boltVertex;
    private int boltLivingTime;
    private byte lightningState;
    public RainBow(World world){
        super(world);
    }

    public RainBow(World world, double x, double y, double z) {
        super(world);
        this.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltVertex = this.rand.nextLong();
        this.boltLivingTime = this.rand.nextInt(3) + 1;
    }
    public void onUpdate() {
        super.onUpdate();


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

        if (this.lightningState >= 0 && this.world.isRemote) {
            this.world.setLastLightningBolt(2);
        }

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
}
