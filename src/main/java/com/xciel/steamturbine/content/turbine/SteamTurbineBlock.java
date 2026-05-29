package com.xciel.steamturbine.content.turbine;

import com.simibubi.create.foundation.block.IBE;
import com.xciel.steamturbine.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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

public class SteamTurbineBlock extends Block implements IBE<SteamTurbineBlockEntity> {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0),
        Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 12.0, 15.0),
        Block.box(1.0, 3.0, 8.0, 15.0, 15.0, 10.0),
        Block.box(2.0, 3.0, 6.0, 14.0, 14.0, 8.0),
        Block.box(0.0, 3.0, 11.0, 16.0, 16.0, 14.0),
        Block.box(0.0, 3.0, 2.0, 16.0, 16.0, 5.0)
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0),
        Block.box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0),
        Block.box(1.0, 3.0, 4.0, 15.0, 12.0, 12.0),
        Block.box(6.0, 3.0, 1.0, 8.0, 15.0, 15.0),
        Block.box(8.0, 3.0, 2.0, 10.0, 14.0, 14.0),
        Block.box(2.0, 3.0, 0.0, 5.0, 16.0, 16.0),
        Block.box(11.0, 3.0, 0.0, 14.0, 16.0, 16.0)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0),
        Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 12.0, 15.0),
        Block.box(1.0, 3.0, 6.0, 15.0, 15.0, 8.0),
        Block.box(2.0, 3.0, 8.0, 14.0, 14.0, 10.0),
        Block.box(0.0, 3.0, 2.0, 16.0, 16.0, 5.0),
        Block.box(0.0, 3.0, 11.0, 16.0, 16.0, 14.0)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0),
        Block.box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0),
        Block.box(1.0, 3.0, 4.0, 15.0, 12.0, 12.0),
        Block.box(8.0, 3.0, 1.0, 10.0, 15.0, 15.0),
        Block.box(6.0, 3.0, 2.0, 8.0, 14.0, 14.0),
        Block.box(11.0, 3.0, 0.0, 14.0, 16.0, 16.0),
        Block.box(2.0, 3.0, 0.0, 5.0, 16.0, 16.0)
    );

    public SteamTurbineBlock(Properties properties) {
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
        if (be instanceof SteamTurbineBlockEntity turbine) {
            turbine.onNeighborChanged();
        }
    }

    @Override
    public Class<SteamTurbineBlockEntity> getBlockEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_TURBINE.get();
    }
}
