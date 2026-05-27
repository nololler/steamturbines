package com.xciel.steamturbine.data;

public class PressureTier {
    public static final float LOW_THRESHOLD = 0.33f;
    public static final float MEDIUM_THRESHOLD = 0.66f;
    public static final float HIGH_THRESHOLD = 0.9f;

    public enum Tier {
        LOW,
        MEDIUM,
        HIGH,
        EXTREME
    }

    public static Tier fromNormalized(float normalizedPressure) {
        if (normalizedPressure > HIGH_THRESHOLD) return Tier.EXTREME;
        if (normalizedPressure > MEDIUM_THRESHOLD) return Tier.HIGH;
        if (normalizedPressure > LOW_THRESHOLD) return Tier.MEDIUM;
        return Tier.LOW;
    }

    public static Tier fromPressure(float pressure, float maxPressure) {
        if (maxPressure <= 0) return Tier.LOW;
        return fromNormalized(pressure / maxPressure);
    }
}