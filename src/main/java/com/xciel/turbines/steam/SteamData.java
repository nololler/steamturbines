package com.xciel.turbines.steam;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SteamData {
    private final float pressure;
    private final SteamType steamType;
    private final float quality;
    private final float sourceStrength;
    private final float throughput;

    private SteamData(float pressure, SteamType steamType, float quality, float sourceStrength, float throughput) {
        this.pressure = Math.max(0f, Math.min(pressure, SteamConstants.MAX_PRESSURE));
        this.steamType = steamType != null ? steamType : SteamType.REGULAR;
        this.quality = Math.max(0f, Math.min(quality, 1f));
        this.sourceStrength = Math.max(0f, Math.min(sourceStrength, SteamConstants.SOURCE_STRENGTH_MAX));
        this.throughput = Math.max(0f, throughput);
    }

    public static SteamData empty() {
        return new SteamData(0f, SteamType.REGULAR, 0f, SteamConstants.SOURCE_STRENGTH_DEFAULT, 0f);
    }

    public static SteamData of(float pressure) {
        return new SteamData(pressure, SteamType.REGULAR, 1f, SteamConstants.SOURCE_STRENGTH_DEFAULT, 0f);
    }

    public static SteamData of(float pressure, float throughput) {
        return new SteamData(pressure, SteamType.REGULAR, 1f, SteamConstants.SOURCE_STRENGTH_DEFAULT, throughput);
    }

    public static SteamData of(float pressure, SteamType steamType) {
        return new SteamData(pressure, steamType, 1f, SteamConstants.SOURCE_STRENGTH_DEFAULT, 0f);
    }

    public static SteamData of(float pressure, SteamType steamType, float quality) {
        return new SteamData(pressure, steamType, quality, SteamConstants.SOURCE_STRENGTH_DEFAULT, 0f);
    }

    public static SteamData of(float pressure, SteamType steamType, float quality, float sourceStrength) {
        return new SteamData(pressure, steamType, quality, sourceStrength, 0f);
    }

    public static SteamData of(float pressure, SteamType steamType, float quality, float sourceStrength, float throughput) {
        return new SteamData(pressure, steamType, quality, sourceStrength, throughput);
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

    public float getThroughput() {
        return throughput;
    }

    public SteamPressureTier getPressureTier() {
        return SteamPressureTier.fromPressure(pressure);
    }

    public boolean isEmpty() {
        return pressure <= 0f;
    }

    public SteamData withPressure(float newPressure) {
        return new SteamData(newPressure, steamType, quality, sourceStrength, throughput);
    }

    public SteamData withThroughput(float newThroughput) {
        return new SteamData(pressure, steamType, quality, sourceStrength, newThroughput);
    }

    public SteamData withPressureAndThroughputAdded(float addedPressure, float addedThroughput) {
        float newPressure = Math.max(pressure, addedPressure);
        float newThroughput = throughput + addedThroughput;
        return new SteamData(newPressure, steamType, quality, sourceStrength, newThroughput);
    }

    public SteamData withSteamType(SteamType newType) {
        return new SteamData(pressure, newType, quality, sourceStrength, throughput);
    }

    public SteamData withQuality(float newQuality) {
        return new SteamData(pressure, steamType, newQuality, sourceStrength, throughput);
    }

    public SteamData withPropagationLoss() {
        float newPressure = pressure * SteamConstants.PROPAGATION_FACTOR;
        float newQuality = quality * SteamConstants.QUALITY_DECAY;
        float newThroughput = throughput * SteamConstants.PROPAGATION_FACTOR;
        if (newPressure < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            newPressure = 0f;
            newThroughput = 0f;
        }
        return new SteamData(newPressure, steamType, newQuality, sourceStrength, newThroughput);
    }

    public SteamData withDecay() {
        float newPressure = pressure * SteamConstants.DECAY_FACTOR;
        float newQuality = quality * SteamConstants.QUALITY_DECAY;
        float newThroughput = throughput * SteamConstants.DECAY_FACTOR;
        if (newPressure < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            newPressure = 0f;
            newThroughput = 0f;
        }
        return new SteamData(newPressure, steamType, newQuality, sourceStrength, newThroughput);
    }

    public boolean shouldPropagate() {
        return pressure >= SteamConstants.PROPAGATION_THRESHOLD;
    }

    public boolean similarTo(SteamData other) {
        if (other == null) return false;
        return Math.abs(this.pressure - other.pressure) < 0.01f
            && Math.abs(this.throughput - other.throughput) < 0.01f
            && this.steamType == other.steamType;
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
        nbt.putFloat("Throughput", throughput);
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
        float throughput = nbt.contains("Throughput") ? nbt.getFloat("Throughput") : 0f;
        return new SteamData(pressure, steamType, quality, sourceStrength, throughput);
    }
}