package com.wjx.kablade.capability;

import com.wjx.kablade.capability.inters.IPotionInSlash;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapabilitySlashPotion {
    public static class Storage implements Capability.IStorage<IPotionInSlash>
    {
        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IPotionInSlash> capability, IPotionInSlash instance, EnumFacing side) {
            return instance.getSlashPotions();
        }

        @Override
        public void readNBT(Capability<IPotionInSlash> capability, IPotionInSlash instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound){
                instance.setSlashPotions((NBTTagCompound) nbt);
            }
        }
    }

    public static class Implementation implements IPotionInSlash{
        NBTTagCompound compound = new NBTTagCompound();

        @Override
        public NBTTagCompound getSlashPotions() {
            return compound;
        }

        @Override
        public void setSlashPotions(NBTTagCompound compound) {
            this.compound =compound;
        }
    }

    public static class ProviderEntity implements ICapabilitySerializable<NBTTagCompound>{
        private final IPotionInSlash slashPotion = new Implementation();
        private final Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return CapabilityLoader.SlashPotion.equals(capability);
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (CapabilityLoader.SlashPotion.equals(capability))
            {
                @SuppressWarnings("unchecked")
                T result = (T) slashPotion;
                return result;
            }
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound compound = new NBTTagCompound();
            NBTBase nbt = storage.writeNBT(CapabilityLoader.SlashPotion, slashPotion, null);
            if (nbt != null)
            compound.setTag("slash_potion",nbt);
            return compound;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            NBTBase base = nbt.getTag("slash_potion");
            if (base instanceof NBTTagCompound){
                storage.readNBT(CapabilityLoader.SlashPotion,slashPotion,null,base);
            }
        }
    }

    public static NBTTagCompound initNBT(IPotionInSlash potion){
        NBTTagCompound compound;
        Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
        if (potion != null){
            if (!storage.writeNBT(CapabilityLoader.SlashPotion,potion,null).isEmpty()){
                compound = (NBTTagCompound) storage.writeNBT(CapabilityLoader.SlashPotion,potion,null).copy();
            }
            else compound = new NBTTagCompound();
        }
        else compound = new NBTTagCompound();
        return compound;
    }
}
