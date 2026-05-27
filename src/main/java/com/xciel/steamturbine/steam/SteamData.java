package com.xciel.steamturbine.steam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SteamData {
    private float pressure;
    private float normalizedPressure;
    private float sourceStrength;
    private boolean valid;
    private boolean empty;

    public SteamData() {
        this.pressure = 0f;
        this.normalizedPressure = 0f;
        this.sourceStrength = SteamConstants.SOURCE_STRENGTH_DEFAULT;
        this.valid = false;
        this.empty = true;
    }

    public SteamData(float pressure, float sourceStrength) {
        this.pressure = clampPressure(pressure);
        this.sourceStrength = clampSourceStrength(sourceStrength);
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public static SteamData empty() {
        return new SteamData();
    }

    public static SteamData of(float pressure) {
        return new SteamData(pressure, SteamConstants.SOURCE_STRENGTH_DEFAULT);
    }

    public static SteamData of(float pressure, float sourceStrength) {
        return new SteamData(pressure, sourceStrength);
    }

    public float getPressure() {
        return pressure;
    }

    public float getNormalizedPressure() {
        return normalizedPressure;
    }

    public float getSourceStrength() {
        return sourceStrength;
    }

    public SteamPressureTier getPressureTier() {
        return SteamPressureTier.fromPressure(pressure);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setPressure(float pressure) {
        this.pressure = clampPressure(pressure);
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public void setSourceStrength(float sourceStrength) {
        this.sourceStrength = clampSourceStrength(sourceStrength);
    }

    public void setRaw(float pressure, float sourceStrength) {
        this.pressure = clampPressure(pressure);
        this.sourceStrength = clampSourceStrength(sourceStrength);
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public void copyFrom(SteamData other) {
        this.pressure = other.pressure;
        this.normalizedPressure = other.normalizedPressure;
        this.sourceStrength = other.sourceStrength;
        this.valid = other.valid;
        this.empty = other.empty;
    }

    public SteamData copy() {
        SteamData copy = new SteamData();
        copy.copyFrom(this);
        return copy;
    }

    public void lerpTo(SteamData target, float factor) {
        this.pressure = lerp(this.pressure, target.pressure, factor);
        this.sourceStrength = lerp(this.sourceStrength, target.sourceStrength, factor);
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public void decay() {
        this.pressure *= SteamConstants.TRANSFER_DECAY;
        if (this.pressure < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            this.pressure = 0f;
        }
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public void saveToNBT(CompoundTag nbt) {
        nbt.putFloat("Pressure", pressure);
        nbt.putFloat("SourceStrength", sourceStrength);
    }

    public void loadFromNBT(CompoundTag nbt) {
        this.pressure = nbt.contains("Pressure") ? nbt.getFloat("Pressure") : 0f;
        this.sourceStrength = nbt.contains("SourceStrength") ? nbt.getFloat("SourceStrength") : SteamConstants.SOURCE_STRENGTH_DEFAULT;
        this.pressure = clampPressure(this.pressure);
        this.sourceStrength = clampSourceStrength(this.sourceStrength);
        this.normalizedPressure = this.pressure / SteamConstants.MAX_PRESSURE;
        this.valid = this.pressure > 0;
        this.empty = this.pressure <= 0;
    }

    public MutableComponent getTierName() {
        return switch (getPressureTier()) {
            case NONE -> Component.translatable("steam.tier.none");
            case REGULAR -> Component.translatable("steam.tier.regular");
            case PRESSURED_1 -> Component.translatable("steam.tier.pressured_1");
            case PRESSURED_2 -> Component.translatable("steam.tier.pressured_2");
            case PRESSURED_3 -> Component.translatable("steam.tier.pressured_3");
        };
    }

    private static float clampPressure(float pressure) {
        return Math.max(0f, Math.min(pressure, SteamConstants.MAX_PRESSURE));
    }

    private static float clampSourceStrength(float strength) {
        return Math.max(0f, Math.min(strength, SteamConstants.SOURCE_STRENGTH_MAX));
    }

    private static float lerp(float a, float b, float factor) {
        return a + (b - a) * factor;
    }
}