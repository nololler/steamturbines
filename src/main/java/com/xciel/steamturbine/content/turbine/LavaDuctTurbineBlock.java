package com.xciel.steamturbine.content.turbine;

import com.simibubi.create.foundation.block.IBE;
import com.xciel.steamturbine.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LavaDuctTurbineBlock extends Block implements IBE<LavaDuctTurbineBlockEntity> {

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(4.0, 1.0, 4.0, 12.0, 15.0, 12.0),
        Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0),
        Block.box(0.0, 12.0, 0.0, 16.0, 13.0, 16.0),
        Block.box(0.0, 9.0, 0.0, 16.0, 10.0, 16.0),
        Block.box(0.0, 6.0, 0.0, 16.0, 7.0, 16.0),
        Block.box(0.0, 3.0, 0.0, 16.0, 4.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)
    );

    public LavaDuctTurbineBlock(Properties properties) {
        super(properties);
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        var be = level.getBlockEntity(pos);
        if (be instanceof LavaDuctTurbineBlockEntity turbine) {
            turbine.onNeighborChanged();
        }
    }

    @Override
    public Class<LavaDuctTurbineBlockEntity> getBlockEntityClass() {
        return LavaDuctTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LavaDuctTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.LAVA_DUCT_TURBINE.get();
    }
}