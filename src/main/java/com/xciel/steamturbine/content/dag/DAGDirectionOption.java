package com.xciel.steamturbine.content.dag;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DAGDirectionOption extends ValueBoxTransform.Sided {

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);
        Direction.Axis facingAxis = facing.getAxis();

        if (side == Direction.UP || side == Direction.DOWN) {
            return true;
        }

        if (facingAxis == Direction.Axis.Z) {
            return side == Direction.EAST || side == Direction.WEST;
        } else if (facingAxis == Direction.Axis.X) {
            return side == Direction.NORTH || side == Direction.SOUTH;
        }

        return false;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);
        Direction.Axis facingAxis = facing.getAxis();
        Direction side = getSide();

        if (side == Direction.UP) {
            return VecHelper.voxelSpace(8, 15.5f, 8);
        } else if (side == Direction.DOWN) {
            return VecHelper.voxelSpace(8, 0.5f, 8);
        }

        if (facingAxis == Direction.Axis.Z) {
            if (side == Direction.EAST) {
                return VecHelper.voxelSpace(15.5f, 8, 8);
            } else if (side == Direction.WEST) {
                return VecHelper.voxelSpace(0.5f, 8, 8);
            }
        } else if (facingAxis == Direction.Axis.X) {
            if (side == Direction.NORTH) {
                return VecHelper.voxelSpace(8, 8, 0.5f);
            } else if (side == Direction.SOUTH) {
                return VecHelper.voxelSpace(8, 8, 15.5f);
            }
        }

        return null;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 15.0f, 8);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction side = getSide();
        float yRot = AngleHelper.horizontalAngle(side) + 180;
        float xRot = side == Direction.UP ? 90 : side == Direction.DOWN ? 270 : 0;
        TransformStack.of(ms)
            .rotateYDegrees(yRot)
            .rotateXDegrees(xRot);
    }

}
