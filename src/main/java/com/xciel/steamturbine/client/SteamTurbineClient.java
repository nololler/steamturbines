package com.xciel.steamturbine.client;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.compressor.SteamCompressorRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorVisual;
import com.xciel.steamturbine.content.shaft.TurbineShaftRenderer;
import com.xciel.steamturbine.content.shaft.TurbineShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class SteamTurbineClient {

    public static void addClientListeners(FMLClientSetupEvent event) {
        SteamCompressorRenderer.register();
        TurbineShaftRenderer.register();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_COMPRESSOR.get())
            .factory(SteamCompressorVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.TURBINE_SHAFT.get())
            .factory(TurbineShaftVisual::new)
            .apply();
    }
}