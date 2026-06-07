package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ShaftCasingBlock extends Block implements IBE<ShaftCasingBlockEntity>, IRotate {

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    public static final VoxelShape SHAPE = Shapes.or(
        Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0)
    );

    public ShaftCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        ShaftCasingBlockEntity be = getBlockEntity(level, pos);
        if (be != null) {
            be.onNeighborChanged();
        }
    }

    @Override
    public Class<ShaftCasingBlockEntity> getBlockEntityClass() {
        return ShaftCasingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShaftCasingBlockEntity> getBlockEntityType() {
        return com.xciel.steamturbine.AllBlockEntityTypes.BOILER_TURBINE_SHAFT_CASING.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    public static int getRotationForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 2;
            case WEST -> 3;
            case EAST -> 1;
            default -> 0;
        };
    }

    public static Direction getFacingFromRotation(int rotation) {
        return switch (rotation) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }
}