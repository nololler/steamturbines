package com.xciel.turbines.content.sjth;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteamJetThrusterBlock extends Block implements IBE<SteamJetThrusterBlockEntity>, IWrenchable {

    public SteamJetThrusterBlock(Properties properties) {
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
        return defaultBlockState().setValue(BlockStateProperties.FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(BlockStateProperties.FACING, rot.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        return switch (facing) {
            case NORTH, SOUTH -> Shapes.box(2 / 16f, 2 / 16f, 0, 14 / 16f, 14 / 16f, 16 / 16f);
            case EAST, WEST -> Shapes.box(0, 2 / 16f, 2 / 16f, 16 / 16f, 14 / 16f, 14 / 16f);
            case UP, DOWN -> Shapes.box(2 / 16f, 0, 2 / 16f, 14 / 16f, 16 / 16f, 14 / 16f);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Class<SteamJetThrusterBlockEntity> getBlockEntityClass() {
        return SteamJetThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamJetThrusterBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_JET_THRUSTER.get();
    }
}
