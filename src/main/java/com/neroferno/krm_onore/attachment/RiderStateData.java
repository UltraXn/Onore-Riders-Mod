package com.neroferno.krm_onore.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Modern NeoForge Attachment storing player transformation and combat/Rider Kick runtime state.
 * Replaces legacy untyped NBT access on player.getPersistentData().
 */
public class RiderStateData implements INBTSerializable<CompoundTag> {

    // === Persistent State (Saved to world/player disk) ===
    private boolean transformed = false;
    private int form = 1; // Default: Growing Form (1)
    private long transformStartTick = 0;
    private boolean hasTransformStartTick = false;
    private long lastTransformTime = 0;

    // === Transient Combat & Animation State (Runtime only, zero disk bloat) ===
    private transient boolean transformPending = false;
    private transient int transformTicksRemaining = 0;

    private transient int riderKickState = 0;
    private transient int riderKickTicks = 0;
    private transient int riderKickHoverTicks = 0;
    private transient boolean riderKickFallImmunity = false;
    private transient boolean riderKickAnimPlayed = false;
    private transient long riderKickCooldownEnd = 0;

    private transient int riderKickTargetId = 0;
    private transient double riderKickTargetX = 0;
    private transient double riderKickTargetY = 0;
    private transient double riderKickTargetZ = 0;

    private transient double riderKickStartX = 0;
    private transient double riderKickStartY = 0;
    private transient double riderKickStartZ = 0;

    private transient double riderKickApexX = 0;
    private transient double riderKickApexY = 0;
    private transient double riderKickApexZ = 0;

    private transient double riderKickImpactX = 0;
    private transient double riderKickImpactY = 0;
    private transient double riderKickImpactZ = 0;

    private transient int prevRiderKickState = 0;
    private transient int clientRiderKickTicks = 0;

    public RiderStateData() {
    }

    // --- Transformation Getters / Setters ---

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }

    public long getTransformStartTick() {
        return transformStartTick;
    }

    public void setTransformStartTick(long tick) {
        this.transformStartTick = tick;
    }

    public boolean hasTransformStartTick() {
        return hasTransformStartTick;
    }

    public void setHasTransformStartTick(boolean val) {
        this.hasTransformStartTick = val;
    }

    public long getLastTransformTime() {
        return lastTransformTime;
    }

    public void setLastTransformTime(long time) {
        this.lastTransformTime = time;
    }

    public boolean isTransformPending() {
        return transformPending;
    }

    public void setTransformPending(boolean pending) {
        this.transformPending = pending;
    }

    public int getTransformTicksRemaining() {
        return transformTicksRemaining;
    }

    public void setTransformTicksRemaining(int ticks) {
        this.transformTicksRemaining = ticks;
    }

    // --- Rider Kick Getters / Setters ---

    public int getRiderKickState() {
        return riderKickState;
    }

    public void setRiderKickState(int state) {
        this.riderKickState = state;
    }

    public int getRiderKickTicks() {
        return riderKickTicks;
    }

    public void setRiderKickTicks(int ticks) {
        this.riderKickTicks = ticks;
    }

    public int getRiderKickHoverTicks() {
        return riderKickHoverTicks;
    }

    public void setRiderKickHoverTicks(int ticks) {
        this.riderKickHoverTicks = ticks;
    }

    public boolean isRiderKickFallImmunity() {
        return riderKickFallImmunity;
    }

    public void setRiderKickFallImmunity(boolean immunity) {
        this.riderKickFallImmunity = immunity;
    }

    public boolean isRiderKickAnimPlayed() {
        return riderKickAnimPlayed;
    }

    public void setRiderKickAnimPlayed(boolean played) {
        this.riderKickAnimPlayed = played;
    }

    public long getRiderKickCooldownEnd() {
        return riderKickCooldownEnd;
    }

    public void setRiderKickCooldownEnd(long cooldownEnd) {
        this.riderKickCooldownEnd = cooldownEnd;
    }

    public int getRiderKickTargetId() {
        return riderKickTargetId;
    }

    public void setRiderKickTargetId(int targetId) {
        this.riderKickTargetId = targetId;
    }

    public double getRiderKickTargetX() {
        return riderKickTargetX;
    }

    public void setRiderKickTargetX(double x) {
        this.riderKickTargetX = x;
    }

    public double getRiderKickTargetY() {
        return riderKickTargetY;
    }

    public void setRiderKickTargetY(double y) {
        this.riderKickTargetY = y;
    }

    public double getRiderKickTargetZ() {
        return riderKickTargetZ;
    }

    public void setRiderKickTargetZ(double z) {
        this.riderKickTargetZ = z;
    }

    public void setRiderKickTargetPos(double x, double y, double z) {
        this.riderKickTargetX = x;
        this.riderKickTargetY = y;
        this.riderKickTargetZ = z;
    }

    public double getRiderKickStartX() {
        return riderKickStartX;
    }

    public double getRiderKickStartY() {
        return riderKickStartY;
    }

    public double getRiderKickStartZ() {
        return riderKickStartZ;
    }

    public void setRiderKickStartPos(double x, double y, double z) {
        this.riderKickStartX = x;
        this.riderKickStartY = y;
        this.riderKickStartZ = z;
    }

    public double getRiderKickApexX() {
        return riderKickApexX;
    }

    public double getRiderKickApexY() {
        return riderKickApexY;
    }

    public double getRiderKickApexZ() {
        return riderKickApexZ;
    }

    public void setRiderKickApexPos(double x, double y, double z) {
        this.riderKickApexX = x;
        this.riderKickApexY = y;
        this.riderKickApexZ = z;
    }

    public double getRiderKickImpactX() {
        return riderKickImpactX;
    }

    public void setRiderKickImpactX(double x) {
        this.riderKickImpactX = x;
    }

    public double getRiderKickImpactY() {
        return riderKickImpactY;
    }

    public void setRiderKickImpactY(double y) {
        this.riderKickImpactY = y;
    }

    public double getRiderKickImpactZ() {
        return riderKickImpactZ;
    }

    public void setRiderKickImpactZ(double z) {
        this.riderKickImpactZ = z;
    }

    public void setRiderKickImpactPos(double x, double y, double z) {
        this.riderKickImpactX = x;
        this.riderKickImpactY = y;
        this.riderKickImpactZ = z;
    }

    public int getPrevRiderKickState() {
        return prevRiderKickState;
    }

    public void setPrevRiderKickState(int state) {
        this.prevRiderKickState = state;
    }

    public int getClientRiderKickTicks() {
        return clientRiderKickTicks;
    }

    public void setClientRiderKickTicks(int ticks) {
        this.clientRiderKickTicks = ticks;
    }

    public void resetKick() {
        this.riderKickState = 0;
        this.riderKickTicks = 0;
        this.riderKickHoverTicks = 0;
        this.riderKickFallImmunity = false;
        this.riderKickAnimPlayed = false;
        this.riderKickTargetId = 0;
        this.riderKickTargetX = 0;
        this.riderKickTargetY = 0;
        this.riderKickTargetZ = 0;
        this.clientRiderKickTicks = 0;
        this.prevRiderKickState = 0;
    }

    // --- NBT Serialization ---

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Transformed", this.transformed);
        tag.putInt("Form", this.form);
        tag.putLong("TransformStartTick", this.transformStartTick);
        tag.putBoolean("HasTransformStartTick", this.hasTransformStartTick);
        tag.putLong("LastTransformTime", this.lastTransformTime);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("Transformed")) {
            this.transformed = nbt.getBoolean("Transformed");
        }
        if (nbt.contains("Form")) {
            this.form = nbt.getInt("Form");
        } else {
            this.form = 1;
        }
        if (nbt.contains("TransformStartTick")) {
            this.transformStartTick = nbt.getLong("TransformStartTick");
        }
        if (nbt.contains("HasTransformStartTick")) {
            this.hasTransformStartTick = nbt.getBoolean("HasTransformStartTick");
        }
        if (nbt.contains("LastTransformTime")) {
            this.lastTransformTime = nbt.getLong("LastTransformTime");
        }
    }
}
