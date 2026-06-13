package com.xciel.steamturbine.content.nd;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.steamturbine.registrate.STBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetworkDiagnoserBlock extends DirectionalAxisKineticBlock implements IBE<NetworkDiagnoserBlockEntity> {

    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(4.0, 4.0, 14.0, 12.0, 12.0, 15.0),
        Block.box(1.0, 1.0, 4.0, 2.0, 15.0, 12.0),
        Block.box(1.0, 0.0, 1.0, 2.0, 1.0, 15.0),
        Block.box(1.0, 15.0, 1.0, 2.0, 16.0, 15.0),
        Block.box(14.0, 0.0, 1.0, 15.0, 1.0, 15.0),
        Block.box(14.0, 1.0, 4.0, 15.0, 15.0, 12.0),
        Block.box(14.0, 15.0, 1.0, 15.0, 16.0, 15.0),
        Block.box(4.0, 4.0, 1.0, 12.0, 12.0, 2.0)
    );

    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(1.0, 4.0, 4.0, 2.0, 12.0, 12.0),
        Block.box(4.0, 1.0, 1.0, 12.0, 15.0, 2.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 2.0),
        Block.box(1.0, 15.0, 1.0, 15.0, 16.0, 2.0),
        Block.box(1.0, 0.0, 14.0, 15.0, 1.0, 15.0),
        Block.box(4.0, 1.0, 14.0, 12.0, 15.0, 15.0),
        Block.box(1.0, 15.0, 14.0, 15.0, 16.0, 15.0),
        Block.box(14.0, 4.0, 4.0, 15.0, 12.0, 12.0)
    );

    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(4.0, 4.0, 1.0, 12.0, 12.0, 2.0),
        Block.box(14.0, 1.0, 4.0, 15.0, 15.0, 12.0),
        Block.box(14.0, 0.0, 1.0, 15.0, 1.0, 15.0),
        Block.box(14.0, 15.0, 1.0, 15.0, 16.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 2.0, 1.0, 15.0),
        Block.box(1.0, 1.0, 4.0, 2.0, 15.0, 12.0),
        Block.box(1.0, 15.0, 1.0, 2.0, 16.0, 15.0),
        Block.box(4.0, 4.0, 14.0, 12.0, 12.0, 15.0)
    );

    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(14.0, 4.0, 4.0, 15.0, 12.0, 12.0),
        Block.box(4.0, 1.0, 14.0, 12.0, 15.0, 15.0),
        Block.box(1.0, 0.0, 14.0, 15.0, 1.0, 15.0),
        Block.box(1.0, 15.0, 14.0, 15.0, 16.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 2.0),
        Block.box(4.0, 1.0, 1.0, 12.0, 15.0, 2.0),
        Block.box(1.0, 15.0, 1.0, 15.0, 16.0, 2.0),
        Block.box(1.0, 4.0, 4.0, 2.0, 12.0, 12.0)
    );

    public NetworkDiagnoserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Class<NetworkDiagnoserBlockEntity> getBlockEntityClass() {
        return NetworkDiagnoserBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetworkDiagnoserBlockEntity> getBlockEntityType() {
        return STBlockEntityTypes.NETWORK_DIAGNOSER.get();
    }
}