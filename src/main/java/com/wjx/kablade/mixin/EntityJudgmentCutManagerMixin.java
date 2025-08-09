package com.wjx.kablade.mixin;


import mods.flammpfeil.slashblade.entity.EntityJudgmentCutManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntityJudgmentCutManager.class , remap = false)
public class EntityJudgmentCutManagerMixin extends Entity implements IThrowableEntity {
    public EntityJudgmentCutManagerMixin(World worldIn) {
        super(worldIn);
    }

    @Shadow
    protected Entity thrower;
    @Shadow
    protected ItemStack blade = ItemStack.EMPTY;

    /**
     * ★多段Hit防止用List
     */
    @Shadow
    protected List<Entity> alreadyHitEntity = new ArrayList<Entity>();

    @Shadow
    void setThrowerEntityID(int id){}

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
        return null;
    }

    @Override
    public void setThrower(Entity entity) {

    }


}
