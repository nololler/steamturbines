package com.xciel.turbines.content.ejector;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.turbines.steam.SteamData;
import com.xciel.turbines.steam.transfer.IPressurizedConsumer;
import com.xciel.turbines.steam.transfer.ISteamEndpoint;
import com.xciel.turbines.steam.transfer.ISteamProducer;
import com.xciel.turbines.steam.transfer.ISteamTransport;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SteamEjectorBlockEntity extends SmartBlockEntity implements
    IPressurizedConsumer, ISteamEndpoint, ISteamTransport, IHaveGoggleInformation {

    private static final float MAX_STEAM_CONSUMPTION_PER_TICK = 3f;
    private static final int MAX_EJECTOR_RANGE = 80;
    private static final float FLUID_TRANSFER_RATE_MILLIBUCKETS_PER_SECOND = 1000f;
    private static final float GAME_TICKS_PER_SECOND = 20f;
    private static final float FLUID_NETWORK_PRESSURE_PER_MILLIBUCKET_PER_TICK = 2f;

    private SteamData inputSteam = SteamData.empty();
    private SteamData pendingSteam = SteamData.empty();
    private SteamData lastAppliedSteam = SteamData.empty();
    private boolean pressureDirty = true;
    private int pressureCheckCooldown;

    public SteamEjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new EjectorFluidTransferBehaviour(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (pressureCheckCooldown > 0)
            pressureCheckCooldown--;

        SteamData currentSteam = pendingSteam;
        pendingSteam = SteamData.empty();
        currentSteam = addSteam(currentSteam, pullSteamFromInput(
            Math.max(0, MAX_STEAM_CONSUMPTION_PER_TICK - currentSteam.getThroughput())));
        if (!inputSteam.similarTo(currentSteam)) {
            inputSteam = currentSteam;
            pressureDirty = true;
            setChanged();
            sendData();
        }

        if (!pressureDirty && !inputSteam.isEmpty() && pressureCheckCooldown == 0
            && hasLostConnectedPipePressure())
            pressureDirty = true;

        if (pressureDirty || hasPressureSignalChanged())
            refreshFluidPressure();
    }

    public void onNeighborChanged() {
        pressureDirty = true;
    }

    private SteamData pullSteamFromInput(float requestedAmount) {
        SteamData pulledTotal = SteamData.empty();
        float remaining = requestedAmount;

        for (Direction face : Direction.values()) {
            if (remaining <= 0.01f || !canConnect(face.getOpposite()))
                continue;

            BlockPos neighborPos = worldPosition.relative(face);
            if (!level.isLoaded(neighborPos))
                continue;

            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            SteamData pulled = SteamData.empty();

            if (neighbor instanceof PressurizedPipeBlockEntity pipe) {
                pulled = pipe.pullSteamFromNetwork(face.getOpposite(), remaining);
            } else if (neighbor instanceof ISteamProducer producer) {
                Direction neighborFace = face.getOpposite();
                if (producer.canProduce(neighborFace)) {
                    SteamData available = producer.produceSteam(neighborFace);
                    if (!available.isEmpty() && available.getThroughput() > 0)
                        pulled = available.withThroughput(Math.min(available.getThroughput(), remaining));
                }
            } else if (neighbor instanceof ISteamTransport transport
                && transport.canConnect(face.getOpposite())) {
                pulled = transport.pullSteam(face.getOpposite(), remaining);
            }

            if (pulled.isEmpty() || pulled.getThroughput() <= 0)
                continue;

            float accepted = Math.min(pulled.getThroughput(), remaining);
            pulledTotal = addSteam(pulledTotal, pulled.withThroughput(accepted));
            remaining -= accepted;
        }

        return pulledTotal;
    }

    private SteamData addSteam(SteamData current, SteamData incoming) {
        if (incoming == null || incoming.isEmpty() || incoming.getThroughput() <= 0)
            return current;
        float added = Math.min(incoming.getThroughput(),
            Math.max(0, MAX_STEAM_CONSUMPTION_PER_TICK - current.getThroughput()));
        if (added <= 0)
            return current;
        if (current.isEmpty())
            return incoming.withThroughput(added);
        return current.withPressureAndThroughputAdded(incoming.getPressure(), added)
            .withThroughput(current.getThroughput() + added);
    }

    private void refreshFluidPressure() {
        FluidTransportBehaviour behaviour = getBehaviour(FluidTransportBehaviour.TYPE);
        if (behaviour == null || level == null || level.isClientSide)
            return;

        Direction facing = getFacing();

        // Clear old pressure across both connected pipe networks before laying down the new signal.
        for (Direction side : List.of(facing, facing.getOpposite())) {
            BlockPos neighborPos = worldPosition.relative(side);
            if (level.isLoaded(neighborPos))
                FluidPropagator.propagateChangedPipe(level, neighborPos, level.getBlockState(neighborPos));
        }
        behaviour.wipePressure();

        if (!inputSteam.isEmpty()) {
            float pressure = getFluidPressure();
            int range = getFluidRange();

            for (Direction side : List.of(facing, facing.getOpposite())) {
                boolean pull = isFront(side);
                distributePressureTo(side, pull, pressure, range);
            }
        }

        lastAppliedSteam = inputSteam;
        pressureDirty = false;
        pressureCheckCooldown = 10;
        setChanged();
        sendData();
    }

    /** Create recalculates vanilla pumps when a fluid graph changes, but not this custom pump. */
    private boolean hasLostConnectedPipePressure() {
        Direction facing = getFacing();
        for (Direction side : List.of(facing, facing.getOpposite())) {
            BlockPos pipePos = worldPosition.relative(side);
            if (!level.isLoaded(pipePos))
                continue;

            BlockState pipeState = level.getBlockState(pipePos);
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pipePos);
            if (pipe == null || !pipe.canHaveFlowToward(pipeState, side.getOpposite()))
                continue;

            PipeConnection connection = pipe.getConnection(side.getOpposite());
            boolean inbound = !isFront(side);
            if (connection == null || connection.getPressure().get(inbound) <= 0)
                return true;
        }
        return false;
    }

    private float getFluidPressure() {
        return getFluidPressure(inputSteam);
    }

    private int getFluidRange() {
        return getFluidRange(inputSteam);
    }

    private float getFluidPressure(SteamData steam) {
        if (steam.isEmpty())
            return 0;
        // Create's FluidNetwork uses pressure / 2 as mB per game tick.
        return FLUID_TRANSFER_RATE_MILLIBUCKETS_PER_SECOND / GAME_TICKS_PER_SECOND
            * FLUID_NETWORK_PRESSURE_PER_MILLIBUCKET_PER_TICK;
    }

    private int getFluidRange(SteamData steam) {
        int vanillaRange = Math.max(1, FluidPropagator.getPumpRange());
        if (steam.isEmpty())
            return vanillaRange;
        int steamBonus = (int) Math.ceil((steam.getPressure() + steam.getThroughput()) * 5f);
        return Math.max(vanillaRange, Math.min(MAX_EJECTOR_RANGE, vanillaRange + steamBonus));
    }

    private boolean hasPressureSignalChanged() {
        return Math.abs(getFluidPressure(inputSteam) - getFluidPressure(lastAppliedSteam)) >= 0.1f
            || getFluidRange(inputSteam) != getFluidRange(lastAppliedSteam)
            || inputSteam.getSteamType() != lastAppliedSteam.getSteamType();
    }

    /** Distribute pressure through Create fluid pipes, following Create's pump network traversal. */
    private void distributePressureTo(Direction side, boolean pull, float pressure, int maxDistance) {
        BlockFace start = new BlockFace(worldPosition, side);
        if (hasReachedValidEndpoint(start, pull))
            return;

        Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph = new HashMap<>();
        Set<BlockFace> targets = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();

        pipeGraph.computeIfAbsent(worldPosition, $ -> Pair.of(0, new IdentityHashMap<>()))
            .getSecond().put(side, pull);
        pipeGraph.computeIfAbsent(start.getConnectedPos(), $ -> Pair.of(1, new IdentityHashMap<>()))
            .getSecond().put(side.getOpposite(), !pull);
        frontier.add(Pair.of(1, start.getConnectedPos()));

        while (!frontier.isEmpty()) {
            Pair<Integer, BlockPos> entry = frontier.remove(0);
            int distance = entry.getFirst();
            BlockPos currentPos = entry.getSecond();

            if (currentPos.equals(worldPosition) || !level.isLoaded(currentPos) || !visited.add(currentPos))
                continue;

            BlockState currentState = level.getBlockState(currentPos);
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, currentPos);
            if (pipe == null)
                continue;

            for (Direction face : FluidPropagator.getPipeConnections(currentState, pipe)) {
                BlockFace blockFace = new BlockFace(currentPos, face);
                BlockPos connectedPos = blockFace.getConnectedPos();
                if (blockFace.isEquivalent(start) || !level.isLoaded(connectedPos))
                    continue;

                if (hasReachedValidEndpoint(blockFace, pull)) {
                    pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>()))
                        .getSecond().put(face, pull);
                    targets.add(blockFace);
                    continue;
                }

                BlockState connectedState = level.getBlockState(connectedPos);
                if (connectedState.getBlock() instanceof SteamEjectorBlock)
                    continue;

                FluidTransportBehaviour connectedPipe = FluidPropagator.getPipe(level, connectedPos);
                if (connectedPipe == null || !connectedPipe.canHaveFlowToward(connectedState, face.getOpposite()))
                    continue;
                if (visited.contains(connectedPos))
                    continue;

                if (distance + 1 >= maxDistance) {
                    pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>()))
                        .getSecond().put(face, pull);
                    targets.add(blockFace);
                    continue;
                }

                pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>()))
                    .getSecond().put(face, pull);
                pipeGraph.computeIfAbsent(connectedPos, $ -> Pair.of(distance + 1, new IdentityHashMap<>()))
                    .getSecond().put(face.getOpposite(), !pull);
                frontier.add(Pair.of(distance + 1, connectedPos));
            }
        }

        Map<Integer, Set<BlockFace>> validFaces = new HashMap<>();
        searchForEndpointRecursively(pipeGraph, targets, validFaces,
            new BlockFace(start.getPos(), start.getOppositeFace()), pull);

        for (Set<BlockFace> faces : validFaces.values()) {
            int parallelBranches = Math.max(1, faces.size() - 1);
            for (BlockFace face : faces) {
                BlockPos pipePos = face.getPos();
                if (pipePos.equals(worldPosition))
                    continue;
                Pair<Integer, Map<Direction, Boolean>> node = pipeGraph.get(pipePos);
                if (node == null)
                    continue;
                Boolean inbound = node.getSecond().get(face.getFace());
                if (inbound == null)
                    continue;
                FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pipePos);
                if (pipe != null)
                    pipe.addPressure(face.getFace(), inbound, pressure / parallelBranches);
            }
        }
    }

    private boolean searchForEndpointRecursively(
        Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph,
        Set<BlockFace> targets,
        Map<Integer, Set<BlockFace>> validFaces,
        BlockFace currentFace,
        boolean pull
    ) {
        BlockPos currentPos = currentFace.getPos();
        Pair<Integer, Map<Direction, Boolean>> node = pipeGraph.get(currentPos);
        if (node == null)
            return false;

        int distance = node.getFirst();
        boolean successful = false;
        for (Direction nextFacing : Direction.values()) {
            if (nextFacing == currentFace.getFace())
                continue;
            Map<Direction, Boolean> connections = node.getSecond();
            if (!connections.containsKey(nextFacing))
                continue;

            BlockFace target = new BlockFace(currentPos, nextFacing);
            if (targets.contains(target)) {
                validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(target);
                successful = true;
                continue;
            }

            if (connections.get(nextFacing) != pull)
                continue;
            if (!searchForEndpointRecursively(pipeGraph, targets, validFaces,
                new BlockFace(currentPos.relative(nextFacing), nextFacing.getOpposite()), pull))
                continue;

            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(target);
            successful = true;
        }

        if (successful)
            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(currentFace);
        return successful;
    }

    private boolean hasReachedValidEndpoint(BlockFace blockFace, boolean pull) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = level.getBlockState(connectedPos);
        BlockEntity blockEntity = level.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();

        if (PumpBlock.isPump(connectedState) && connectedState.getValue(PumpBlock.FACING).getAxis() == face.getAxis()
            && blockEntity instanceof PumpBlockEntity pump) {
            boolean pumpPulling = pump.isPullingOnSide(
                connectedState.getValue(PumpBlock.FACING) == blockFace.getOppositeFace());
            return pumpPulling != pull;
        }

        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace()))
            return false;

        if (blockEntity != null) {
            IFluidHandler capability = level.getCapability(Capabilities.FluidHandler.BLOCK,
                blockEntity.getBlockPos(), face.getOpposite());
            if (capability != null)
                return true;
        }

        return FluidPropagator.isOpenEnd(level, blockFace.getPos(), face);
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    public boolean isFront(Direction side) {
        return getFacing() == side;
    }

    private Direction getSteamInputDirection() {
        return SteamEjectorBlock.getSteamInputDirection(getBlockState());
    }

    @Override
    public boolean canReceive(Direction direction) {
        return canConnect(direction);
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty() || steam.getThroughput() <= 0 || !canReceive(direction))
            return;
        pendingSteam = addSteam(pendingSteam, steam);
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return canReceive(direction) ? MAX_STEAM_CONSUMPTION_PER_TICK : 0;
    }

    @Override
    public boolean canConnect(Direction direction) {
        Direction steamInput = getSteamInputDirection();
        return direction == steamInput || direction == steamInput.getOpposite();
    }

    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        receiveSteam(direction, steam);
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return canConnect(direction) ? inputSteam.getThroughput() : 0;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Steam Ejector").withStyle(ChatFormatting.GOLD));
        if (!inputSteam.isEmpty()) {
            tooltip.add(Component.literal("    Steam: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f @ %.1f/t", inputSteam.getPressure(), inputSteam.getThroughput()))
                    .withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(Component.literal("    Fluid range: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(Integer.toString(getFluidRange())).withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(Component.literal("    Fluid rate: 1.0 B/s").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal("    Idle").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (isPlayerSneaking) {
            addFluidFlowTooltip(tooltip, "FACING/inlet", getFacing());
            addFluidFlowTooltip(tooltip, "Opposite/outlet", getFacing().getOpposite());
        }
        return true;
    }

    private void addFluidFlowTooltip(List<Component> tooltip, String label, Direction side) {
        FluidTransportBehaviour behaviour = getBehaviour(FluidTransportBehaviour.TYPE);
        PipeConnection.Flow flow = behaviour == null ? null : behaviour.getFlow(side);
        if (flow == null) {
            tooltip.add(Component.literal("    " + label + ": idle").withStyle(ChatFormatting.GRAY));
            return;
        }
        String direction = flow.inbound ? "pulling" : "pushing";
        String progress = flow.complete ? "ready" : "in transit";
        tooltip.add(Component.literal("    " + label + ": " + direction + " " + flow.fluid.getAmount()
            + " mB (" + progress + ")").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("InputSteam"))
            inputSteam = SteamData.loadFromNBT(tag.getCompound("InputSteam"), registries);
        pressureDirty = true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag inputTag = new CompoundTag();
        inputSteam.saveToNBT(inputTag, registries);
        tag.put("InputSteam", inputTag);
    }

    private class EjectorFluidTransferBehaviour extends FluidTransportBehaviour {
        public EjectorFluidTransferBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            Direction facing = getFacing();
            return direction == facing || direction == facing.getOpposite();
        }

        @Override
        public void tick() {
            super.tick();
            if (interfaces == null)
                return;

            float pressureValue = inputSteam.isEmpty() ? 0 : getFluidPressure();
            for (var entry : interfaces.entrySet()) {
                boolean pull = isFront(entry.getKey());
                Couple<Float> pressure = entry.getValue().getPressure();
                pressure.set(pull, pressureValue);
                pressure.set(!pull, 0f);
            }
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state,
                                                         Direction direction) {
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            return attachment == AttachmentTypes.RIM ? AttachmentTypes.NONE : attachment;
        }
    }
}
