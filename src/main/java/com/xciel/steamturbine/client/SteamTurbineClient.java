package com.xciel.steamturbine.client;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.compressor.SteamCompressorRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;

public class SteamTurbineClient {

    public static void addClientListeners(IEventBus eventBus) {
        SteamCompressorRenderer.register();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_COMPRESSOR.get())
            .factory(SteamCompressorVisual::new)
            .apply();
    }
}