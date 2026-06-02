package com.xciel.steamturbine.client;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.compressor.SteamCompressorRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorVisual;
import com.xciel.steamturbine.content.pump.SteamPumpRenderer;
import com.xciel.steamturbine.content.pump.SteamPumpVisual;
import com.xciel.steamturbine.content.shaft.TurbineShaftRenderer;
import com.xciel.steamturbine.content.shaft.TurbineShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class SteamTurbineClient {

    // Toggle for pump visual: "visual" = use SteamPumpVisual, "renderer" = use SteamPumpRenderer
    private static final String PUMP_VISUAL_MODE = "visual";

    public static void addClientListeners(FMLClientSetupEvent event) {
        SteamCompressorRenderer.register();
        TurbineShaftRenderer.register();
        SteamPumpRenderer.register();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_COMPRESSOR.get())
            .factory(SteamCompressorVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.TURBINE_SHAFT.get())
            .factory(TurbineShaftVisual::new)
            .apply();

        if ("visual".equals(PUMP_VISUAL_MODE)) {
            SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_PUMP.get())
                .factory(SteamPumpVisual::new)
                .apply();
        }
    }
}