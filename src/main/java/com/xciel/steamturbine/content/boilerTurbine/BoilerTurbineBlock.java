package com.xciel.steamturbine.content.boilerTurbine;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.xciel.steamturbine.AllBlockEntityTypes;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class BoilerTurbineBlock extends FaceAttachedHorizontalDirectionalBlock
    implements SimpleWaterloggedBlock, IWrenchable, IBE<BoilerTurbineBlockEntity> {

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(1.0, 3.0, 4.0, 2.0, 11.0, 12.0),
        Block.box(14.0, 3.0, 4.0, 15.0, 11.0, 12.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 11.0, 2.0),
        Block.box(4.0, 3.0, 14.0, 12.0, 11.0, 15.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(14.0, 3.0, 4.0, 15.0, 11.0, 12.0),
        Block.box(1.0, 3.0, 4.0, 2.0, 11.0, 12.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),
        Block.box(2.0, 4.0, 2.0, 14.0, 7.0, 14.0),
        Block.box(3.0, 8.0, 3.0, 13.0, 11.0, 13.0),
        Block.box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(4.0, 3.0, 14.0, 12.0, 11.0, 15.0),
        Block.box(4.0, 3.0, 1.0, 12.0, 11.0, 2.0),
        Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0)
    );

    public BoilerTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACE, AttachFace.FLOOR)
            .setValue(FACING, Direction.NORTH)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(FACE, FACING, WATERLOGGED));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return canAttach(pLevel, pPos, getConnectedDirection(pState).getOpposite());
    }

    public static boolean canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection) {
        BlockPos blockpos = pPos.relative(pDirection);
        return pReader.getBlockState(blockpos).getBlock() instanceof SteamBoilerBlock;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (placementHelper.matchesItem(stack)) {
            if (player.isShiftKeyDown()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BoilerTurbineBlockEntity turbineBE && turbineBE.shouldRespawnCasing()) {
                    turbineBE.markCasingRespawned();
                }
            }
            return placementHelper.getOffset(player, level, state, pos, hitResult)
                .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world,
                                  BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED))
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        return state;
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        updateBoilerState(pState, pLevel, pPos);
        BlockPos shaftPos = getShaftPos(pState, pPos);
        BlockState shaftState = pLevel.getBlockState(shaftPos);
        if (isShaftValid(pState, shaftState)) {
            pLevel.setBlock(shaftPos, getPoweredShaftEquivalent(shaftState), Block.UPDATE_ALL);
        }
        trySpawnCasing(pLevel, pPos, pState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.hasBlockEntity() && (!pState.is(pNewState.getBlock()) || !pNewState.hasBlockEntity()))
            pLevel.removeBlockEntity(pPos);
        updateBoilerState(pState, pLevel, pPos);
        BlockPos shaftPos = getShaftPos(pState, pPos);
        BlockState shaftState = pLevel.getBlockState(shaftPos);
        if (isPoweredShaft(shaftState))
            pLevel.scheduleTick(shaftPos, shaftState.getBlock(), 1);
        removeCasing(pLevel, pPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;

        BlockPos shaftPos = getShaftPos(state, pos);
        BlockPos casingPos = getCasingPos(pos);

        if (neighborPos.equals(shaftPos)) {
            BlockState shaftState = level.getBlockState(shaftPos);
            if (isShaftValid(state, shaftState) && !isPoweredShaft(shaftState)) {
                level.setBlock(shaftPos, getPoweredShaftEquivalent(shaftState), Block.UPDATE_ALL);
            }
            BoilerTurbineBlockEntity be = getBlockEntity(level, pos);
            if (be != null) {
                if (isShaftValid(state, shaftState)) {
                    be.onShaftPlaced();
                    trySpawnCasing(level, pos, state);
                }
            }
        }
    }

    public static BlockPos getCasingPos(BlockPos pos) {
        return pos.relative(Direction.UP);
    }

    private void trySpawnCasing(Level level, BlockPos pos, BlockState state) {
        BlockPos casingPos = getCasingPos(pos);
        if (level.hasChunkAt(casingPos)) {
            BlockState existing = level.getBlockState(casingPos);
            if (!existing.is(com.xciel.steamturbine.AllBlocks.BOILER_TURBINE_SHAFT_CASING.get())) {
                level.setBlock(casingPos, com.xciel.steamturbine.AllBlocks.BOILER_TURBINE_SHAFT_CASING.getDefaultState()
                    .setValue(ShaftCasingBlock.ROTATION, getCasingRotation(state.getValue(FACING))), Block.UPDATE_ALL);
                BoilerTurbineBlockEntity be = getBlockEntity(level, pos);
                if (be != null) {
                    be.onCasingSpawned();
                }
            }
        }
    }

    private void removeCasing(Level level, BlockPos pos) {
        BlockPos casingPos = getCasingPos(pos);
        if (level.hasChunkAt(casingPos)) {
            BlockState casingState = level.getBlockState(casingPos);
            if (casingState.is(com.xciel.steamturbine.AllBlocks.BOILER_TURBINE_SHAFT_CASING.get())) {
                level.removeBlockEntity(casingPos);
                level.setBlock(casingPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public static int getCasingRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 2;
            case WEST -> 3;
            case EAST -> 1;
            default -> 0;
        };
    }

    private void updateBoilerState(BlockState state, Level level, BlockPos pos) {
        Direction facing = getFacing(state);
        level.updateNeighbourForOutputSignal(pos.relative(facing.getOpposite()), state.getBlock());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        return switch (direction) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState ifluidstate = level.getFluidState(pos);
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        return state.setValue(WATERLOGGED, Boolean.valueOf(ifluidstate.getType() == Fluids.WATER));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    public static Direction getFacing(BlockState sideState) {
        return getConnectedDirection(sideState);
    }

    public static BlockPos getShaftPos(BlockState sideState, BlockPos pos) {
        return pos.relative(Direction.UP);
    }

    public static boolean isShaftValid(BlockState state, BlockState shaft) {
        return (AllBlocks.SHAFT.has(shaft) || isPoweredShaft(shaft))
            && shaft.getValue(ShaftBlock.AXIS) == Direction.Axis.Y;
    }

    public static boolean isPoweredShaft(BlockState state) {
        return AllBlocks.POWERED_SHAFT.has(state);
    }

    private BlockState getPoweredShaftEquivalent(BlockState shaftState) {
        return PoweredShaftBlock.getEquivalent(shaftState);
    }

    @Override
    public Class<BoilerTurbineBlockEntity> getBlockEntityClass() {
        return BoilerTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoilerTurbineBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.BOILER_TURBINE.get();
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return AllBlocks.SHAFT::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof BoilerTurbineBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
            BlockPos shaftPos = BoilerTurbineBlock.getShaftPos(state, pos);
            BlockState shaft = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);

            BlockState newState = world.getBlockState(shaftPos);
            if (!newState.canBeReplaced())
                return PlacementOffset.fail();

            return PlacementOffset.success(shaftPos,
                s -> BlockHelper
                    .copyProperties(s, AllBlocks.POWERED_SHAFT.getDefaultState())
                    .setValue(PoweredShaftBlock.AXIS, Direction.Axis.Y));
        }
    }

    public static Couple<Integer> getSpeedRange() {
        return Couple.create(16, 128);
    }

    public static Direction getConnectedDirection(BlockState state) {
        return FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state);
    }

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(BoilerTurbineBlock::new);
    }
}