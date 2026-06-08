package com.xciel.steamturbine.content.shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class LavaDuctShaftRenderer extends KineticBlockEntityRenderer<LavaDuctShaftBlockEntity> {

    public LavaDuctShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LavaDuctShaftBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        BlockPos pos = be.getBlockPos();

        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        Direction outputDir = ((LavaDuctShaftBlock) state.getBlock()).getShaftOutputDirection(state);
        Direction.Axis rotationAxis = outputDir.getAxis();

        float offset = getRotationOffsetForPosition(be, pos, rotationAxis);
        float angle = (time * be.getSpeed() * 3f / 10) % 360;
        angle += offset;
        angle = angle / 180f * (float) Math.PI;

        SuperByteBuffer northShaftBuffer = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, facing);
        kineticRotationTransform(northShaftBuffer, be, rotationAxis, angle, light);
        northShaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));

        SuperByteBuffer southShaftBuffer = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
        kineticRotationTransform(southShaftBuffer, be, rotationAxis, angle, light);
        southShaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.LAVA_DUCT_SHAFT.get(),
            LavaDuctShaftRenderer::new);
    }
}