package com.xciel.steamturbine.client;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.compressor.SteamCompressorRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class SteamTurbineClient {

    public static void addClientListeners(FMLClientSetupEvent event) {
        SteamCompressorRenderer.register();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_COMPRESSOR.get())
            .factory(SteamCompressorVisual::new)
            .apply();
    }
}