package com.xciel.steamturbine.content.shaft;

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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TurbineShaftBlock extends Block implements IBE<TurbineShaftBlockEntity>, IRotate {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 2.0, 14.0, 15.0, 14.0),
        Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0),  // shaft casing north
        Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0), // input from south
        Block.box(14.0, 3.0, 3.0, 16.0, 13.0, 13.0)  // exhaust east
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 2.0, 14.0, 15.0, 14.0),
        Block.box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0), // shaft casing east
        Block.box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0),   // input from west
        Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 2.0)    // exhaust north
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 2.0, 14.0, 15.0, 14.0),
        Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0), // shaft casing south
        Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0),   // input from north
        Block.box(0.0, 3.0, 3.0, 2.0, 13.0, 13.0)    // exhaust west
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 2.0, 14.0, 15.0, 14.0),
        Block.box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0),   // shaft casing west
        Block.box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0), // input from east
        Block.box(3.0, 3.0, 14.0, 13.0, 13.0, 16.0)  // exhaust south
    );

    public TurbineShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        var be = level.getBlockEntity(pos);
        if (be instanceof TurbineShaftBlockEntity shaft) {
            shaft.onNeighborChanged();
        }
    }

    // IRotate: shaft outputs on the face OPPOSITE to FACING (the "back")
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return getShaftOutputDirection(state).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == getShaftOutputDirection(state);
    }

    public Direction getShaftOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    public Direction getTurbineInputDirection(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    // IBE
    @Override
    public Class<TurbineShaftBlockEntity> getBlockEntityClass() {
        return TurbineShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineShaftBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.TURBINE_SHAFT.get();
    }
}
