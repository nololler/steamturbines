package com.xciel.steamturbine.content.boiler;

import com.xciel.steamturbine.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Rotation;

public class SteamBoilerBlock extends Block implements IBE<SteamBoilerBlockEntity> {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Shapes.or(
        Shapes.box(0/16.0, 0/16.0, 0/16.0, 16/16.0, 3/16.0, 16/16.0),
        Shapes.box(2/16.0, 3/16.0, 2/16.0, 14/16.0, 14/16.0, 14/16.0),
        Shapes.box(0/16.0, 14/16.0, 0/16.0, 3/16.0, 16/16.0, 3/16.0),
        Shapes.box(13/16.0, 14/16.0, 0/16.0, 16/16.0, 16/16.0, 3/16.0),
        Shapes.box(0/16.0, 14/16.0, 13/16.0, 3/16.0, 16/16.0, 16/16.0),
        Shapes.box(13/16.0, 14/16.0, 13/16.0, 16/16.0, 16/16.0, 16/16.0),
        Shapes.box(3/16.0, 14/16.0, 0/16.0, 13/16.0, 16/16.0, 2/16.0),
        Shapes.box(3/16.0, 14/16.0, 14/16.0, 13/16.0, 16/16.0, 16/16.0),
        Shapes.box(0/16.0, 14/16.0, 3/16.0, 2/16.0, 16/16.0, 13/16.0),
        Shapes.box(14/16.0, 14/16.0, 3/16.0, 16/16.0, 16/16.0, 13/16.0)
    );

    public SteamBoilerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public Class<SteamBoilerBlockEntity> getBlockEntityClass() {
        return SteamBoilerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamBoilerBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_BOILER.get();
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
        if (be instanceof SteamBoilerBlockEntity boiler) {
            boiler.updateConnectionStates();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
}