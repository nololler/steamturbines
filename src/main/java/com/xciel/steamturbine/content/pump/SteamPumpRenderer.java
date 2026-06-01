package com.xciel.steamturbine.content.pump;

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

public class SteamPumpRenderer extends KineticBlockEntityRenderer<SteamPumpBlockEntity> {

    public SteamPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SteamPumpBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SteamPumpBlock pumpBlock)) return;

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        BlockPos pos = be.getBlockPos();

        Direction.Axis rotationAxis = pumpBlock.getRotationAxis(state);

        float offset = getRotationOffsetForPosition(be, pos, rotationAxis);
        float angle = (time * be.getSpeed() * 3f / 10) % 360;
        angle += offset;
        angle = angle / 180f * (float) Math.PI;

        Direction.Axis blockAxis = rotationAxis;

        for (Direction side : Direction.values()) {
            if (side.getAxis() != blockAxis) continue;
            if (!pumpBlock.hasShaftTowards(be.getLevel(), pos, state, side)) continue;

            SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF, state, side);
            kineticRotationTransform(shaftBuffer, be, blockAxis, angle, light);
            shaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.STEAM_PUMP.get(),
            SteamPumpRenderer::new);
    }
}