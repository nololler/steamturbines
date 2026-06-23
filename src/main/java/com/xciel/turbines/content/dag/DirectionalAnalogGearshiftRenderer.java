package com.xciel.turbines.content.dag;

import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DirectionalAnalogGearshiftRenderer extends SplitShaftRenderer {

    public DirectionalAnalogGearshiftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.turbines.AllBlockEntityTypes.DIRECTIONAL_ANALOG_GEARSHIFT.get(),
            DirectionalAnalogGearshiftRenderer::new);
    }
}