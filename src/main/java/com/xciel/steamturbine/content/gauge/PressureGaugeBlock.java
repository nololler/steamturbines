package com.xciel.steamturbine.content.gauge;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressureGaugeBlock extends Block implements IBE<PressureGaugeBlockEntity> {
    public PressureGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<PressureGaugeBlockEntity> getBlockEntityClass() {
        return PressureGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressureGaugeBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PRESSURE_GAUGE.get();
    }
}