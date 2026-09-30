package com.xciel.turbines.content.shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class HydroTurbineShaftRenderer extends KineticBlockEntityRenderer<HydroTurbineShaftBlockEntity> {

    private static final FluidStack CHAMBER_WATER = new FluidStack(Fluids.WATER, 1000);

    public HydroTurbineShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HydroTurbineShaftBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (be.shouldRenderWaterColumn())
            renderWaterColumn(be, ms, buffer, light);

        Direction facing = be.getBlockState().getValue(HydroTurbineShaftBlock.FACING);
        SuperByteBuffer shaft = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, be.getBlockState(), facing);
        renderRotatingBuffer(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light);
    }

    private static void renderWaterColumn(HydroTurbineShaftBlockEntity be, PoseStack ms,
                                         MultiBufferSource buffer, int light) {
        int stages = be.getChamberStages();
        if (stages <= 0)
            return;
        boolean roofShaft = be.getBlockState().getValue(HydroTurbineShaftBlock.FACING) == Direction.UP;
        float wallInset = 1 / 16f;
        float xMin = -1 + wallInset;
        float xMax = 2 - wallInset;
        float chamberBottom = roofShaft ? -stages + wallInset : 1 + wallInset;
        float chamberTop = roofShaft ? -wallInset : stages + 1 - wallInset;
        float progress = be.getWaterTransitionProgress();
        float yMin = be.isWaterActiveForRender()
            ? chamberTop - (chamberTop - chamberBottom) * progress : chamberBottom;
        float yMax = be.isWaterActiveForRender()
            ? chamberTop : chamberTop - (chamberTop - chamberBottom) * progress;
        if (yMax - yMin <= 0.001f)
            return;
        float zMin = -1 + wallInset;
        float zMax = 2 - wallInset;

        ms.pushPose();
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(CHAMBER_WATER, xMin, yMin, zMin,
            xMax, yMax, zMax, buffer, ms, light, false, true);
        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HydroTurbineShaftBlockEntity be) {
        return be.shouldRenderWaterColumn();
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.turbines.AllBlockEntityTypes.HYDRO_TURBINE_SHAFT.get(),
            HydroTurbineShaftRenderer::new);
    }
}
