package com.xciel.turbines.content.hydro_turbine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.xciel.turbines.registrate.STBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible occupied cells belonging to a Hydro Turbine controller. */
public class HydroTurbinePartBlock extends Block implements IWrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final int MAX_MASTER_SEARCH = 3;

    public HydroTurbinePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            BlockPos master = findMaster(level, pos, state);
            if (master != null && !master.equals(pos))
                level.destroyBlock(master, true);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Resolve the controller through the directional links in the 3x3 support plane. */
    public static BlockPos findMaster(BlockGetter level, BlockPos pos, BlockState state) {
        return findMaster(level, pos, state, 0);
    }

    private static BlockPos findMaster(BlockGetter level, BlockPos pos, BlockState state, int depth) {
        if (state.getBlock() instanceof HydroTurbineBlock)
            return pos;
        if (!(state.getBlock() instanceof HydroTurbinePartBlock) || depth >= MAX_MASTER_SEARCH)
            return null;

        BlockPos nextPos = pos.relative(state.getValue(FACING));
        BlockState nextState = level.getBlockState(nextPos);
        if (nextState.getBlock() instanceof HydroTurbineBlock)
            return nextPos;
        return findMaster(level, nextPos, nextState, depth + 1);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return STBlocks.HYDRO_TURBINE.asStack();
    }
}
