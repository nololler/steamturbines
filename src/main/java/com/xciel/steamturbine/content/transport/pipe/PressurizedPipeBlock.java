package com.xciel.steamturbine.content.transport.pipe;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressurizedPipeBlock extends Block implements IBE<PressurizedPipeBlockEntity> {
    public PressurizedPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<PressurizedPipeBlockEntity> getBlockEntityClass() {
        return PressurizedPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurizedPipeBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PRESSURE_PIPE.get();
    }
}