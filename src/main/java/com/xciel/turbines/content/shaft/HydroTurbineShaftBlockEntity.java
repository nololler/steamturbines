package com.xciel.turbines.content.shaft;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Aggregation and Hydro chamber validation will be added with the water-flow system. */
public class HydroTurbineShaftBlockEntity extends GeneratingKineticBlockEntity {

    public HydroTurbineShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public float getGeneratedSpeed() {
        return 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = 0;
        return 0;
    }
}
