package com.xciel.turbines.content.shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class HydroTurbineShaftRenderer extends KineticBlockEntityRenderer<HydroTurbineShaftBlockEntity> {

    public HydroTurbineShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HydroTurbineShaftBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        Direction facing = be.getBlockState().getValue(HydroTurbineShaftBlock.FACING);
        SuperByteBuffer shaft = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, be.getBlockState(), facing);
        renderRotatingBuffer(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light);
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.turbines.AllBlockEntityTypes.HYDRO_TURBINE_SHAFT.get(),
            HydroTurbineShaftRenderer::new);
    }
}
