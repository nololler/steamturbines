package com.xciel.turbines.content.boiler;

import com.xciel.turbines.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class SteamBoilerBlock extends Block implements IBE<SteamBoilerBlockEntity>, IWrenchable {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(2.0, 3.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(13.0, 14.0, 0.0, 16.0, 16.0, 16.0),
        Block.box(0.0, 14.0, 0.0, 3.0, 16.0, 16.0),
        Block.box(3.0, 14.0, 0.0, 13.0, 16.0, 3.0),
        Block.box(3.0, 14.0, 13.0, 13.0, 16.0, 16.0),
        Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 2.0),
        Block.box(13.0, 3.0, 0.0, 16.0, 6.0, 3.0),
        Block.box(0.0, 3.0, 0.0, 3.0, 6.0, 3.0),
        Block.box(13.0, 11.0, 0.0, 16.0, 14.0, 3.0),
        Block.box(0.0, 11.0, 0.0, 3.0, 14.0, 3.0),
        Block.box(13.0, 6.0, 1.0, 15.0, 11.0, 3.0),
        Block.box(1.0, 6.0, 1.0, 3.0, 11.0, 3.0),
        Block.box(0.0, 3.0, 13.0, 3.0, 6.0, 16.0),
        Block.box(13.0, 3.0, 13.0, 16.0, 6.0, 16.0),
        Block.box(0.0, 11.0, 13.0, 3.0, 14.0, 16.0),
        Block.box(13.0, 11.0, 13.0, 16.0, 14.0, 16.0),
        Block.box(13.0, 6.0, 13.0, 15.0, 11.0, 15.0),
        Block.box(1.0, 6.0, 13.0, 3.0, 11.0, 15.0)
    );

    public static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(2.0, 3.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(0.0, 14.0, 13.0, 16.0, 16.0, 16.0),
        Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 3.0),
        Block.box(13.0, 14.0, 3.0, 16.0, 16.0, 13.0),
        Block.box(0.0, 14.0, 3.0, 3.0, 16.0, 13.0),
        Block.box(14.0, 3.0, 3.0, 16.0, 13.0, 13.0),
        Block.box(13.0, 3.0, 13.0, 16.0, 6.0, 16.0),
        Block.box(13.0, 3.0, 0.0, 16.0, 6.0, 3.0),
        Block.box(13.0, 11.0, 13.0, 16.0, 14.0, 16.0),
        Block.box(13.0, 11.0, 0.0, 16.0, 14.0, 3.0),
        Block.box(13.0, 6.0, 13.0, 15.0, 11.0, 15.0),
        Block.box(13.0, 6.0, 1.0, 15.0, 11.0, 3.0),
        Block.box(0.0, 3.0, 0.0, 3.0, 6.0, 3.0),
        Block.box(0.0, 3.0, 13.0, 3.0, 6.0, 16.0),
        Block.box(0.0, 11.0, 0.0, 3.0, 14.0, 3.0),
        Block.box(0.0, 11.0, 13.0, 3.0, 14.0, 16.0),
        Block.box(1.0, 6.0, 13.0, 3.0, 11.0, 15.0),
        Block.box(1.0, 6.0, 1.0, 3.0, 11.0, 3.0)
    );

    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(2.0, 3.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(0.0, 14.0, 0.0, 3.0, 16.0, 16.0),
        Block.box(13.0, 14.0, 0.0, 16.0, 16.0, 16.0),
        Block.box(3.0, 14.0, 13.0, 13.0, 16.0, 16.0),
        Block.box(3.0, 14.0, 0.0, 13.0, 16.0, 3.0),
        Block.box(3.0, 3.0, 14.0, 13.0, 13.0, 16.0),
        Block.box(0.0, 3.0, 13.0, 3.0, 6.0, 16.0),
        Block.box(13.0, 3.0, 13.0, 16.0, 6.0, 16.0),
        Block.box(0.0, 11.0, 13.0, 3.0, 14.0, 16.0),
        Block.box(13.0, 11.0, 13.0, 16.0, 14.0, 16.0),
        Block.box(1.0, 6.0, 13.0, 3.0, 11.0, 15.0),
        Block.box(13.0, 6.0, 13.0, 15.0, 11.0, 15.0),
        Block.box(13.0, 3.0, 0.0, 16.0, 6.0, 3.0),
        Block.box(0.0, 3.0, 0.0, 3.0, 6.0, 3.0),
        Block.box(13.0, 11.0, 0.0, 16.0, 14.0, 3.0),
        Block.box(0.0, 11.0, 0.0, 3.0, 14.0, 3.0),
        Block.box(1.0, 6.0, 1.0, 3.0, 11.0, 3.0),
        Block.box(13.0, 6.0, 1.0, 15.0, 11.0, 3.0)
    );

    public static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(2.0, 3.0, 2.0, 14.0, 14.0, 14.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
        Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 3.0),
        Block.box(0.0, 14.0, 13.0, 16.0, 16.0, 16.0),
        Block.box(0.0, 14.0, 3.0, 3.0, 16.0, 13.0),
        Block.box(13.0, 14.0, 3.0, 16.0, 16.0, 13.0),
        Block.box(0.0, 3.0, 3.0, 2.0, 13.0, 13.0),
        Block.box(0.0, 3.0, 0.0, 3.0, 6.0, 3.0),
        Block.box(0.0, 3.0, 13.0, 3.0, 6.0, 16.0),
        Block.box(0.0, 11.0, 0.0, 3.0, 14.0, 3.0),
        Block.box(0.0, 11.0, 13.0, 3.0, 14.0, 16.0),
        Block.box(1.0, 6.0, 1.0, 3.0, 11.0, 3.0),
        Block.box(1.0, 6.0, 13.0, 3.0, 11.0, 15.0),
        Block.box(13.0, 3.0, 13.0, 16.0, 6.0, 16.0),
        Block.box(13.0, 3.0, 0.0, 16.0, 6.0, 3.0),
        Block.box(13.0, 11.0, 13.0, 16.0, 14.0, 16.0),
        Block.box(13.0, 11.0, 0.0, 16.0, 14.0, 3.0),
        Block.box(13.0, 6.0, 1.0, 15.0, 11.0, 3.0),
        Block.box(13.0, 6.0, 13.0, 15.0, 11.0, 15.0)
    );

    public SteamBoilerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public Class<SteamBoilerBlockEntity> getBlockEntityClass() {
        return SteamBoilerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamBoilerBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.STEAM_BOILER.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        SteamBoilerBlockEntity be = getBlockEntity(level, pos);
        if (be == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack held = stack;

        if (held.getItem() instanceof BucketItem bucket) {
            FluidStack bucketContent = new FluidStack(bucket.content, 1000);
            IFluidHandler fluidHandler = be.getFluidHandler();
            if (fluidHandler.fill(bucketContent, IFluidHandler.FluidAction.SIMULATE) >= 1000) {
                fluidHandler.fill(bucketContent, IFluidHandler.FluidAction.EXECUTE);
                if (!player.isCreative()) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                be.setChanged();
                be.sendData();
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (held.is(Items.BUCKET)) {
            boolean fromFuel = be.getFuelFluidTank().getFluidAmount() >= 1000;
            IFluidHandler source = fromFuel ? be.getFuelFluidTank() : be.getFluidHandler();
            FluidStack drained = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                source.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                if (!player.isCreative()) {
                    player.setItemInHand(hand, new ItemStack(drained.getFluid().getBucket()));
                }
                be.setChanged();
                be.sendData();
                return ItemInteractionResult.SUCCESS;
            }
        }

        IItemHandler handler = be.getItemHandler();
        IItemHandlerModifiable modHandler = (IItemHandlerModifiable) handler;
        ItemStack currentSlot = handler.getStackInSlot(0);

        if (held.isEmpty()) {
            if (!currentSlot.isEmpty()) {
                ItemStack toGive = currentSlot.copy();
                modHandler.setStackInSlot(0, ItemStack.EMPTY);
                if (player.getInventory().add(toGive)) {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, player.getSoundSource(), 0.5f, 1.2f);
                }
                be.setChanged();
                be.sendData();
            }
            return ItemInteractionResult.SUCCESS;
        }

        int maxInsert = handler.getSlotLimit(0);
        int canAccept = maxInsert - currentSlot.getCount();
        if (canAccept <= 0)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack toInsert = held.copy();
        toInsert.setCount(Math.min(held.getCount(), canAccept));

        ItemStack remainder = ItemHandlerHelper.insertItem(handler, toInsert, false);
        int inserted = held.getCount() - remainder.getCount();
        if (inserted > 0) {
            held.shrink(inserted);
            player.setItemInHand(hand, held);
            be.setChanged();
            be.sendData();
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, neighborPos, isMoving);
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SteamBoilerBlockEntity boiler) {
            boiler.updateConnectionStates();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
            return;

        SteamBoilerBlockEntity be = getBlockEntity(level, pos);
        if (be != null && !isMoving) {
            ItemStack fuel = be.getItemHandler().getStackInSlot(0);
            if (!fuel.isEmpty()) {
                Block.popResource(level, pos, fuel);
            }
        }
        level.removeBlockEntity(pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }
}