package com.xciel.steamturbine.content.boilerTurbine;

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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShaftCasingBlock extends Block implements IBE<ShaftCasingBlockEntity> {

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
        Block.box(0.0, 2.0, 0.0, 16.0, 14.0, 16.0)
    );

    public ShaftCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<ShaftCasingBlockEntity> getBlockEntityClass() {
        return ShaftCasingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShaftCasingBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.BOILER_TURBINE_SHAFT_CASING.get();
    }
}