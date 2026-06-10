package com.xciel.steamturbine.content.pump;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SteamPumpRenderer extends KineticBlockEntityRenderer<SteamPumpBlockEntity> {

    public SteamPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected BlockState getRenderedBlockState(SteamPumpBlockEntity be) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,
            ((SteamPumpBlock) be.getBlockState().getBlock()).getRotationAxis(be.getBlockState()));
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.steamturbine.AllBlockEntityTypes.STEAM_PUMP.get(),
            SteamPumpRenderer::new);
    }
}