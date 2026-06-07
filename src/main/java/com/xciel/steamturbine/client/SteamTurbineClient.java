package com.xciel.steamturbine.client;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.boilerTurbine.ShaftCasingVisual;
import com.xciel.steamturbine.content.compressor.SteamCompressorRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorVisual;
import com.xciel.steamturbine.content.pump.SteamPumpVisual;
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

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_PUMP.get())
            .factory(SteamPumpVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.BOILER_TURBINE_SHAFT_CASING.get())
            .factory(ShaftCasingVisual::new)
            .apply();
    }
}