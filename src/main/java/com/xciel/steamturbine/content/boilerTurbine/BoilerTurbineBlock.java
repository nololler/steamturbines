package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.steamturbine.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoilerTurbineBlock extends Block implements IBE<BoilerTurbineBlockEntity>, IRotate {

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

    public BoilerTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (facing == Direction.DOWN) {
            facing = Direction.UP;
        }
        return defaultBlockState().setValue(BlockStateProperties.FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(BlockStateProperties.FACING, rot.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public Class<BoilerTurbineBlockEntity> getBlockEntityClass() {
        return BoilerTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoilerTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.BOILER_TURBINE.get();
    }
}