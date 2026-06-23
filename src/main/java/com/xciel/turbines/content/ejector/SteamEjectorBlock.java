package com.xciel.turbines.content.ejector;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.AllBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
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
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            facing = facing.getOpposite();

        boolean alongFirst = false;
        if (facing.getAxis().isVertical()) {
            alongFirst = context.getHorizontalDirection().getAxis() == Axis.X;
        } else if (facing.getAxis().isHorizontal()) {
            alongFirst = facing.getAxis() == Axis.Z;
        }

        return defaultBlockState()
            .setValue(BlockStateProperties.FACING, facing)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot.ordinal() % 2 == 1)
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        return state.setValue(BlockStateProperties.FACING, rot.rotate(state.getValue(BlockStateProperties.FACING)));
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
    public Class<SteamEjectorBlockEntity> getBlockEntityClass() {
        return SteamEjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamEjectorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_EJECTOR.get();
    }
}