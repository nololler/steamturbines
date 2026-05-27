package com.xciel.steamturbine.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PressureData {
    public static final float MAX_PRESSURE = 10.0f;

    private float pressure;
    private float targetPressure;

    public PressureData() {
        this.pressure = 0f;
        this.targetPressure = 0f;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = Math.max(0f, Math.min(pressure, MAX_PRESSURE));
    }

    public float getTargetPressure() {
        return targetPressure;
    }

    public void setTargetPressure(float targetPressure) {
        this.targetPressure = Math.max(0f, Math.min(targetPressure, MAX_PRESSURE));
    }

    public float getNormalizedPressure() {
        return pressure / MAX_PRESSURE;
    }

    public float getVisualPressure() {
        return Math.max(0f, Math.min(pressure / MAX_PRESSURE, 1f));
    }

    public PressureTier.Tier getTier() {
        return PressureTier.fromPressure(pressure, MAX_PRESSURE);
    }

    public void lerpToTarget(float factor) {
        this.pressure += (targetPressure - pressure) * factor;
    }

    public void saveToNBT(CompoundTag nbt) {
        nbt.putFloat("Pressure", pressure);
        nbt.putFloat("TargetPressure", targetPressure);
    }

    public void loadFromNBT(CompoundTag nbt) {
        this.pressure = nbt.getFloat("Pressure");
        this.targetPressure = nbt.getFloat("TargetPressure");
    }

    public MutableComponent getTierName() {
        return switch (getTier()) {
            case LOW -> Component.translatable("pressure.tier.low");
            case MEDIUM -> Component.translatable("pressure.tier.medium");
            case HIGH -> Component.translatable("pressure.tier.high");
            case EXTREME -> Component.translatable("pressure.tier.extreme");
        };
    }
}