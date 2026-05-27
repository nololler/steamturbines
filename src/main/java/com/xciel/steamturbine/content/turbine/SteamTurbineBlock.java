package com.xciel.steamturbine.content.turbine;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamTurbineBlock extends Block implements IBE<SteamTurbineBlockEntity> {
    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SteamTurbineBlockEntity> getBlockEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_TURBINE.get();
    }
}