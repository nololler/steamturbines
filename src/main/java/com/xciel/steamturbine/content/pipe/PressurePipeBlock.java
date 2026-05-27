package com.xciel.steamturbine.content.pipe;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressurePipeBlock extends Block implements IBE<PressurePipeBlockEntity> {
    public PressurePipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<PressurePipeBlockEntity> getBlockEntityClass() {
        return PressurePipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurePipeBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PRESSURE_PIPE.get();
    }
}