package com.xciel.turbines.client;

import com.xciel.turbines.AllBlockEntityTypes;
import com.xciel.turbines.content.compressor.SteamCompressorRenderer;
import com.xciel.turbines.content.compressor.SteamCompressorVisual;
import com.xciel.turbines.content.nd.NetworkDiagnoserRenderer;
import com.xciel.turbines.content.pump.SteamPumpRenderer;
import com.xciel.turbines.content.pump.SteamPumpVisual;
import com.xciel.turbines.content.shaft.TurbineShaftRenderer;
import com.xciel.turbines.content.shaft.TurbineShaftVisual;
import com.xciel.turbines.content.shaft.LavaDuctShaftRenderer;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class SteamTurbineClient {

    public static void addClientListeners(FMLClientSetupEvent event) {
        SteamCompressorRenderer.register();
        SteamPumpRenderer.register();
        TurbineShaftRenderer.register();
        LavaDuctShaftRenderer.register();

        DirectionalAnalogGearshiftRenderer.register();
        NetworkDiagnoserRenderer.register();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_COMPRESSOR.get())
            .factory(SteamCompressorVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.TURBINE_SHAFT.get())
            .factory(TurbineShaftVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.STEAM_PUMP.get())
            .factory(SteamPumpVisual::new)
            .apply();

        SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.DIRECTIONAL_ANALOG_GEARSHIFT.get())
            .factory(SplitShaftVisual::new)
            .apply();
    }
}