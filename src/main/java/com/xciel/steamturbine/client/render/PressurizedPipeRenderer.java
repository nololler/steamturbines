package com.xciel.steamturbine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xciel.steamturbine.client.STPartialModels;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamHelper;
import com.xciel.steamturbine.steam.SteamPressureTier;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PressurizedPipeRenderer extends SafeBlockEntityRenderer<PressurizedPipeBlockEntity> {

    public PressurizedPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PressurizedPipeBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        boolean hasAnyConnection = false;
        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir) && hasNeighborPipe(be, dir)) {
                hasAnyConnection = true;
                break;
            }
        }

        if (!hasAnyConnection) {
            renderAllSpouts(be, ms, buffer, light);
        } else {
            renderWithConnections(be, ms, buffer, light);
        }
    }

    private boolean hasNeighborPipe(PressurizedPipeBlockEntity be, Direction dir) {
        BlockPos neighborPos = be.getBlockPos().relative(dir);
        BlockState neighborState = be.getLevel().getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();
        return neighborBlock instanceof com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
    }

    private void renderAllSpouts(PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        float avgPressure = getAveragePressure(be);
        int color = getPressureColor(avgPressure);

        SuperByteBuffer coreBuffer = CachedBuffers.partial(STPartialModels.PIPE_CORE, be.getBlockState());
        coreBuffer.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir)) {
                renderSpout(dir, color, be, ms, buffer, light);
            }
        }
    }

    private void renderWithConnections(PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        float avgPressure = getAveragePressure(be);
        int color = getPressureColor(avgPressure);

        SuperByteBuffer coreBuffer = CachedBuffers.partial(STPartialModels.PIPE_CORE, be.getBlockState());
        coreBuffer.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir) && !hasNeighborPipe(be, dir)) {
                renderSpout(dir, color, be, ms, buffer, light);
            }
        }
    }

    private void renderSpout(Direction dir, int color, PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        SuperByteBuffer spoutBuffer = CachedBuffers.partialFacing(STPartialModels.PIPE_SPOUT, be.getBlockState(), dir);
        float pressure = be.getVisualPressure(dir);
        int spoutColor = getPressureColor(pressure);
        spoutBuffer.color(spoutColor).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private float getAveragePressure(PressurizedPipeBlockEntity be) {
        float total = 0f;
        int count = 0;
        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir)) {
                total += be.getVisualPressure(dir);
                count++;
            }
        }
        return count > 0 ? total / count : 0f;
    }

    private int getPressureColor(float pressure) {
        SteamPressureTier tier = SteamHelper.getPressureTier(pressure);
        return switch (tier) {
            case NONE -> 0xFF666666;
            case REGULAR -> 0xFF00FF00;
            case PRESSURED_1 -> 0xFFFFFF00;
            case PRESSURED_2 -> 0xFFFF8800;
            case PRESSURED_3 -> 0xFFFF0000;
        };
    }
}