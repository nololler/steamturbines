package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TurbineShaftDirectionOption extends ValueBoxTransform.Sided {

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side == Direction.UP;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return VecHelper.voxelSpace(8, 13, 8);
    }

    @Override
    protected Vec3 getSouthLocation() {
        return Vec3.ZERO;
    }
}