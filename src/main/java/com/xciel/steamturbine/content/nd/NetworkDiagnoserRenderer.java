package com.xciel.steamturbine.content.nd;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkDiagnoserRenderer extends KineticBlockEntityRenderer<NetworkDiagnoserBlockEntity> {

    public NetworkDiagnoserRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(NetworkDiagnoserBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction outputDir = state.getValue(NetworkDiagnoserBlock.FACING).getOpposite();

        SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, outputDir);
        renderRotatingBuffer(be, shaftBuffer, ms, buffer.getBuffer(RenderType.solid()), light);
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.NETWORK_DIAGNOSER.get(),
            NetworkDiagnoserRenderer::new);
    }
}