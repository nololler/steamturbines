package com.xciel.steamturbine.content.compressor;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamCompressorBlock extends Block implements IBE<SteamCompressorBlockEntity> {
    public SteamCompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SteamCompressorBlockEntity> getBlockEntityClass() {
        return SteamCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamCompressorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_COMPRESSOR.get();
    }
}