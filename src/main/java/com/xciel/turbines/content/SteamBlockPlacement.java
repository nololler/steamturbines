package com.xciel.turbines.content;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.turbines.steam.transfer.ICompressorEndpoint;
import com.xciel.turbines.steam.transfer.ISteamConsumer;
import com.xciel.turbines.steam.transfer.ISteamEndpoint;
import com.xciel.turbines.steam.transfer.ISteamProducer;
import com.xciel.turbines.steam.transfer.ISteamTransport;
import com.xciel.turbines.steam.transfer.ITurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.function.ToIntFunction;

/** Common Shift-aware facing and neighbour-snapping rules for steam machinery. */
public final class SteamBlockPlacement {

    private SteamBlockPlacement() {}

    public static Direction horizontalFacing(BlockPlaceContext context, ToIntFunction<Direction> connectionScore) {
        Direction fallback = context.getHorizontalDirection().getOpposite();
        if (isShiftDown(context))
            return fallback.getOpposite();
        Direction preferred = preferredFacing(context, fallback, true, connectionScore);
        return preferred == null ? fallback : preferred;
    }

    public static Direction facing(BlockPlaceContext context, ToIntFunction<Direction> connectionScore) {
        Direction fallback = context.getNearestLookingDirection().getOpposite();
        if (isShiftDown(context))
            return fallback.getOpposite();
        Direction preferred = preferredFacing(context, fallback, false, connectionScore);
        return preferred == null ? fallback : preferred;
    }

    public static Direction preferredFacing(BlockPlaceContext context, Direction fallback, boolean horizontalOnly,
                                              ToIntFunction<Direction> connectionScore) {
        if (isShiftDown(context))
            return null;

        Direction best = null;
        int bestScore = 0;
        int bestAlignment = Integer.MIN_VALUE;
        boolean ambiguous = false;

        for (Direction candidate : Direction.values()) {
            if (horizontalOnly && !candidate.getAxis().isHorizontal())
                continue;
            int score = connectionScore.applyAsInt(candidate);
            if (score <= 0)
                continue;

            int alignment = candidate.getStepX() * fallback.getStepX()
                + candidate.getStepY() * fallback.getStepY()
                + candidate.getStepZ() * fallback.getStepZ();
            if (score > bestScore || (score == bestScore && alignment > bestAlignment)) {
                best = candidate;
                bestScore = score;
                bestAlignment = alignment;
                ambiguous = false;
            } else if (score == bestScore && alignment == bestAlignment) {
                ambiguous = true;
            }
        }

        return best == null || ambiguous ? null : best;
    }

    public static boolean isShiftDown(BlockPlaceContext context) {
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
    }

    /** Direction here points from the block being placed toward its neighbour. */
    public static boolean hasSteamInputAt(BlockPlaceContext context, Direction direction) {
        BlockEntity neighbour = neighbour(context, direction);
        if (neighbour == null)
            return context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock()
                instanceof PressurizedPipeBlock;

        Direction neighbourFace = direction.getOpposite();
        if (neighbour instanceof ISteamProducer producer && producer.canProduce(neighbourFace))
            return true;
        if (neighbour instanceof ISteamTransport transport && transport.canConnect(neighbourFace))
            return true;
        return neighbour instanceof ITurbineEndpoint endpoint && endpoint.canTurbineConnect(neighbourFace);
    }

    /** Direction here points from the block being placed toward its neighbour. */
    public static boolean hasSteamOutputAt(BlockPlaceContext context, Direction direction) {
        BlockEntity neighbour = neighbour(context, direction);
        if (neighbour == null)
            return context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock()
                instanceof PressurizedPipeBlock;

        Direction neighbourFace = direction.getOpposite();
        if (neighbour instanceof ISteamConsumer consumer && consumer.canReceive(neighbourFace))
            return true;
        if (neighbour instanceof ICompressorEndpoint compressor
            && compressor.getCompressorOutputDirection() == neighbourFace.getOpposite())
            return true;
        if (neighbour instanceof ISteamTransport transport && !(neighbour instanceof ISteamProducer)
            && transport.canConnect(neighbourFace))
            return true;
        if (neighbour instanceof ISteamEndpoint endpoint && !(neighbour instanceof ISteamProducer)
            && endpoint.canConnect(neighbourFace))
            return true;
        return neighbour instanceof ITurbineEndpoint endpoint && endpoint.canTurbineConnect(neighbourFace);
    }

    /** Direction here points from the block being placed toward its neighbour. */
    public static boolean hasFluidConnectionAt(BlockPlaceContext context, Direction direction) {
        BlockPos neighbourPos = context.getClickedPos().relative(direction);
        if (!context.getLevel().isLoaded(neighbourPos))
            return false;
        BlockState neighbourState = context.getLevel().getBlockState(neighbourPos);
        return FluidPipeBlock.canConnectTo(context.getLevel(), neighbourPos, neighbourState, direction)
            || context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighbourPos, direction.getOpposite()) != null;
    }

    public static boolean hasFluidHandlerAt(BlockPlaceContext context, Direction direction) {
        BlockPos neighbourPos = context.getClickedPos().relative(direction);
        if (!context.getLevel().isLoaded(neighbourPos))
            return false;
        return context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighbourPos, direction.getOpposite()) != null
            || context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighbourPos, null) != null;
    }

    /** Checks whether an adjacent kinetic block exposes a shaft along the requested axis. */
    public static boolean hasKineticConnectionOnAxis(BlockPlaceContext context, Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis)
                continue;
            BlockPos neighbourPos = context.getClickedPos().relative(direction);
            if (!context.getLevel().isLoaded(neighbourPos))
                continue;
            BlockState neighbourState = context.getLevel().getBlockState(neighbourPos);
            if (neighbourState.getBlock() instanceof IRotate rotate
                && rotate.hasShaftTowards(context.getLevel(), neighbourPos, neighbourState, direction.getOpposite()))
                return true;
        }
        return false;
    }

    public static boolean hasKineticConnectionAt(BlockPlaceContext context, Direction direction) {
        BlockPos neighbourPos = context.getClickedPos().relative(direction);
        if (!context.getLevel().isLoaded(neighbourPos))
            return false;
        BlockState neighbourState = context.getLevel().getBlockState(neighbourPos);
        return neighbourState.getBlock() instanceof IRotate rotate
            && rotate.hasShaftTowards(context.getLevel(), neighbourPos, neighbourState, direction.getOpposite());
    }

    private static BlockEntity neighbour(BlockPlaceContext context, Direction direction) {
        BlockPos neighbourPos = context.getClickedPos().relative(direction);
        if (!context.getLevel().hasChunkAt(neighbourPos))
            return null;
        return context.getLevel().getBlockEntity(neighbourPos);
    }
}
