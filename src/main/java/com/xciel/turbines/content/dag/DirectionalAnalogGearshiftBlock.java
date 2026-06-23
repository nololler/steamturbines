package com.xciel.turbines.content.dag;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.registrate.STBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public class DirectionalAnalogGearshiftBlock extends DirectionalAxisKineticBlock implements IBE<SplitShaftBlockEntity> {

    public static final IntegerProperty LEFT_POWER = IntegerProperty.create("left_power", 0, 15);
    public static final IntegerProperty RIGHT_POWER = IntegerProperty.create("right_power", 0, 15);

    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(4.0, 4.0, 1.0, 12.0, 12.0, 15.0),
        Block.box(0.0, 0.0, 4.0, 2.0, 16.0, 12.0),
        Block.box(14.0, 0.0, 4.0, 16.0, 16.0, 12.0),
        Block.box(2.0, 14.0, 4.0, 14.0, 16.0, 12.0),
        Block.box(2.0, 0.0, 4.0, 14.0, 2.0, 12.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0),
        Block.box(7.0, 2.0, 7.0, 9.0, 14.0, 9.0)
    );

    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(1.0, 4.0, 4.0, 15.0, 12.0, 12.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 2.0),
        Block.box(4.0, 0.0, 14.0, 12.0, 16.0, 16.0),
        Block.box(4.0, 14.0, 2.0, 12.0, 16.0, 14.0),
        Block.box(4.0, 0.0, 2.0, 12.0, 2.0, 14.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0),
        Block.box(7.0, 2.0, 7.0, 9.0, 14.0, 9.0)
    );

    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(4.0, 4.0, 1.0, 12.0, 12.0, 15.0),
        Block.box(14.0, 0.0, 4.0, 16.0, 16.0, 12.0),
        Block.box(0.0, 0.0, 4.0, 2.0, 16.0, 12.0),
        Block.box(2.0, 14.0, 4.0, 14.0, 16.0, 12.0),
        Block.box(2.0, 0.0, 4.0, 14.0, 2.0, 12.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0),
        Block.box(7.0, 2.0, 7.0, 9.0, 14.0, 9.0)
    );

    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(1.0, 4.0, 4.0, 15.0, 12.0, 12.0),
        Block.box(4.0, 0.0, 14.0, 12.0, 16.0, 16.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 2.0),
        Block.box(4.0, 14.0, 2.0, 12.0, 16.0, 14.0),
        Block.box(4.0, 0.0, 2.0, 12.0, 2.0, 14.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0),
        Block.box(7.0, 2.0, 7.0, 9.0, 14.0, 9.0)
    );

    public static final VoxelShape SHAPE_UP_NORTH = Shapes.or(
        Block.box(4.0, 1.0, 4.0, 12.0, 15.0, 12.0),
        Block.box(0.0, 4.0, 0.0, 2.0, 12.0, 16.0),
        Block.box(14.0, 4.0, 0.0, 16.0, 12.0, 16.0),
        Block.box(2.0, 4.0, 0.0, 14.0, 12.0, 2.0),
        Block.box(2.0, 4.0, 14.0, 14.0, 12.0, 16.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0)
    );

    public static final VoxelShape SHAPE_UP_EAST = Shapes.or(
        Block.box(4.0, 1.0, 4.0, 12.0, 15.0, 12.0),
        Block.box(0.0, 4.0, 0.0, 16.0, 12.0, 2.0),
        Block.box(0.0, 4.0, 14.0, 16.0, 12.0, 16.0),
        Block.box(14.0, 4.0, 2.0, 16.0, 12.0, 14.0),
        Block.box(0.0, 4.0, 2.0, 2.0, 12.0, 14.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0)
    );

    public static final VoxelShape SHAPE_DOWN_NORTH = Shapes.or(
        Block.box(4.0, 1.0, 4.0, 12.0, 15.0, 12.0),
        Block.box(0.0, 4.0, 0.0, 2.0, 12.0, 16.0),
        Block.box(14.0, 4.0, 0.0, 16.0, 12.0, 16.0),
        Block.box(2.0, 4.0, 14.0, 14.0, 12.0, 16.0),
        Block.box(2.0, 4.0, 0.0, 14.0, 12.0, 2.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0)
    );

    public static final VoxelShape SHAPE_DOWN_EAST = Shapes.or(
        Block.box(4.0, 1.0, 4.0, 12.0, 15.0, 12.0),
        Block.box(0.0, 4.0, 0.0, 16.0, 12.0, 2.0),
        Block.box(0.0, 4.0, 14.0, 16.0, 12.0, 16.0),
        Block.box(0.0, 4.0, 2.0, 2.0, 12.0, 14.0),
        Block.box(14.0, 4.0, 2.0, 16.0, 12.0, 14.0),
        Block.box(7.0, 7.0, 2.0, 9.0, 9.0, 14.0),
        Block.box(2.0, 7.0, 7.0, 14.0, 9.0, 9.0)
    );

    public DirectionalAnalogGearshiftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT_POWER, 0).setValue(RIGHT_POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LEFT_POWER).add(RIGHT_POWER));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> alongFirst ? SHAPE_UP_EAST : SHAPE_UP_NORTH;
            case DOWN -> alongFirst ? SHAPE_DOWN_EAST : SHAPE_DOWN_NORTH;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return getPoweredState(context.getLevel(), state, context.getClickedPos());
    }

    public BlockState getPoweredState(Level level, BlockState state, BlockPos pos) {
        Direction leftFace = getLeftFace(state);
        Direction rightFace = getRightFace(state);
        int leftPower = level.getSignal(pos.relative(leftFace), leftFace);
        int rightPower = level.getSignal(pos.relative(rightFace), rightFace);
        return state.setValue(LEFT_POWER, leftPower).setValue(RIGHT_POWER, rightPower);
    }

    public static Direction getLeftFace(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis().isHorizontal())
            return facing.getCounterClockWise();
        boolean alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        return alongFirst ? Direction.EAST : Direction.NORTH;
    }

    public static Direction getRightFace(BlockState state) {
        return getLeftFace(state).getOpposite();
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (worldIn.isClientSide) return;
        BlockState newState = getPoweredState(worldIn, state, pos);
        int newLeft = newState.getValue(LEFT_POWER);
        int newRight = newState.getValue(RIGHT_POWER);
        if (newLeft != state.getValue(LEFT_POWER) || newRight != state.getValue(RIGHT_POWER)) {
            detachKinetics(worldIn, pos, true);
            worldIn.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
        if (oldState.getBlock() == state.getBlock()) return;
        if (worldIn.isClientSide) return;
        detachKinetics(worldIn, pos, true);
    }

    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
        BlockEntity be = worldIn.getBlockEntity(pos);
        if (be == null || !(be instanceof KineticBlockEntity kte)) return;
        RotationPropagator.handleAdded(worldIn, pos, kte);
    }

    public void detachKinetics(Level worldIn, BlockPos pos, boolean reAttachNextTick) {
        BlockEntity be = worldIn.getBlockEntity(pos);
        if (be == null || !(be instanceof KineticBlockEntity)) return;
        RotationPropagator.handleRemoved(worldIn, pos, (KineticBlockEntity) be);
        if (reAttachNextTick)
            worldIn.scheduleTick(pos, this, 1, TickPriority.EXTREMELY_HIGH);
    }

    @Override
    public Class<SplitShaftBlockEntity> getBlockEntityClass() {
        return SplitShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SplitShaftBlockEntity> getBlockEntityType() {
        return STBlockEntityTypes.DIRECTIONAL_ANALOG_GEARSHIFT.get();
    }
}
