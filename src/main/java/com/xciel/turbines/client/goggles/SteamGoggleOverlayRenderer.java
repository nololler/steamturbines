package com.xciel.turbines.client.goggles;

import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.turbines.steam.SteamPressureTier;
import com.xciel.turbines.steam.SteamType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class SteamGoggleOverlayRenderer {
    public static void appendPipeInfo(PressurizedPipeBlockEntity pipe, java.util.List<Component> tooltip) {
        for (Direction dir : Direction.values()) {
            float pressure = pipe.getVisualPressure(dir);
            if (pressure <= 0.01f) continue;

            SteamType type = pipe.getVisualSteamType(dir);
            float quality = pipe.getVisualQuality(dir);

            SteamPressureTier tier = SteamPressureTier.fromPressure(pressure);
            float normalizedPressure = Math.min(pressure / 10f, 1f) * 100f;

            tooltip.add(Component.translatable("steam.pipe.flow", dir.getName().toUpperCase()));
            tooltip.add(Component.literal(switch (type) {
                case REGULAR -> "Regular Steam";
                case PRESSURIZED -> "Pressurized Steam";
                case SUPERHEATED -> "Superheated Steam";
                case EXHAUST -> "Exhaust Steam";
            }));
            tooltip.add(Component.literal(String.format("Pressure: %.1f (%.0f%%)", pressure, normalizedPressure)));
            tooltip.add(Component.literal("Tier: " + tier.name()));
            tooltip.add(Component.literal(String.format("Quality: %.0f%%", quality * 100)));
            return;
        }
    }
}