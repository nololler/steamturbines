package com.xciel.steamturbine.steam;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SteamData {
    private final float pressure;
    private final SteamType steamType;
    private final float quality;
    private final float sourceStrength;

    private SteamData(float pressure, SteamType steamType, float quality, float sourceStrength) {
        this.pressure = Math.max(0f, Math.min(pressure, SteamConstants.MAX_PRESSURE));
        this.steamType = steamType != null ? steamType : SteamType.REGULAR;
        this.quality = Math.max(0f, Math.min(quality, 1f));
        this.sourceStrength = Math.max(0f, Math.min(sourceStrength, SteamConstants.SOURCE_STRENGTH_MAX));
    }

    public static SteamData empty() {
        return new SteamData(0f, SteamType.REGULAR, 0f, SteamConstants.SOURCE_STRENGTH_DEFAULT);
    }

    public static SteamData of(float pressure) {
        return new SteamData(pressure, SteamType.REGULAR, 1f, SteamConstants.SOURCE_STRENGTH_DEFAULT);
    }

    public static SteamData of(float pressure, SteamType steamType) {
        return new SteamData(pressure, steamType, 1f, SteamConstants.SOURCE_STRENGTH_DEFAULT);
    }

    public static SteamData of(float pressure, SteamType steamType, float quality) {
        return new SteamData(pressure, steamType, quality, SteamConstants.SOURCE_STRENGTH_DEFAULT);
    }

    public static SteamData of(float pressure, SteamType steamType, float quality, float sourceStrength) {
        return new SteamData(pressure, steamType, quality, sourceStrength);
    }

    public float getPressure() {
        return pressure;
    }

    public float getNormalizedPressure() {
        if (SteamConstants.MAX_PRESSURE <= 0) return 0f;
        return Math.min(pressure / SteamConstants.MAX_PRESSURE, 1f);
    }

    public SteamType getSteamType() {
        return steamType;
    }

    public float getQuality() {
        return quality;
    }

    public float getSourceStrength() {
        return sourceStrength;
    }

    public SteamPressureTier getPressureTier() {
        return SteamPressureTier.fromPressure(pressure);
    }

    public boolean isEmpty() {
        return pressure <= 0f;
    }

    public SteamData withPressure(float newPressure) {
        return new SteamData(newPressure, steamType, quality, sourceStrength);
    }

    public SteamData withSteamType(SteamType newType) {
        return new SteamData(pressure, newType, quality, sourceStrength);
    }

    public SteamData withQuality(float newQuality) {
        return new SteamData(pressure, steamType, newQuality, sourceStrength);
    }

    public SteamData withPropagationLoss() {
        float newPressure = pressure * SteamConstants.PROPAGATION_FACTOR;
        float newQuality = quality * SteamConstants.QUALITY_DECAY;
        if (newPressure < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            newPressure = 0f;
        }
        return new SteamData(newPressure, steamType, newQuality, sourceStrength);
    }

    public SteamData withDecay() {
        float newPressure = pressure * SteamConstants.DECAY_FACTOR;
        float newQuality = quality * SteamConstants.QUALITY_DECAY;
        if (newPressure < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            newPressure = 0f;
        }
        return new SteamData(newPressure, steamType, newQuality, sourceStrength);
    }

    public boolean shouldPropagate() {
        return pressure >= SteamConstants.PROPAGATION_THRESHOLD;
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

    public void saveToNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putFloat("Pressure", pressure);
        nbt.putString("SteamType", steamType.name());
        nbt.putFloat("Quality", quality);
        nbt.putFloat("SourceStrength", sourceStrength);
    }

    public static SteamData loadFromNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        float pressure = nbt.contains("Pressure") ? nbt.getFloat("Pressure") : 0f;
        SteamType steamType = SteamType.REGULAR;
        if (nbt.contains("SteamType")) {
            try {
                steamType = SteamType.valueOf(nbt.getString("SteamType"));
            } catch (IllegalArgumentException ignored) {}
        }
        float quality = nbt.contains("Quality") ? nbt.getFloat("Quality") : 1f;
        float sourceStrength = nbt.contains("SourceStrength")
            ? nbt.getFloat("SourceStrength")
            : SteamConstants.SOURCE_STRENGTH_DEFAULT;
        return new SteamData(pressure, steamType, quality, sourceStrength);
    }
}