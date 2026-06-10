package com.xciel.steamturbine.content.compressor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SteamCompressorRenderer extends KineticBlockEntityRenderer<SteamCompressorBlockEntity> {

    public SteamCompressorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected BlockState getRenderedBlockState(SteamCompressorBlockEntity be) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,
            ((SteamCompressorBlock) be.getBlockState().getBlock()).getRotationAxis(be.getBlockState()));
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.STEAM_COMPRESSOR.get(),
            SteamCompressorRenderer::new);
    }
}