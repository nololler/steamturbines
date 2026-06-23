package com.xciel.turbines.content.boiler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record LiquidFuelData(int burnTime, boolean superheated, float heatLevel, float consumptionMultiplier) {

    public static final Codec<LiquidFuelData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.fieldOf("burnTime").forGetter(LiquidFuelData::burnTime),
        Codec.BOOL.optionalFieldOf("superheated", false).forGetter(LiquidFuelData::superheated),
        Codec.FLOAT.optionalFieldOf("heatLevel", 0f).forGetter(LiquidFuelData::heatLevel),
        Codec.FLOAT.optionalFieldOf("consumptionMultiplier", 1.0f).forGetter(LiquidFuelData::consumptionMultiplier)
    ).apply(inst, LiquidFuelData::new));

    public static final LiquidFuelData EMPTY = new LiquidFuelData(0, false, 0f, 1.0f);

    public boolean hasHeatOverride() {
        return heatLevel > 0f;
    }
}
