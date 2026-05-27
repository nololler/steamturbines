package com.xciel.steamturbine.steam;

public enum SteamPressureTier {
    NONE(0.0f, 0.0f),
    REGULAR(0.01f, 0.99f),
    PRESSURED_1(1.0f, 1.99f),
    PRESSURED_2(2.0f, 2.99f),
    PRESSURED_3(3.0f, Float.MAX_VALUE);

    private final float minPressure;
    private final float maxPressure;

    SteamPressureTier(float minPressure, float maxPressure) {
        this.minPressure = minPressure;
        this.maxPressure = maxPressure;
    }

    public float getMinPressure() {
        return minPressure;
    }

    public float getMaxPressure() {
        return maxPressure;
    }

    public boolean isActive() {
        return this != NONE;
    }

    public boolean isPressurized() {
        return this == PRESSURED_1 || this == PRESSURED_2 || this == PRESSURED_3;
    }

    public int getTierIndex() {
        return switch (this) {
            case NONE -> 0;
            case REGULAR -> 1;
            case PRESSURED_1 -> 2;
            case PRESSURED_2 -> 3;
            case PRESSURED_3 -> 4;
        };
    }

    public static SteamPressureTier fromPressure(float pressure) {
        if (pressure <= 0.0f) return NONE;
        if (pressure < 1.0f) return REGULAR;
        if (pressure < 2.0f) return PRESSURED_1;
        if (pressure < 3.0f) return PRESSURED_2;
        return PRESSURED_3;
    }

    public static SteamPressureTier fromPressureClamped(float pressure) {
        return fromPressure(clampPressure(pressure));
    }

    public static SteamPressureTier fromNormalized(float normalizedPressure) {
        return fromPressure(normalizedPressure * SteamConstants.MAX_PRESSURE);
    }

    private static float clampPressure(float pressure) {
        return Math.max(0f, Math.min(pressure, SteamConstants.MAX_PRESSURE));
    }
}