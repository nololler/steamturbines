package com.xciel.steamturbine.steam;

public final class SteamConstants {
    public static final float MAX_PRESSURE = 10.0f;

    public static final float PROPAGATION_THRESHOLD = 0.05f;
    public static final float PROPAGATION_FACTOR = 0.8f;

    public static final float QUALITY_DECAY = 0.995f;

    public static final float TRANSFER_DECAY = 0.95f;
    public static final float MINIMUM_PROPAGATION_THRESHOLD = 0.01f;

    public static final float LERP_FACTOR = 0.2f;
    public static final float DECAY_FACTOR = 0.98f;

    public static final float VISUAL_NORMALIZATION_MIN = 0.0f;
    public static final float VISUAL_NORMALIZATION_MAX = 1.0f;

    public static final float SYNC_THRESHOLD = 0.05f;
    public static final int SYNC_COOLDOWN_TICKS = 20;

    public static final float SOURCE_STRENGTH_DEFAULT = 1.0f;
    public static final float SOURCE_STRENGTH_MAX = 3.0f;

    public static final int MIN_TICKS_BETWEEN_PARTICLES = 8;

    // Boiler
    public static final float HEAT_LEVEL_MAX = 3.0f;
    public static final float HEAT_LEVEL_NORMAL = 2.0f;
    public static final float MIN_HEAT_FOR_STEAM = 0.25f;
    public static final int WATER_PER_STEAM = 250;
    public static final int WATER_TANK_CAPACITY = 2000;

    // Throughput: base steam output per tick at max heat
    public static final float BASE_THROUGHPUT_PER_TICK = 1.0f;
    public static final float MAX_THROUGHPUT = 3.0f;

    // Su calculation: SU = throughput * pressure * efficiency
    public static final float SU_PER_THROUGHPUT_PRESSURE = 2.0f;

    // Lava Duct System
    public static final float LAVA_DUCT_SU_PER_FACE = 2500f;
    public static final int LAVA_DUCT_MAX_TURBINES = 5;
    public static final int LAVA_DUCT_MAX_WALK_DEPTH = 5;
    public static final int LAVA_DUCT_WATER_TANK_CAPACITY = 2000;
    public static final int LAVA_DUCT_WATER_PER_TICK_PER_TURBINE = 1;
}
