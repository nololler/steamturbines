package com.xciel.turbines.content.hydro_turbine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.xciel.turbines.registrate.STBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.UseOnContext;

/** The placed controller for the 3x3x1 Hydro Turbine assembly. */
public class HydroTurbineBlock extends Block implements IWrenchable {

    /** Direction of the upper water inlet; the lower outlet points the opposite way. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Generated from models/block/turbine/hydroturbine.json with voxelshape_gen.py.
    private static final VoxelShape SHAPE_NORTH = combine(
        Block.box(0.1875, 0.75, 0.4375, 0.8125, 1, 0.5625),
        Block.box(0.3125, 0.75, 0.375, 0.6875, 1, 0.625),
        Block.box(0.1875, 0, 0.4375, 0.8125, 0.25, 0.5625),
        Block.box(0.3125, 0, 0.375, 0.6875, 0.25, 0.625),
        Block.box(0.1875, 0.4375, 0, 0.8125, 0.5625, 0.25),
        Block.box(0.3125, 0.375, 0, 0.6875, 0.625, 0.25),
        Block.box(0.3125, 0.375, 0.75, 0.6875, 0.625, 1),
        Block.box(0.1875, 0.4375, 0.75, 0.8125, 0.5625, 1),
        Block.box(0.1875, 0.25, 0.4375, 0.8125, 0.75, 0.5625),
        Block.box(0.1875, 0.4375, 0.25, 0.8125, 0.5625, 0.75),
        Block.box(0, 0.75, 0, 0.125, 1, 0.25),
        Block.box(0, 0.3125, 0.3125, 0.125, 0.6875, 0.6875),
        Block.box(0, 0, 0, 0.125, 0.25, 0.25),
        Block.box(0, 0, 0.75, 0.125, 0.25, 1),
        Block.box(0, 0.75, 0.75, 0.125, 1, 1),
        Block.box(0.125, 0, 0, 0.1875, 1, 1),
        Block.box(0.875, 0.75, 0.75, 1, 1, 1),
        Block.box(0.875, 0.3125, 0.3125, 1, 0.6875, 0.6875),
        Block.box(0.875, 0.75, 0, 1, 1, 0.25),
        Block.box(0.8125, 0, 0, 0.875, 1, 1),
        Block.box(0.875, 0, 0, 1, 0.25, 0.25),
        Block.box(0.875, 0, 0.75, 1, 0.25, 1)
    );

    private static final VoxelShape SHAPE_EAST = combine(
        Block.box(0.4375, 0.75, 0.1875, 0.5625, 1, 0.8125),
        Block.box(0.375, 0.75, 0.3125, 0.625, 1, 0.6875),
        Block.box(0.4375, 0, 0.1875, 0.5625, 0.25, 0.8125),
        Block.box(0.375, 0, 0.3125, 0.625, 0.25, 0.6875),
        Block.box(0, 0.4375, 0.1875, 0.25, 0.5625, 0.8125),
        Block.box(0, 0.375, 0.3125, 0.25, 0.625, 0.6875),
        Block.box(0.75, 0.375, 0.3125, 1, 0.625, 0.6875),
        Block.box(0.75, 0.4375, 0.1875, 1, 0.5625, 0.8125),
        Block.box(0, 0.375, 0.3125, 0.25, 0.625, 0.6875),
        Block.box(0, 0.4375, 0.1875, 0.25, 0.5625, 0.8125),
        Block.box(0.375, 0.75, 0.3125, 0.625, 1, 0.6875),
        Block.box(0.4375, 0.75, 0.1875, 0.5625, 1, 0.8125),
        Block.box(0.375, 0, 0.3125, 0.625, 0.25, 0.6875),
        Block.box(0.4375, 0, 0.1875, 0.5625, 0.25, 0.8125),
        Block.box(0.4375, 0.25, 0.1875, 0.5625, 0.75, 0.8125),
        Block.box(0.25, 0.4375, 0.1875, 0.75, 0.5625, 0.8125),
        Block.box(0, 0.75, 0.875, 0.25, 1, 1),
        Block.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 1),
        Block.box(0, 0, 0.875, 0.25, 0.25, 1),
        Block.box(0.75, 0, 0.875, 1, 0.25, 1),
        Block.box(0.75, 0.75, 0.875, 1, 1, 1),
        Block.box(0, 0, 0.8125, 1, 1, 0.875),
        Block.box(0.75, 0.75, 0, 1, 1, 0.125),
        Block.box(0.3125, 0.3125, 0, 0.6875, 0.6875, 0.125),
        Block.box(0, 0.75, 0, 0.25, 1, 0.125),
        Block.box(0, 0, 0.125, 1, 1, 0.1875),
        Block.box(0, 0, 0, 0.25, 0.25, 0.125),
        Block.box(0.75, 0, 0, 1, 0.25, 0.125)
    );

    private static final VoxelShape SHAPE_SOUTH = combine(
        Block.box(0.1875, 0.75, 0.4375, 0.8125, 1, 0.5625),
        Block.box(0.3125, 0.75, 0.375, 0.6875, 1, 0.625),
        Block.box(0.1875, 0, 0.4375, 0.8125, 0.25, 0.5625),
        Block.box(0.3125, 0, 0.375, 0.6875, 0.25, 0.625),
        Block.box(0.1875, 0.4375, 0.75, 0.8125, 0.5625, 1),
        Block.box(0.3125, 0.375, 0.75, 0.6875, 0.625, 1),
        Block.box(0.3125, 0.375, 0, 0.6875, 0.625, 0.25),
        Block.box(0.1875, 0.4375, 0, 0.8125, 0.5625, 0.25),
        Block.box(0.1875, 0.25, 0.4375, 0.8125, 0.75, 0.5625),
        Block.box(0.1875, 0.4375, 0.25, 0.8125, 0.5625, 0.75),
        Block.box(0.875, 0.75, 0.75, 1, 1, 1),
        Block.box(0.875, 0.3125, 0.3125, 1, 0.6875, 0.6875),
        Block.box(0.875, 0, 0.75, 1, 0.25, 1),
        Block.box(0.875, 0, 0, 1, 0.25, 0.25),
        Block.box(0.875, 0.75, 0, 1, 1, 0.25),
        Block.box(0.8125, 0, 0, 0.875, 1, 1),
        Block.box(0, 0.75, 0, 0.125, 1, 0.25),
        Block.box(0, 0.3125, 0.3125, 0.125, 0.6875, 0.6875),
        Block.box(0, 0.75, 0.75, 0.125, 1, 1),
        Block.box(0.125, 0, 0, 0.1875, 1, 1),
        Block.box(0, 0, 0.75, 0.125, 0.25, 1),
        Block.box(0, 0, 0, 0.125, 0.25, 0.25)
    );

    private static final VoxelShape SHAPE_WEST = combine(
        Block.box(0.4375, 0.75, 0.1875, 0.5625, 1, 0.8125),
        Block.box(0.375, 0.75, 0.3125, 0.625, 1, 0.6875),
        Block.box(0.4375, 0, 0.1875, 0.5625, 0.25, 0.8125),
        Block.box(0.375, 0, 0.3125, 0.625, 0.25, 0.6875),
        Block.box(0.75, 0.4375, 0.1875, 1, 0.5625, 0.8125),
        Block.box(0.75, 0.375, 0.3125, 1, 0.625, 0.6875),
        Block.box(0, 0.375, 0.3125, 0.25, 0.625, 0.6875),
        Block.box(0, 0.4375, 0.1875, 0.25, 0.5625, 0.8125),
        Block.box(0.4375, 0.25, 0.1875, 0.5625, 0.75, 0.8125),
        Block.box(0.25, 0.4375, 0.1875, 0.75, 0.5625, 0.8125),
        Block.box(0.75, 0.75, 0, 1, 1, 0.125),
        Block.box(0.3125, 0.3125, 0, 0.6875, 0.6875, 0.125),
        Block.box(0.75, 0, 0, 1, 0.25, 0.125),
        Block.box(0, 0, 0, 0.25, 0.25, 0.125),
        Block.box(0, 0.75, 0, 0.25, 1, 0.125),
        Block.box(0, 0, 0.125, 1, 1, 0.1875),
        Block.box(0, 0.75, 0.875, 0.25, 1, 1),
        Block.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 1),
        Block.box(0.75, 0.75, 0.875, 1, 1, 1),
        Block.box(0, 0, 0.8125, 1, 1, 0.875),
        Block.box(0.75, 0, 0.875, 1, 0.25, 1),
        Block.box(0, 0, 0.875, 0.25, 0.25, 1)
    );

    private static VoxelShape combine(VoxelShape... parts) {
        VoxelShape shape = Shapes.empty();
        for (VoxelShape part : parts)
            shape = Shapes.or(shape, part);
        return shape;
    }

    public HydroTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction inlet = context.getHorizontalDirection().getOpposite();
        BlockPos masterPos = context.getClickedPos();
        if (!canPlaceStructure(context.getLevel(), masterPos, inlet))
            return null;
        return defaultBlockState().setValue(FACING, inlet);
    }

    private static boolean canPlaceStructure(LevelReader level, BlockPos masterPos, Direction inlet) {
        for (BlockPos partPos : getPartPositions(masterPos, inlet)) {
            if (!level.getBlockState(partPos).canBeReplaced())
                return false;
        }
        return true;
    }

    private static Iterable<BlockPos> getPartPositions(BlockPos masterPos, Direction inlet) {
        Direction across = Direction.get(Direction.AxisDirection.POSITIVE, inlet.getAxis());
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(8);
        for (int y = -1; y <= 1; y++) {
            for (int acrossOffset = -1; acrossOffset <= 1; acrossOffset++) {
                if (y == 0 && acrossOffset == 0)
                    continue;
                positions.add(masterPos.above(y).relative(across, acrossOffset));
            }
        }
        return positions;
    }

    private static Direction partDirectionToMaster(BlockPos masterPos, BlockPos partPos, Direction inlet) {
        int verticalDelta = masterPos.getY() - partPos.getY();
        if (verticalDelta != 0)
            return verticalDelta > 0 ? Direction.UP : Direction.DOWN;

        Direction across = Direction.get(Direction.AxisDirection.POSITIVE, inlet.getAxis());
        int acrossDelta = (masterPos.getX() - partPos.getX()) * across.getStepX()
            + (masterPos.getZ() - partPos.getZ()) * across.getStepZ();
        return acrossDelta > 0 ? across : across.getOpposite();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.getBlock() != oldState.getBlock())
            level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction inlet = state.getValue(FACING);
        if (!canPlaceStructure(level, pos, inlet)) {
            level.destroyBlock(pos, false);
            return;
        }

        for (BlockPos partPos : getPartPositions(pos, inlet)) {
            BlockState partState = STBlocks.HYDRO_TURBINE_PART.getDefaultState()
                .setValue(HydroTurbinePartBlock.FACING, partDirectionToMaster(pos, partPos, inlet));
            level.setBlockAndUpdate(partPos, partState);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            for (BlockPos partPos : getPartPositions(pos, state.getValue(FACING))) {
                if (level.getBlockState(partPos).is(STBlocks.HYDRO_TURBINE_PART.get()))
                    level.setBlock(partPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // The footprint is a multiblock. Rotate it by replacing it, not by changing only the controller state.
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The unrotated model has its inlet on South, so South uses the script's 0°/North shape.
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_NORTH;
            case NORTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_WEST;
            case WEST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public static BlockPos masterFromPart(BlockGetter level, BlockPos pos, BlockState partState) {
        return HydroTurbinePartBlock.findMaster(level, pos, partState);
    }

}
