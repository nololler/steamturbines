package com.xciel.steamturbine.steam;

public final class SteamHelper {
    private SteamHelper() {}

    public static float normalizePressure(float pressure) {
        if (pressure <= 0) return 0f;
        return Math.min(pressure / SteamConstants.MAX_PRESSURE, 1f);
    }

    public static float denormalizePressure(float normalizedPressure) {
        return normalizedPressure * SteamConstants.MAX_PRESSURE;
    }

    public static SteamPressureTier getPressureTier(float pressure) {
        return SteamPressureTier.fromPressure(pressure);
    }

    public static float clampPressure(float pressure) {
        return Math.max(0f, Math.min(pressure, SteamConstants.MAX_PRESSURE));
    }

    public static float clampSourceStrength(float strength) {
        return Math.max(0f, Math.min(strength, SteamConstants.SOURCE_STRENGTH_MAX));
    }

    public static float lerp(float a, float b, float factor) {
        return a + (b - a) * factor;
    }

    public static SteamData lerpSteam(SteamData a, SteamData b, float factor) {
        return SteamData.of(
            lerp(a.getPressure(), b.getPressure(), factor),
            a.getSteamType(),
            lerp(a.getQuality(), b.getQuality(), factor),
            1f,
            lerp(a.getThroughput(), b.getThroughput(), factor)
        );
    }

    public static float decayPressure(float pressure) {
        float decayed = pressure * SteamConstants.TRANSFER_DECAY;
        if (decayed < SteamConstants.MINIMUM_PROPAGATION_THRESHOLD) {
            return 0f;
        }
        return decayed;
    }

    public static boolean canPropagate(float pressure) {
        return pressure >= SteamConstants.MINIMUM_PROPAGATION_THRESHOLD;
    }

    public static boolean shouldSync(float oldPressure, float newPressure) {
        float oldNorm = normalizePressure(oldPressure);
        float newNorm = normalizePressure(newPressure);
        return Math.abs(oldNorm - newNorm) >= SteamConstants.SYNC_THRESHOLD;
    }

    public static float getVisualPressure(float pressure) {
        return Math.max(0f, Math.min(normalizePressure(pressure), 1f));
    }

    public static float calculateTransferAmount(float available, float requested) {
        return Math.min(available, requested);
    }

    public static SteamData extractSteam(SteamData source, float amount) {
        if (source.isEmpty() || amount <= 0) {
            return SteamData.empty();
        }
        float extracted = Math.min(source.getPressure(), amount);
        float extractedRatio = source.getPressure() > 0 ? extracted / source.getPressure() : 0f;
        float extractedThroughput = source.getThroughput() * extractedRatio;
        return SteamData.of(extracted, source.getSteamType(), source.getQuality(), 1f, extractedThroughput);
    }

    public static SteamData receiveSteam(SteamData target, SteamData incoming) {
        if (incoming.isEmpty()) return target;
        float combinedPressure = target.getPressure() + incoming.getPressure();
        float avgQuality = (target.getQuality() + incoming.getQuality()) * 0.5f;
        SteamType type = target.getSteamType().isBetterThan(incoming.getSteamType())
            ? target.getSteamType() : incoming.getSteamType();
        float combinedThroughput = target.getThroughput() + incoming.getThroughput();
        return SteamData.of(combinedPressure, type, avgQuality, 1f, combinedThroughput);
    }
}