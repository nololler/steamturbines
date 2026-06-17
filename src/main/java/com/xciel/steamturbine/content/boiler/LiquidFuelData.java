package com.xciel.steamturbine.content.boiler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record LiquidFuelData(int burnTime, boolean superheated) {

    public static final Codec<LiquidFuelData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.fieldOf("burnTime").forGetter(LiquidFuelData::burnTime),
        Codec.BOOL.optionalFieldOf("superheated", false).forGetter(LiquidFuelData::superheated)
    ).apply(inst, LiquidFuelData::new));

    public static final LiquidFuelData EMPTY = new LiquidFuelData(0, false);
}
