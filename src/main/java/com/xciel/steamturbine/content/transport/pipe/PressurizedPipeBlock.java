package com.xciel.steamturbine.content.transport.pipe;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PressurizedPipeBlock extends Block implements IBE<PressurizedPipeBlockEntity> {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public PressurizedPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    public Class<PressurizedPipeBlockEntity> getBlockEntityClass() {
        return PressurizedPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurizedPipeBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PRESSURE_PIPE.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH).add(SOUTH).add(EAST).add(WEST).add(UP).add(DOWN);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PressurizedPipeBlockEntity pipe) {
            pipe.updateConnectionStates();
        }
    }

    public static boolean getConnection(BlockState state, Direction dir) {
        return switch (dir) {
            case NORTH -> state.getValue(NORTH);
            case SOUTH -> state.getValue(SOUTH);
            case EAST -> state.getValue(EAST);
            case WEST -> state.getValue(WEST);
            case UP -> state.getValue(UP);
            case DOWN -> state.getValue(DOWN);
        };
    }

    public static BlockState setConnection(BlockState state, Direction dir, boolean connected) {
        return switch (dir) {
            case NORTH -> state.setValue(NORTH, connected);
            case SOUTH -> state.setValue(SOUTH, connected);
            case EAST -> state.setValue(EAST, connected);
            case WEST -> state.setValue(WEST, connected);
            case UP -> state.setValue(UP, connected);
            case DOWN -> state.setValue(DOWN, connected);
        };
    }
}