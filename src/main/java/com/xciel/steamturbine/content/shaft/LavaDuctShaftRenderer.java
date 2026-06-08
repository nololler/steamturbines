package com.xciel.steamturbine.content.shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
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
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        BlockPos pos = be.getBlockPos();

        float offset = getRotationOffsetForPosition(be, pos, Direction.Axis.Z);
        float angle = (time * be.getSpeed() * 3f / 10) % 360;
        angle += offset;
        angle = angle / 180f * (float) Math.PI;

        Direction dir1 = facing.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        Direction dir2 = dir1.getOpposite();
        Direction.Axis rotationAxis = dir1.getAxis();

        SuperByteBuffer dir1Buffer = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, dir1);
        kineticRotationTransform(dir1Buffer, be, rotationAxis, angle, light);
        dir1Buffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));

        SuperByteBuffer dir2Buffer = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, dir2);
        kineticRotationTransform(dir2Buffer, be, rotationAxis, angle, light);
        dir2Buffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.LAVA_DUCT_SHAFT.get(),
            LavaDuctShaftRenderer::new);
    }
}