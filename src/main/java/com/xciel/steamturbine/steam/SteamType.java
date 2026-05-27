package com.xciel.steamturbine.steam;

public enum SteamType {
    REGULAR(0.0f, 0),
    PRESSURIZED(1.0f, 1),
    SUPERHEATED(2.0f, 2),
    EXHAUST(3.0f, 3);

    private final float thermalEnergy;
    private final int upgradeLevel;

    SteamType(float thermalEnergy, int upgradeLevel) {
        this.thermalEnergy = thermalEnergy;
        this.upgradeLevel = upgradeLevel;
    }

    public float getThermalEnergy() {
        return thermalEnergy;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public SteamType getUpgradedType() {
        return switch (this) {
            case REGULAR -> PRESSURIZED;
            case PRESSURIZED -> SUPERHEATED;
            case SUPERHEATED, EXHAUST -> EXHAUST;
        };
    }

    public boolean isBetterThan(SteamType other) {
        return this.upgradeLevel > other.upgradeLevel;
    }

    public static SteamType fromUpgradeLevel(int level) {
        return switch (level) {
            case 0 -> REGULAR;
            case 1 -> PRESSURIZED;
            case 2 -> SUPERHEATED;
            default -> EXHAUST;
        };
    }
}