package com.wjx.kablade.Entity;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.Vec3f;
import com.wjx.kablade.util.renderingQueue.Actions.ActionBase;
import com.wjx.kablade.util.renderingQueue.RenderModel;
import com.wjx.kablade.util.renderingQueue.RenderObj;
import com.wjx.kablade.util.renderingQueue.RenderQueue;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import javax.annotation.Nonnull;

import java.util.List;

import static com.wjx.kablade.Entity.Render.RenderConceptual.*;

public class EntityConceptual extends Entity implements IThrowableEntity {
    public RenderQueue queue;
    public EntityConceptual(World world) {
        super(world);
        ticksExisted = 0;
        if(world.isRemote)
        {
            queue = new RenderQueue();
            RenderModel model1 = new RenderModel(modelE1);
            RenderModel model2 = new RenderModel(modelE2);
            model1.texModel = texE1;
            model2.texModel = texE1;
            model1.objColor = 0xFFFFCCFF;
            model2.objColor = 0xFFFFCCFF;
            RenderObj obj1 = new RenderObj(-1f, 2.2f, 1.2f, 5, -14, 122, 2, 2, 2);
            RenderObj obj3 = new RenderObj(-0.8f, 2.3f, 1.8f, 5, -14, 122, 2, 2, 2);
            RenderObj obj4 = new RenderObj(-0.7f, 2.6f, 2f, 18, -14, 90, 2, 2, 2);
            RenderObj obj6 = new RenderObj(1.6f, 3.4f, 2.1f, -1.4f, -7.3f, -3.55f, 2, 2, 2);
            RenderObj obj8 = new RenderObj(-1.3f, 1.3f, 1.2f, 60f, 68f, -71f, 2, 2, 2);
            RenderObj obj9 = new RenderObj(-1.1f, 2.7f, 2f, 18, -14, 90, 1.2f, 1.2f, 1.2f);
            RenderObj obj11 = new RenderObj(-2.1f, 1.6f, -1.5f, 14.6f, 10.4f, -27.4f, 2f, 2f, 2f);


            RenderObj obj2 = new RenderObj(1.5f, 1.5f, 2.5f, -30, 57.5f, 0, 1.2f, 1.2f, 1.2f);
            RenderObj obj5 = new RenderObj(1.1f, 2.3f, 2.7f, -22f, 53f, -2.5f, 1.2f, 1.2f, 1.2f);
            RenderObj obj7 = new RenderObj(0f, 1f, 0f, -22f, 53f, -2.5f, 1.2f, 1.2f, 1.2f);
            RenderObj obj10 = new RenderObj(0.8f, 2.7f, 2.7f, 71, -18f, -72, 1.5f, 1.5f, 1.5f);

            ActionBase o1_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 0, false);
            ActionBase o1_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 5, 0, false);
            ActionBase o1_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 8, 0, (byte) 7, true);
            ActionBase o1_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 12, 0, true);
            ActionBase o1_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 15, 0, (byte) 7, false);
            o1_5.doRemove = true;

            ActionBase o2_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 1, false);
            ActionBase o2_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 5, 1, false);
            ActionBase o2_3 = new ActionBase((byte) 2, new Vec3f(1.2f, 1.2f, 1.2f), 8, 1, (byte) 7, true);
            ActionBase o2_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 12, 1, true);
            ActionBase o2_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 15, 1, (byte) 7, false);
            o2_5.doRemove = true;


            ActionBase o3_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 2, false);
            ActionBase o3_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 7, 2, false);
            ActionBase o3_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 12, 2, (byte) 7, true);
            ActionBase o3_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 22, 2, true);
            ActionBase o3_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 24, 2, (byte) 7, false);
            o3_5.doRemove = true;

            ActionBase o4_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 3, false);
            ActionBase o4_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 7, 3, false);
            ActionBase o4_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 12, 3, (byte) 7, true);
            ActionBase o4_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 24, 3, true);
            ActionBase o4_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 40, 3, (byte) 7, false);
            o4_5.doRemove = true;


            ActionBase o5_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 4, false);
            ActionBase o5_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 7, 4, false);
            ActionBase o5_3 = new ActionBase((byte) 2, new Vec3f(1.2f, 1.2f, 1.2f), 12, 4, (byte) 7, true);
            ActionBase o5_4 = new ActionBase((byte) 1, new Vec3f(0f, 0f, 0f), 12, 4, true);
            ActionBase o5_5 = new ActionBase((byte) 0, new Vec3f(0f, 0f, 0f), 12, 4, true);
            ActionBase o5_6 = new ActionBase((byte) 0, new Vec3f(0.9f, 2.4f, 2.7f), 20, 4, (byte) 7, false);
            ActionBase o5_7 = new ActionBase((byte) 1, new Vec3f(-19f, 73f, -5f), 20, 4, (byte) 7, false);
            ActionBase o5_8 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 15, 4, true);
            ActionBase o5_9 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 20, 4, (byte) 7, false);
            o5_9.doRemove = true;


            ActionBase o6_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 5, false);
            ActionBase o6_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 7, 5, false);
            ActionBase o6_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 15, 5, (byte) 7, true);
            ActionBase o6_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 18, 5, true);
            ActionBase o6_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 20, 5, (byte) 7, false);
            o6_5.doRemove = true;

            ActionBase o7_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 6, false);
            ActionBase o7_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 10, 6, false);
            ActionBase o7_3 = new ActionBase((byte) 2, new Vec3f(1.2f, 1.2f, 1.2f), 15, 6, (byte) 7, true);
            ActionBase o7_4 = new ActionBase((byte) 1, new Vec3f(0f, 0f, 0f), 10, 6, true);
            ActionBase o7_5 = new ActionBase((byte) 1, new Vec3f(0f, 90f, 0f), 40, 6, true);
            ActionBase o7_6 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 40, 6, (byte) 7, false);
            o7_6.doRemove = true;

            ActionBase o8_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 7, false);
            ActionBase o8_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 10, 7, false);
            ActionBase o8_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 15, 7, (byte) 7, true);
            ActionBase o8_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 18, 7, true);
            ActionBase o8_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 20, 7, (byte) 7, false);
            o8_5.doRemove = true;

            ActionBase o9_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 8, false);
            ActionBase o9_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 12, 8, false);
            ActionBase o9_3 = new ActionBase((byte) 2, new Vec3f(2, 2, 2), 15, 8, (byte) 7, true);
            ActionBase o9_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 24, 8, true);
            ActionBase o9_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 40, 8, (byte) 7, false);
            o9_5.doRemove = true;

            ActionBase o10_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 9, false);
            ActionBase o10_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 18, 9, false);
            ActionBase o10_3 = new ActionBase((byte) 2, new Vec3f(1.5f, 1.5f, 1.5f), 20, 9, (byte) 7, true);
            ActionBase o10_6 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 22, 9, true);
            ActionBase o10_4 = new ActionBase((byte) 1, new Vec3f(0f, 0f, 0f), 15, 9, true);
            ActionBase o10_5 = new ActionBase((byte) 1, new Vec3f(-89f, 11f, -112f), 24, 9, (byte) 3, false);
            ActionBase o10_7 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 24, 9, (byte) 7, false);
            o10_7.doRemove = true;

            ActionBase o11_1 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 0, 10, false);
            ActionBase o11_2 = new ActionBase((byte) 2, new Vec3f(0, 0, 0), 18, 10, false);
            ActionBase o11_3 = new ActionBase((byte) 2, new Vec3f(2f, 2f, 2f), 20, 10, (byte) 7, true);
            ActionBase o11_4 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 24, 10, true);
            ActionBase o11_5 = new ActionBase((byte) 2, new Vec3f(0f, 0f, 0f), 40, 10, (byte) 7, false);
            o11_5.doRemove = true;


            queue.addObj(obj1);
            queue.addObj(obj2);
            queue.addObj(obj3);
            queue.addObj(obj4);
            queue.addObj(obj5);
            queue.addObj(obj6);
            queue.addObj(obj7);
            queue.addObj(obj8);
            queue.addObj(obj9);
            queue.addObj(obj10);
            queue.addObj(obj11);

            queue.addModel(0, model1);
            queue.addModel(1, model2);
            queue.addModel(2, model1);
            queue.addModel(3, model1);
            queue.addModel(4, model2);
            queue.addModel(5, model1);
            queue.addModel(6, model2);
            queue.addModel(7, model1);
            queue.addModel(8, model1);
            queue.addModel(9, model2);
            queue.addModel(10, model1);

            queue.addAction(o1_1);
            queue.addAction(o1_2);
            queue.addAction(o1_3);
            queue.addAction(o1_4);
            queue.addAction(o1_5);

            queue.addAction(o2_1);
            queue.addAction(o2_2);
            queue.addAction(o2_3);
            queue.addAction(o2_4);
            queue.addAction(o2_5);


            queue.addAction(o3_1);
            queue.addAction(o3_2);
            queue.addAction(o3_3);
            queue.addAction(o3_4);
            queue.addAction(o3_5);

            queue.addAction(o4_1);
            queue.addAction(o4_2);
            queue.addAction(o4_3);
            queue.addAction(o4_4);
            queue.addAction(o4_5);


            queue.addAction(o5_1);
            queue.addAction(o5_2);
            queue.addAction(o5_3);
            queue.addAction(o5_4);
            queue.addAction(o5_5);
            queue.addAction(o5_6);
            queue.addAction(o5_7);
            queue.addAction(o5_8);
            queue.addAction(o5_9);

            queue.addAction(o6_1);
            queue.addAction(o6_2);
            queue.addAction(o6_3);
            queue.addAction(o6_4);
            queue.addAction(o6_5);

            queue.addAction(o7_1);
            queue.addAction(o7_2);
            queue.addAction(o7_3);
            queue.addAction(o7_4);
            queue.addAction(o7_5);
            queue.addAction(o7_6);

            queue.addAction(o8_1);
            queue.addAction(o8_2);
            queue.addAction(o8_3);
            queue.addAction(o8_4);
            queue.addAction(o8_5);

            queue.addAction(o9_1);
            queue.addAction(o9_2);
            queue.addAction(o9_3);
            queue.addAction(o9_4);
            queue.addAction(o9_5);

            queue.addAction(o10_1);
            queue.addAction(o10_2);
            queue.addAction(o10_3);
            queue.addAction(o10_4);
            queue.addAction(o10_5);
            queue.addAction(o10_6);
            queue.addAction(o10_7);

            queue.addAction(o11_1);
            queue.addAction(o11_2);
            queue.addAction(o11_3);
            queue.addAction(o11_4);
            queue.addAction(o11_5);


            queue.initialize();
        }
    }

    public EntityConceptual(World world,EntityLivingBase ownerIn){
        super(world);
        ticksExisted = 0;
        this.rotationYaw = ownerIn.rotationYaw;
        this.owner = ownerIn;
    }

    public EntityLivingBase owner=null;
    public ItemStack blade = null;

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(@Nonnull NBTTagCompound nbtTagCompound) {

    }

    @Override
    protected void writeEntityToNBT(@Nonnull NBTTagCompound nbtTagCompound) {

    }

    @Override
    public Entity getThrower() {
        return owner;
    }

    @Override
    public void setThrower(Entity entity) {
        if(entity instanceof EntityLivingBase)
            owner = (EntityLivingBase) entity;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if(ticksExisted>60){
            this.setDead();
        }
        if (ticksExisted > 6 && ticksExisted < 35){
            World world1 = this.world;
            if (!world1.isRemote) {
                if(ticksExisted % 4 == 0)
                {
                    List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(8.0D, 4.0D, 8.0D), EntitySelectorAttackable.getInstance());
                    for (Entity entity : list) {

                        {
                            if (entity instanceof EntityLivingBase && !(entity.equals(owner))) {
                                EntityLivingBase living = (EntityLivingBase) entity;
                                if (owner instanceof EntityPlayer) {
                                    float extraDamage = 0;
                                    if (blade != null) {
                                        blade.hitEntity(living, (EntityPlayer) owner);

                                        extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()), 5f);
                                    }
                                    StylishRankManager.setNextAttackType(this.owner, StylishRankManager.AttackTypes.SlashDim);

                                    StylishRankManager.doAttack(this.owner);
                                    ((EntityPlayer) owner).onCriticalHit(living);
                                    living.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner).setDamageBypassesArmor(), 20.0F + extraDamage);
                                    if (blade != null) {
                                        blade.getItem().hitEntity(blade, living, (EntityPlayer) owner);


                                    }
                                    living.hurtTime = 0;
                                    living.hurtResistantTime = 0;


                                } else {
                                    living.attackEntityFrom(DamageSource.causeMobDamage(owner).setDamageBypassesArmor(), 20.0F);
                                    living.hurtTime = 0;
                                    living.hurtResistantTime = 0;
                                }


                            }
                        }
                    }
                }

            }
        }
    }
}
