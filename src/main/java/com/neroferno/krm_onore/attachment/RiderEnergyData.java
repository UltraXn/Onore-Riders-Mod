package com.neroferno.krm_onore.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores and manages Rider Energy (Henshin Gauge) for players.
 */
public class RiderEnergyData implements INBTSerializable<CompoundTag> {
    public static final float DEFAULT_MAX_ENERGY = 100.0f;

    private float currentEnergy;
    private float maxEnergy;

    public RiderEnergyData() {
        this.maxEnergy = DEFAULT_MAX_ENERGY;
        this.currentEnergy = DEFAULT_MAX_ENERGY;
    }

    public float getEnergy() {
        return currentEnergy;
    }

    public void setEnergy(float energy) {
        this.currentEnergy = Math.max(0.0f, Math.min(energy, this.maxEnergy));
    }

    public float getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(float maxEnergy) {
        this.maxEnergy = Math.max(1.0f, maxEnergy);
        this.currentEnergy = Math.min(this.currentEnergy, this.maxEnergy);
    }

    public float getPercentage() {
        return maxEnergy > 0 ? (currentEnergy / maxEnergy) : 0.0f;
    }

    public boolean consume(float amount) {
        if (currentEnergy >= amount) {
            setEnergy(currentEnergy - amount);
            return true;
        }
        return false;
    }

    public void gain(float amount) {
        setEnergy(currentEnergy + amount);
    }

    public void fill() {
        this.currentEnergy = this.maxEnergy;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Energy", this.currentEnergy);
        tag.putFloat("MaxEnergy", this.maxEnergy);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("MaxEnergy")) {
            this.maxEnergy = nbt.getFloat("MaxEnergy");
        } else {
            this.maxEnergy = DEFAULT_MAX_ENERGY;
        }
        if (nbt.contains("Energy")) {
            this.currentEnergy = nbt.getFloat("Energy");
        } else {
            this.currentEnergy = this.maxEnergy;
        }
    }
}
