package com.xciel.turbines.content.ejector;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.AllBlockEntityTypes;
import com.xciel.turbines.content.SteamBlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteamEjectorBlock extends Block implements IBE<SteamEjectorBlockEntity>, IWrenchable {

    public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = BooleanProperty.create("axis_along_first");

    public SteamEjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.NORTH)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, AXIS_ALONG_FIRST_COORDINATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = SteamBlockPlacement.facing(context, candidateFacing -> {
            boolean alongFirst = isAxisAlongFirst(context, candidateFacing);
            BlockState candidateState = defaultBlockState()
                .setValue(BlockStateProperties.FACING, candidateFacing)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
            Direction steamInput = getSteamInputDirection(candidateState);
            int score = SteamBlockPlacement.fluidTransferScore(context, candidateFacing);
            int steamScore = SteamBlockPlacement.steamFlowScore(context,
                side -> side == steamInput || side == steamInput.getOpposite()
                    ? SteamBlockPlacement.SteamPort.INPUT : SteamBlockPlacement.SteamPort.NONE);
            return score + Math.min(steamScore, 4);
        });

        boolean alongFirst = isAxisAlongFirst(context, facing);

        return defaultBlockState()
            .setValue(BlockStateProperties.FACING, facing)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
    }

    private static boolean isAxisAlongFirst(BlockPlaceContext context, Direction facing) {
        if (facing.getAxis().isVertical())
            return context.getHorizontalDirection().getAxis() == Axis.X;
        return facing.getAxis() == Axis.Z;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot.ordinal() % 2 == 1)
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        return state.setValue(BlockStateProperties.FACING, rot.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    public static Direction getSteamInputDirection(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        if (facing.getAxis().isVertical())
            return alongFirst ? Direction.NORTH : Direction.EAST;
        return Direction.fromAxisAndDirection(facing.getClockWise().getAxis(), Direction.AxisDirection.NEGATIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof SteamEjectorBlockEntity ejector)
            ejector.onNeighborChanged();
    }

    @Override
    public Class<SteamEjectorBlockEntity> getBlockEntityClass() {
        return SteamEjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamEjectorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_EJECTOR.get();
    }
}
