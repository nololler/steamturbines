package com.xciel.steamturbine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.core.Direction;

public class PressurizedPipeRenderer extends SafeBlockEntityRenderer<PressurizedPipeBlockEntity> {

    public PressurizedPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PressurizedPipeBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        Direction[] directions = Iterate.directions;
        int connectedCount = 0;
        for (Direction dir : directions) {
            if (be.canConnect(dir)) {
                connectedCount++;
            }
        }
        if (connectedCount == 0) {
            renderCoreOnly(be, ms, buffer, light);
            return;
        }
        boolean isStraight = connectedCount == 2 && isOppositeConnections(be);
        if (isStraight) {
            renderStraight(be, ms, buffer, light);
        } else {
            renderHub(be, ms, buffer, light);
        }
    }

    private boolean isOppositeConnections(PressurizedPipeBlockEntity be) {
        int oppositePairs = 0;
        if (be.canConnect(Direction.NORTH) && be.canConnect(Direction.SOUTH)) oppositePairs++;
        if (be.canConnect(Direction.EAST) && be.canConnect(Direction.WEST)) oppositePairs++;
        if (be.canConnect(Direction.UP) && be.canConnect(Direction.DOWN)) oppositePairs++;
        return oppositePairs == 1;
    }

    private void renderCoreOnly(PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        SuperByteBuffer coreBuffer = CachedBuffers.partial(STPartialModels.PIPE_CORE, be.getBlockState());
        coreBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private void renderStraight(PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        float avgPressure = getAveragePressure(be);
        int color = getPressureColor(avgPressure);
        SuperByteBuffer coreBuffer = CachedBuffers.partial(STPartialModels.PIPE_CORE, be.getBlockState());
        coreBuffer.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir)) {
                renderSpout(be, dir, ms, buffer, light);
            }
        }
    }

    private void renderHub(PressurizedPipeBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        float avgPressure = getAveragePressure(be);
        int color = getPressureColor(avgPressure);
        SuperByteBuffer hubBuffer = CachedBuffers.partial(STPartialModels.PIPE_HUB, be.getBlockState());
        hubBuffer.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        for (Direction dir : Iterate.directions) {
            if (be.canConnect(dir)) {
                renderSpout(be, dir, ms, buffer, light);
            }
        }
    }

    private void renderSpout(PressurizedPipeBlockEntity be, Direction dir, PoseStack ms, MultiBufferSource buffer, int light) {
        SuperByteBuffer spoutBuffer = CachedBuffers.partialFacing(STPartialModels.PIPE_SPOUT, be.getBlockState(), dir);
        float pressure = be.getVisualPressure(dir);
        int color = getPressureColor(pressure);
        spoutBuffer.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
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