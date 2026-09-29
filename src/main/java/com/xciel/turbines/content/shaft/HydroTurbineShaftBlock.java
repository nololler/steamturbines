package com.xciel.turbines.content.shaft;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fixed, upright kinetic shaft for Hydro Turbine chambers. */
public class HydroTurbineShaftBlock extends Block implements IBE<HydroTurbineShaftBlockEntity>, IRotate {

    public static final EnumProperty<Direction> FACING = EnumProperty.create(
        "facing", Direction.class, Direction.UP, Direction.DOWN);

    private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 13, 16);
    private static final VoxelShape SHAPE_DOWN = Block.box(0, 3, 0, 16, 16, 16);

    public HydroTurbineShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction facing = clickedFace.getAxis() == Axis.Y ? clickedFace : Direction.UP;
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        // The shaft stays vertical; horizontal rotations do not change its facing.
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? SHAPE_DOWN : SHAPE_UP;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public Direction getShaftOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Axis.Y;
    }

    @Override
    public Class<HydroTurbineShaftBlockEntity> getBlockEntityClass() {
        return HydroTurbineShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HydroTurbineShaftBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.HYDRO_TURBINE_SHAFT.get();
    }
}
