package com.xciel.turbines.content.shaft;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TurbineShaftDirectionOption extends ValueBoxTransform.Sided {

    private static final float POSITION_X = 8;
    private static final float POSITION_Y = 14.5f;
    private static final float POSITION_Z = 8;

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side == Direction.UP;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return VecHelper.voxelSpace(POSITION_X, POSITION_Y, POSITION_Z);
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(POSITION_X, POSITION_Y, POSITION_Z);
    }
}
