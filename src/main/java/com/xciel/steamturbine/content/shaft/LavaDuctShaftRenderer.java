package com.xciel.steamturbine.content.shaft;

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

public class LavaDuctShaftRenderer extends KineticBlockEntityRenderer<LavaDuctShaftBlockEntity> {

    public LavaDuctShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LavaDuctShaftBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);

        SuperByteBuffer shaft1 = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing);
        renderRotatingBuffer(be, shaft1, ms, buffer.getBuffer(RenderType.solid()), light);

        SuperByteBuffer shaft2 = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
        renderRotatingBuffer(be, shaft2, ms, buffer.getBuffer(RenderType.solid()), light);
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.LAVA_DUCT_SHAFT.get(),
            LavaDuctShaftRenderer::new);
    }
}