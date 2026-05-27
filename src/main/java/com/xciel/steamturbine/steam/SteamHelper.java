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
        SteamData result = new SteamData();
        result.setRaw(
            lerp(a.getPressure(), b.getPressure(), factor),
            lerp(a.getSourceStrength(), b.getSourceStrength(), factor)
        );
        return result;
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
        return Math.abs(oldNorm - newNorm) >= SteamConstants.SYNC_THRESHOLD_CHANGE;
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
        return SteamData.of(extracted, source.getSourceStrength());
    }

    public static void receiveSteam(SteamData target, SteamData incoming) {
        if (incoming.isEmpty()) return;
        float combinedPressure = target.getPressure() + incoming.getPressure();
        float avgStrength = (target.getSourceStrength() + incoming.getSourceStrength()) * 0.5f;
        target.setRaw(combinedPressure, avgStrength);
    }
}