package com.xciel.steamturbine.content.shaft;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineShaftBlock extends Block implements IBE<TurbineShaftBlockEntity> {
    public TurbineShaftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TurbineShaftBlockEntity> getBlockEntityClass() {
        return TurbineShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineShaftBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.TURBINE_SHAFT.get();
    }
}