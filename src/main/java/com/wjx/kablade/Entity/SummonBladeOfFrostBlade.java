package com.wjx.kablade.Entity;

import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class SummonBladeOfFrostBlade extends EntitySummonedSwordBase {
    public SummonBladeOfFrostBlade(World par1World) {
        super(par1World);
    }

    private MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

    public SummonBladeOfFrostBlade(World par1World, EntityLivingBase entityLiving, float AttackLevel, float roll) {
        super(par1World, entityLiving, AttackLevel, roll);
    }

    public SummonBladeOfFrostBlade(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        super(par1World, entityLiving, AttackLevel);
    }

    @Override
    protected void attackEntity(Entity target) {
        target.getEntityData().setInteger("frost_blade_1",20);
        super.attackEntity(target);
    }
}
