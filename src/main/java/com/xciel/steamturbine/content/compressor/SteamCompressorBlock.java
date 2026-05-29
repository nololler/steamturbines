package com.xciel.steamturbine.content.compressor;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class SteamCompressorBlock extends Block implements IBE<SteamCompressorBlockEntity>, IRotate {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(1.0, 3.0, 2.0, 15.0, 5.0, 14.0),
        Block.box(4.0, 5.0, 2.0, 12.0, 12.0, 14.0),
        Block.box(3.0, 3.0, 14.0, 13.0, 13.0, 16.0),
        Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0),
        Block.box(1.0, 5.0, 5.0, 4.0, 11.0, 11.0),
        Block.box(12.0, 5.0, 5.0, 15.0, 11.0, 11.0)
    );

    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 1.0, 14.0, 5.0, 15.0),
        Block.box(2.0, 5.0, 4.0, 14.0, 12.0, 12.0),
        Block.box(0.0, 3.0, 3.0, 2.0, 13.0, 13.0),
        Block.box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0),
        Block.box(5.0, 5.0, 1.0, 11.0, 11.0, 4.0),
        Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 15.0)
    );

    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(1.0, 3.0, 2.0, 15.0, 5.0, 14.0),
        Block.box(4.0, 5.0, 2.0, 12.0, 12.0, 14.0),
        Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 2.0),
        Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0),
        Block.box(12.0, 5.0, 5.0, 15.0, 11.0, 11.0),
        Block.box(1.0, 5.0, 5.0, 4.0, 11.0, 11.0)
    );

    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(2.0, 3.0, 1.0, 14.0, 5.0, 15.0),
        Block.box(2.0, 5.0, 4.0, 14.0, 12.0, 12.0),
        Block.box(14.0, 3.0, 3.0, 16.0, 13.0, 13.0),
        Block.box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0),
        Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 15.0),
        Block.box(5.0, 5.0, 1.0, 11.0, 11.0, 4.0)
    );

    public SteamCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public Class<SteamCompressorBlockEntity> getBlockEntityClass() {
        return SteamCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamCompressorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_COMPRESSOR.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SteamCompressorBlockEntity compressor) {
            compressor.updateConnectionStates();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
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
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis faceAxis = face.getAxis();
        Direction.Axis rotationAxis = getRotationAxis(state);
        return faceAxis != rotationAxis;
    }
}