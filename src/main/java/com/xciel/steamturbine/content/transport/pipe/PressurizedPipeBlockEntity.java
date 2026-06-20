package com.xciel.steamturbine.content.transport.pipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.IPressurizedConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamProducer;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PressurizedPipeBlockEntity extends SmartBlockEntity implements IPressurizedConsumer, ISteamTransport, ITurbineEndpoint {
    private static final float LERP_FACTOR = 0.2f;
    private static final float DECAY_FACTOR = 0.98f;
    private static final float MAX_THROUGHPUT = 10.0f;
    private static final float MAX_STORAGE = 100f;

    private float storage = 0f;
    private SteamType storedSteamType = SteamType.REGULAR;
    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamData> outgoingSteam = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualPressure = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamType> visualSteamType = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualQuality = new EnumMap<>(Direction.class);

    public PressurizedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        storage = 0f;
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
            outgoingSteam.put(dir, SteamData.empty());
            visualPressure.put(dir, 0f);
            visualSteamType.put(dir, SteamType.REGULAR);
            visualQuality.put(dir, 1f);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void receiveSteam(Direction from, SteamData steam) {
        if (steam.isEmpty()) return;
        storage = Math.min(storage + steam.getThroughput(), MAX_STORAGE);
        storedSteamType = steam.getSteamType();
        receivedSteam.put(from, steam);
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            clientVisualUpdate();
        } else {
            updateConnectionStates();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            updateConnectionStates();
        }
    }

    void updateConnectionStates() {
        if (level == null) return;

        BlockState state = getBlockState();
        boolean changed = false;

        for (Direction dir : Direction.values()) {
            boolean wasConnected = PressurizedPipeBlock.getConnection(state, dir);
            boolean isConnected = hasNeighborPipe(dir);

            if (isConnected != wasConnected) {
                state = PressurizedPipeBlock.setConnection(state, dir, isConnected);
                changed = true;
            }

            // Clear steam when source connection is lost
            if (wasConnected && !isConnected) {
                receivedSteam.put(dir, SteamData.empty());
                outgoingSteam.put(dir, SteamData.empty());
            }
        }

        if (changed) {
            level.setBlock(getBlockPos(), state, Block.UPDATE_ALL);
        }
    }

    private boolean hasNeighborPipe(Direction dir) {
        BlockPos neighborPos = getBlockPos().relative(dir);
        if (!level.isLoaded(neighborPos)) return false;

        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (neighborBlock instanceof PressurizedPipeBlock) return true;

        var neighborBE = level.getBlockEntity(neighborPos);
        Direction opposite = dir.getOpposite();

        if (neighborBE instanceof ISteamEndpoint endpoint) {
            if (endpoint.canConnect(opposite)) return true;
        }

        if (neighborBE instanceof ITurbineEndpoint turbineEndpoint) {
            if (turbineEndpoint.canTurbineConnect(opposite)) return true;
        }

        return false;
    }

    private void clientVisualUpdate() {
        for (Direction dir : Direction.values()) {
            float currentPressure = visualPressure.get(dir);
            SteamData received = receivedSteam.get(dir);
            SteamData outgoing = outgoingSteam.get(dir);
            float targetPressure = 0f;
            float targetQuality = visualQuality.get(dir);
            SteamType targetType = SteamType.REGULAR;

            if (received != null && !received.isEmpty()) {
                targetPressure = Math.max(targetPressure, received.getThroughput());
                targetQuality = received.getQuality();
                targetType = received.getSteamType();
            }

            if (outgoing != null && !outgoing.isEmpty()) {
                targetPressure = Math.max(targetPressure, outgoing.getThroughput());
                if (outgoing.getQuality() < targetQuality) {
                    targetQuality = outgoing.getQuality();
                }
                if (outgoing.getSteamType() != SteamType.REGULAR) {
                    targetType = outgoing.getSteamType();
                }
            }

            float newPressure = currentPressure + (targetPressure - currentPressure) * LERP_FACTOR;
            newPressure *= DECAY_FACTOR;
            if (newPressure < 0.01f) newPressure = 0f;

            float newQuality = targetQuality * 0.998f;
            if (newQuality < 0.01f) newQuality = 0f;

            visualPressure.put(dir, newPressure);
            visualQuality.put(dir, newQuality);
            visualSteamType.put(dir, targetType);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        storage = tag.getFloat("Storage");
        if (tag.contains("StoredSteamType")) {
            try {
                storedSteamType = SteamType.valueOf(tag.getString("StoredSteamType"));
            } catch (IllegalArgumentException ignored) {
                storedSteamType = SteamType.REGULAR;
            }
        }
        for (Direction dir : Direction.values()) {
            String prefix = "S_" + dir.getName() + "_";
            if (tag.contains(prefix + "P")) {
                float p = tag.getFloat(prefix + "P");
                String typeStr = tag.contains(prefix + "T") ? tag.getString(prefix + "T") : "REGULAR";
                float q = tag.contains(prefix + "Q") ? tag.getFloat(prefix + "Q") : 1f;
                float tp = tag.contains(prefix + "TP") ? tag.getFloat(prefix + "TP") : 0f;
                SteamType type = SteamType.REGULAR;
                try { type = SteamType.valueOf(typeStr); } catch (Exception ignored) {}
                SteamData steam = SteamData.of(p, type, q, 1f, tp);
                visualPressure.put(dir, p);
                visualSteamType.put(dir, type);
                visualQuality.put(dir, q);
                receivedSteam.put(dir, steam);
            }
        }
    }

    public void recordOutgoingSteam(Direction dir, float amount) {
        if (amount <= 0) return;
        SteamData current = outgoingSteam.get(dir);
        float newPressure = Math.min(current.getPressure() + amount, MAX_THROUGHPUT);
        outgoingSteam.put(dir, SteamData.of(newPressure, storedSteamType, 1f, 1f, amount));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Storage", storage);
        tag.putString("StoredSteamType", storedSteamType.name());
        for (Direction dir : Direction.values()) {
            String prefix = "S_" + dir.getName() + "_";
            SteamData steam = receivedSteam.get(dir);
            if (steam != null) {
                tag.putFloat(prefix + "P", steam.getPressure());
                tag.putString(prefix + "T", steam.getSteamType().name());
                tag.putFloat(prefix + "Q", steam.getQuality());
                tag.putFloat(prefix + "TP", Math.min(steam.getThroughput(), MAX_THROUGHPUT));
            }
        }
    }

    public float getVisualPressure(Direction dir) {
        return visualPressure.getOrDefault(dir, 0f);
    }

    public SteamType getVisualSteamType(Direction dir) {
        return visualSteamType.getOrDefault(dir, SteamType.REGULAR);
    }

    public float getVisualQuality(Direction dir) {
        return visualQuality.getOrDefault(dir, 1f);
    }

    public SteamType getStoredSteamType() {
        return storedSteamType;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return true;
    }

    @Override
    public boolean canReceive(Direction direction) {
        return canConnect(direction);
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return MAX_THROUGHPUT;
    }

    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        receiveSteam(direction, steam);
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        if (storage <= 0) return SteamData.empty();
        float maxExtract = Math.min(amount, MAX_THROUGHPUT);
        float extracted = Math.min(storage, maxExtract);
        storage -= extracted;
        return SteamData.of(extracted, storedSteamType, 1f, 1f, extracted);
    }

    private static final int MAX_BFS_NODES = 64;
    private static final float MAX_BFS_AMOUNT_PER_NODE = 10.0f;

    public SteamData pullSteamFromNetwork(Direction requestedFrom, float amount) {
        if (level == null || level.isClientSide) return SteamData.empty();
        if (storage <= 0 && amount <= 0) return SteamData.empty();

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new LinkedList<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        float totalExtracted = 0f;
        float remaining = amount;
        SteamType extractedType = storedSteamType;
        int nodesVisited = 0;

        while (!queue.isEmpty() && remaining > 0.01f && nodesVisited < MAX_BFS_NODES) {
            BlockPos current = queue.pollFirst();
            nodesVisited++;
            boolean isOrigin = current.equals(worldPosition);

            var be = level.getBlockEntity(current);
            if (!(be instanceof PressurizedPipeBlockEntity pipe)) continue;

            float available = pipe.storage;
            if (available > 0) {
                float toExtract = Math.min(remaining, Math.min(available, MAX_BFS_AMOUNT_PER_NODE));
                if (toExtract > 0.01f) {
                    pipe.storage -= toExtract;
                    pipe.setChanged();
                    if (!isOrigin) {
                        pipe.sendData();
                    }
                    if (isOrigin) {
                        pipe.recordOutgoingSteam(requestedFrom, toExtract);
                    }
                    totalExtracted += toExtract;
                    remaining -= toExtract;
                    extractedType = pipe.storedSteamType;
                }
            }

            if (remaining <= 0.01f) break;

            if (level != null) {
                for (Direction dir : Direction.values()) {
                    if (dir == requestedFrom && current.equals(worldPosition)) continue;

                    BlockPos neighborPos = current.relative(dir);
                    if (visited.contains(neighborPos)) continue;
                    if (!level.isLoaded(neighborPos)) continue;

                    visited.add(neighborPos);
                    var neighbor = level.getBlockEntity(neighborPos);

                    if (neighbor instanceof PressurizedPipeBlockEntity) {
                        queue.add(neighborPos);
                    }
                }
            }
        }

        if (totalExtracted > 0) {
            return SteamData.of(totalExtracted, extractedType, 1f, 1f, totalExtracted);
        }
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return Math.min(storage, MAX_THROUGHPUT);
    }

    public float getStorage() {
        return storage;
    }

    public float getOutboundPressure(Direction direction) {
        return visualPressure.getOrDefault(direction, 0f);
    }

    public float getActualThroughput() {
        return storage;
    }

    public float getTotalPressure() {
        float total = 0f;
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null) {
                total += steam.getPressure();
            }
        }
        return total;
    }

    public float getMaxThroughput() {
        float max = 0f;
        for (Direction dir : Direction.values()) {
            SteamData received = receivedSteam.get(dir);
            SteamData outgoing = outgoingSteam.get(dir);
            if (received != null && received.getThroughput() > max) {
                max = received.getThroughput();
            }
            if (outgoing != null && outgoing.getThroughput() > max) {
                max = outgoing.getThroughput();
            }
        }
        return max;
    }

    public float getMaxPressure() {
        float max = 0f;
        for (Direction dir : Direction.values()) {
            SteamData received = receivedSteam.get(dir);
            SteamData outgoing = outgoingSteam.get(dir);
            if (received != null && received.getPressure() > max) {
                max = received.getPressure();
            }
            if (outgoing != null && outgoing.getPressure() > max) {
                max = outgoing.getPressure();
            }
        }
        return max;
    }

    public int getActiveDirectionCount() {
        int count = 0;
        for (Direction dir : Direction.values()) {
            SteamData received = receivedSteam.get(dir);
            SteamData outgoing = outgoingSteam.get(dir);
            boolean hasReceived = received != null && received.getPressure() > 0.001f;
            boolean hasOutgoing = outgoing != null && outgoing.getPressure() > 0.001f;
            if (hasReceived || hasOutgoing) {
                count++;
            }
        }
        return count;
    }

    // IHaveGoggleInformation
    // empty lmao, maybe implemented in the future

    // ITurbineEndpoint
    @Override
    public boolean canTurbineConnect(Direction direction) {
        // Pipes can accept turbine exhaust from any direction they have a connection
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PressurizedPipeBlock) {
            return PressurizedPipeBlock.getConnection(state, direction);
        }
        return false;
    }

    @Override
    public SteamData produceTurbineSteam(Direction from) {
        return SteamData.empty();
    }

    public void clearState() {
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
            visualPressure.put(dir, 0f);
            visualSteamType.put(dir, SteamType.REGULAR);
            visualQuality.put(dir, 1f);
        }
    }
}
