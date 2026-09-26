package com.xciel.turbines.content;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.xciel.turbines.content.boiler.SteamBoilerBlock;
import com.xciel.turbines.content.compressor.SteamCompressorBlock;
import com.xciel.turbines.content.ejector.SteamEjectorBlock;
import com.xciel.turbines.content.pump.SteamPumpBlock;
import com.xciel.turbines.content.sjth.SteamJetThrusterBlock;
import com.xciel.turbines.content.shaft.TurbineShaftBlock;
import com.xciel.turbines.content.turbine.SteamTurbineBlock;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlock;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.function.ToIntFunction;
import java.util.function.Function;

/** Common Shift-aware facing and neighbour-snapping rules for steam machinery. */
public final class SteamBlockPlacement {

    public enum SteamPort {
        NONE,
        INPUT,
        OUTPUT,
        BIDIRECTIONAL
    }

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

    /** Scores only compatible steam port pairs; output-to-output and input-to-input do not snap. */
    public static int steamFlowScore(BlockPlaceContext context, Function<Direction, SteamPort> placedPorts) {
        int score = 0;
        for (Direction side : Direction.values()) {
            SteamPort placed = placedPorts.apply(side);
            SteamPort neighbour = steamPortAt(context, side);
            if (placed == SteamPort.OUTPUT && neighbour == SteamPort.INPUT
                || placed == SteamPort.INPUT && neighbour == SteamPort.OUTPUT) {
                score += 12;
            } else if ((placed == SteamPort.INPUT || placed == SteamPort.OUTPUT)
                && neighbour == SteamPort.BIDIRECTIONAL) {
                score += 3;
            } else if (placed == SteamPort.BIDIRECTIONAL && neighbour != SteamPort.NONE) {
                score++;
            }
        }
        return score;
    }

    private static SteamPort steamPortAt(BlockPlaceContext context, Direction sideFromNewBlock) {
        BlockPos neighbourPos = context.getClickedPos().relative(sideFromNewBlock);
        if (!context.getLevel().hasChunkAt(neighbourPos))
            return SteamPort.NONE;

        BlockState state = context.getLevel().getBlockState(neighbourPos);
        Direction faceOnNeighbour = sideFromNewBlock.getOpposite();

        if (state.getBlock() instanceof PressurizedPipeBlock)
            return SteamPort.BIDIRECTIONAL;

        if (state.getBlock() instanceof SteamBoilerBlock)
            return state.getValue(SteamBoilerBlock.FACING) == faceOnNeighbour ? SteamPort.OUTPUT : SteamPort.NONE;

        if (state.getBlock() instanceof SteamCompressorBlock)
            return directionalPort(state.getValue(SteamCompressorBlock.FACING), faceOnNeighbour);

        if (state.getBlock() instanceof SteamPumpBlock)
            return directionalPort(state.getValue(SteamPumpBlock.FACING), faceOnNeighbour);

        if (state.getBlock() instanceof SteamTurbineBlock)
            return state.getValue(SteamTurbineBlock.FACING) == faceOnNeighbour
                ? SteamPort.OUTPUT : SteamPort.INPUT;

        if (state.getBlock() instanceof SteamJetThrusterBlock)
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                .getOpposite() == faceOnNeighbour ? SteamPort.INPUT : SteamPort.NONE;

        if (state.getBlock() instanceof SteamEjectorBlock) {
            Direction steamInput = SteamEjectorBlock.getSteamInputDirection(state);
            return faceOnNeighbour == steamInput || faceOnNeighbour == steamInput.getOpposite()
                ? SteamPort.INPUT : SteamPort.NONE;
        }

        if (state.getBlock() instanceof TurbineShaftBlock) {
            Direction facing = state.getValue(TurbineShaftBlock.FACING);
            if (faceOnNeighbour == facing.getOpposite())
                return SteamPort.INPUT;
            if (faceOnNeighbour == facing.getClockWise())
                return SteamPort.OUTPUT;
            return SteamPort.NONE;
        }

        BlockEntity neighbour = context.getLevel().getBlockEntity(neighbourPos);
        boolean output = neighbour instanceof ISteamProducer producer && producer.canProduce(faceOnNeighbour);
        boolean input = neighbour instanceof ISteamConsumer consumer && consumer.canReceive(faceOnNeighbour);
        if (output && input)
            return SteamPort.BIDIRECTIONAL;
        if (output)
            return SteamPort.OUTPUT;
        if (input)
            return SteamPort.INPUT;
        if (neighbour instanceof ISteamTransport transport && transport.canConnect(faceOnNeighbour))
            return SteamPort.BIDIRECTIONAL;
        if (neighbour instanceof ITurbineEndpoint endpoint && endpoint.canTurbineConnect(faceOnNeighbour))
            return SteamPort.BIDIRECTIONAL;
        if (neighbour instanceof ISteamEndpoint endpoint && endpoint.canConnect(faceOnNeighbour))
            return SteamPort.BIDIRECTIONAL;
        return SteamPort.NONE;
    }

    private static SteamPort directionalPort(Direction outputDirection, Direction faceOnNeighbour) {
        if (faceOnNeighbour == outputDirection)
            return SteamPort.OUTPUT;
        if (faceOnNeighbour == outputDirection.getOpposite())
            return SteamPort.INPUT;
        return SteamPort.NONE;
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

    /** Scores an inlet-to-outlet route using simulated transfer between directly adjacent handlers. */
    public static int fluidTransferScore(BlockPlaceContext context, Direction inlet) {
        FluidStack available = simulatedDrainAt(context, inlet);
        int score = 0;
        if (!available.isEmpty()) {
            score += 8;
            IFluidHandler outlet = fluidHandlerAt(context, inlet.getOpposite());
            if (outlet != null && outlet.fill(available.copy(), FluidAction.SIMULATE) > 0)
                score += 12;
        }

        // A pipe has no intrinsic flow direction until its network is pressurized.
        // Treat it as a weak placement hint and prefer the inlet side when otherwise tied.
        if (hasFluidConnectionAt(context, inlet))
            score += 2;
        if (hasFluidConnectionAt(context, inlet.getOpposite()))
            score++;
        return score;
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

    private static FluidStack simulatedDrainAt(BlockPlaceContext context, Direction direction) {
        IFluidHandler handler = fluidHandlerAt(context, direction);
        return handler == null ? FluidStack.EMPTY : handler.drain(1, FluidAction.SIMULATE);
    }

    private static IFluidHandler fluidHandlerAt(BlockPlaceContext context, Direction direction) {
        BlockPos neighbourPos = context.getClickedPos().relative(direction);
        if (!context.getLevel().isLoaded(neighbourPos))
            return null;
        IFluidHandler handler = context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
            neighbourPos, direction.getOpposite());
        if (handler == null)
            handler = context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighbourPos, null);
        return handler;
    }
}
