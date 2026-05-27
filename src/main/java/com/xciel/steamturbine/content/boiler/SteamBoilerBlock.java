package com.xciel.steamturbine.content.boiler;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamBoilerBlock extends Block implements IBE<SteamBoilerBlockEntity> {
    public SteamBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SteamBoilerBlockEntity> getBlockEntityClass() {
        return SteamBoilerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamBoilerBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_BOILER.get();
    }
}