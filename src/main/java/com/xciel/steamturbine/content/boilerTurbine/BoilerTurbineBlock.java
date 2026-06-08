package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.steamturbine.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoilerTurbineBlock extends DirectionalAxisKineticBlock implements IBE<BoilerTurbineBlockEntity>, IRotate {

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(1.0, 3.0, 4.0, 2.0, 11.0, 12.0),
        Block.box(14.0, 3.0, 4.0, 15.0, 11.0, 12.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 11.0, 2.0),
        Block.box(4.0, 3.0, 14.0, 12.0, 11.0, 15.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(14.0, 3.0, 4.0, 15.0, 11.0, 12.0),
        Block.box(1.0, 3.0, 4.0, 2.0, 11.0, 12.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(4.0, 3.0, 14.0, 12.0, 11.0, 15.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 11.0, 2.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    private static final VoxelShape SHAPE_UP = Shapes.or(
        Block.box(3.0, 3.0, 1.0, 13.0, 13.0, 2.0),
        Block.box(2.0, 2.0, 9.0, 14.0, 14.0, 12.0),
        Block.box(3.0, 3.0, 5.0, 13.0, 13.0, 8.0),
        Block.box(1.0, 1.0, 2.0, 15.0, 15.0, 4.0),
        Block.box(1.0, 1.0, 13.0, 15.0, 15.0, 16.0),
        Block.box(1.0, 4.0, 5.0, 2.0, 12.0, 13.0),
        Block.box(14.0, 4.0, 5.0, 15.0, 12.0, 13.0),
        Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 13.0)
    );

    private static final VoxelShape SHAPE_UP_EAST = Shapes.or(
        Block.box(3.0, 3.0, 1.0, 13.0, 13.0, 2.0),
        Block.box(2.0, 2.0, 9.0, 14.0, 14.0, 12.0),
        Block.box(3.0, 3.0, 5.0, 13.0, 13.0, 8.0),
        Block.box(1.0, 1.0, 2.0, 15.0, 15.0, 4.0),
        Block.box(1.0, 1.0, 13.0, 15.0, 15.0, 16.0),
        Block.box(1.0, 4.0, 5.0, 2.0, 12.0, 13.0),
        Block.box(14.0, 4.0, 5.0, 15.0, 12.0, 13.0),
        Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 13.0)
    );

    private static final VoxelShape SHAPE_DOWN = Shapes.or(
        Block.box(3.0, 3.0, 14.0, 13.0, 13.0, 15.0),
        Block.box(2.0, 2.0, 4.0, 14.0, 14.0, 7.0),
        Block.box(3.0, 3.0, 8.0, 13.0, 13.0, 11.0),
        Block.box(1.0, 1.0, 12.0, 15.0, 15.0, 14.0),
        Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 3.0),
        Block.box(1.0, 4.0, 3.0, 2.0, 12.0, 11.0),
        Block.box(14.0, 4.0, 3.0, 15.0, 12.0, 11.0),
        Block.box(4.0, 4.0, 3.0, 12.0, 12.0, 12.0)
    );

    private static final VoxelShape SHAPE_DOWN_EAST = Shapes.or(
        Block.box(3.0, 3.0, 14.0, 13.0, 13.0, 15.0),
        Block.box(2.0, 2.0, 4.0, 14.0, 14.0, 7.0),
        Block.box(3.0, 3.0, 8.0, 13.0, 13.0, 11.0),
        Block.box(1.0, 1.0, 12.0, 15.0, 15.0, 14.0),
        Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 3.0),
        Block.box(1.0, 4.0, 3.0, 2.0, 12.0, 11.0),
        Block.box(14.0, 4.0, 3.0, 15.0, 12.0, 11.0),
        Block.box(4.0, 4.0, 3.0, 12.0, 12.0, 12.0)
    );

    public BoilerTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> alongFirst ? SHAPE_UP_EAST : SHAPE_UP;
            case DOWN -> alongFirst ? SHAPE_DOWN_EAST : SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Class<BoilerTurbineBlockEntity> getBlockEntityClass() {
        return BoilerTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoilerTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.BOILER_TURBINE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);

        if (facing == Direction.UP || facing == Direction.DOWN) {
            return alongFirst ? Direction.Axis.Z : Direction.Axis.X;
        }

        return facing.getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, Direction face) {
        Direction facing = state.getValue(FACING);
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);

        if (facing == Direction.UP || facing == Direction.DOWN) {
            Direction.Axis blockAxis = alongFirst ? Direction.Axis.Z : Direction.Axis.X;
            return face.getAxis() == blockAxis;
        }

        Direction.Axis blockAxis = facing.getClockWise().getAxis();
        return face.getAxis() == blockAxis;
    }
}