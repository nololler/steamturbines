package com.xciel.steamturbine.content.dag;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DAGDirectionOption extends ValueBoxTransform.Sided {

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side == Direction.UP || side == Direction.DOWN;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction side = getSide();
        if (side == Direction.UP) {
            return VecHelper.voxelSpace(8, 15.5f, 8);
        } else if (side == Direction.DOWN) {
            return VecHelper.voxelSpace(8, 0.5f, 8);
        }
        return null;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 15.0f, 8);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);

        if (facing == Direction.NORTH) {
            TransformStack.of(ms).rotateYDegrees(180);
        } else if (facing == Direction.SOUTH) {
        } else if (facing == Direction.EAST) {
            TransformStack.of(ms).rotateYDegrees(270);
        } else if (facing == Direction.WEST) {
            TransformStack.of(ms).rotateYDegrees(90);
        }

        super.rotate(level, pos, state, ms);
    }

    // Vertical facing - scroll on N/S or E/W depending on AXIS_ALONG_FIRST
    public static class DAGDirectionOptionVertical extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(BlockState state, Direction side) {
            Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);
            boolean alongFirst = state.getValue(DirectionalAnalogGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE);

            if (facing == Direction.UP && !alongFirst) {
                return side == Direction.NORTH || side == Direction.SOUTH;
            }
            if (facing == Direction.UP && alongFirst) {
                return side == Direction.EAST || side == Direction.WEST;
            }
            if (facing == Direction.DOWN && !alongFirst) {
                return side == Direction.NORTH || side == Direction.SOUTH;
            }
            if (facing == Direction.DOWN && alongFirst) {
                return side == Direction.EAST || side == Direction.WEST;
            }
            return false;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction side = getSide();
            if (side == Direction.NORTH) {
                return VecHelper.voxelSpace(8, 8, 0.5f);
            } else if (side == Direction.SOUTH) {
                return VecHelper.voxelSpace(8, 8, 15.5f);
            } else if (side == Direction.EAST) {
                return VecHelper.voxelSpace(15.5f, 8, 8);
            } else if (side == Direction.WEST) {
                return VecHelper.voxelSpace(0.5f, 8, 8);
            }
            return null;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5f);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
        }
    }
}
