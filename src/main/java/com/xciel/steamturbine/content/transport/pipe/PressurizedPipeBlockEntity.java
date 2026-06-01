package com.xciel.steamturbine.content.transport.pipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.transfer.ICompressorEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;

public class PressurizedPipeBlockEntity extends SmartBlockEntity implements ISteamTransport, ITurbineEndpoint, IHaveGoggleInformation {
    private static final float LERP_FACTOR = 0.2f;
    private static final float DECAY_FACTOR = 0.98f;
    private static final float MAX_THROUGHPUT = 1.0f;  // Pipes act as bottlenecks - low throughput limit
    private static final float MAX_STORAGE = 100f;     // Each pipe has its own storage of steam

    private float storage = 0f;  // Pipe's internal steam reserve
    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);  // For visual only
    private final EnumMap<Direction, Float> visualPressure = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamType> visualSteamType = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualQuality = new EnumMap<>(Direction.class);

    public PressurizedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        storage = 0f;
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
            visualPressure.put(dir, 0f);
            visualSteamType.put(dir, SteamType.REGULAR);
            visualQuality.put(dir, 1f);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void receiveSteam(Direction from, SteamData steam) {
        if (steam.isEmpty()) return;
        // Add to pipe's storage, capped at MAX_STORAGE
        storage = Math.min(storage + steam.getPressure(), MAX_STORAGE);
        // Store for visual feedback
        receivedSteam.put(from, steam);
        setChanged();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) {
            clientVisualUpdate();
        } else {
            updateConnectionStates();
            serverPropagation();
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

    private void serverPropagation() {
        if (storage <= 0) return;

        BlockState state = getBlockState();
        for (Direction outDir : Direction.values()) {
            if (!PressurizedPipeBlock.getConnection(state, outDir)) continue;

            Direction oppositeOut = outDir.getOpposite();
            BlockPos outNeighborPos = worldPosition.relative(oppositeOut);
            if (!level.isLoaded(outNeighborPos)) continue;

            var outNeighbor = level.getBlockEntity(outNeighborPos);
            if (!(outNeighbor instanceof SteamCompressorBlockEntity)) continue;

            // Destination is a compressor - check if source is turbine or compressor
            // Walk back from the pipe in oppositeOut direction to find sources
            if (walkBackFromTurbineSource(oppositeOut, 99)) continue;
            if (walkBackFromCompressorSource(oppositeOut, 99)) continue;

            // Propagate from storage to compressor
            float amount = Math.min(storage, MAX_THROUGHPUT);
            if (amount > 0.01f) {
                propagateToNeighbor(outDir, SteamData.of(amount, SteamType.REGULAR, 1f, 1f, MAX_THROUGHPUT));
                storage -= amount;
            }
        }
    }

    private boolean wouldCreateCompressorLoop(Direction inDir, Direction outDir, SteamData steam) {
        Direction oppositeOut = outDir.getOpposite();

        BlockPos outNeighborPos = worldPosition.relative(oppositeOut);
        if (!level.isLoaded(outNeighborPos)) return false;

        // Check if going TO a compressor's input
        boolean outToCompressorInput = false;
        var outNeighbor = level.getBlockEntity(outNeighborPos);
        if (outNeighbor instanceof SteamCompressorBlockEntity outComp) {
            outToCompressorInput = (outComp.getSteamInputDirection() == oppositeOut);
        }
        if (!outToCompressorInput) return false;

        // Walk back through pipes to find if source is a compressor
        return walkBackFromCompressorSource(inDir, 99);
    }

    private boolean walkBackFromCompressorSource(Direction fromDir, int depth) {
        if (depth <= 0) return false;
        BlockPos neighborPos = worldPosition.relative(fromDir);
        if (!level.isLoaded(neighborPos)) return false;

        var neighborBE = level.getBlockEntity(neighborPos);

        if (neighborBE instanceof ICompressorEndpoint inComp) {
            // This is a compressor - check if we're receiving from its output direction
            return inComp.getCompressorOutputDirection() == fromDir;
        }

        if (level.getBlockState(neighborPos).getBlock() instanceof PressurizedPipeBlock) {
            // Continue walking back
            return walkBackFromCompressorSource(fromDir, depth - 1);
        }

        return false;
    }

    private boolean wouldRouteTurbineToCompressor(Direction inDir, Direction outDir) {
        Direction oppositeOut = outDir.getOpposite();

        BlockPos outNeighborPos = worldPosition.relative(outDir);
        if (!level.isLoaded(outNeighborPos)) return false;

        // Check if it's going TO a compressor's input
        boolean outToCompressorInput = false;
        var outNeighborBE = level.getBlockEntity(outNeighborPos);
        if (outNeighborBE instanceof SteamCompressorBlockEntity outComp) {
            outToCompressorInput = (outComp.getSteamInputDirection() == oppositeOut);
        }
        if (!outToCompressorInput) return false;

        // Walk back through pipes to find if source is a turbine
        return walkBackFromTurbineSource(inDir, 99);
    }

    private boolean walkBackFromTurbineSource(Direction fromDir, int depth) {
        if (depth <= 0) return false;
        BlockPos neighborPos = worldPosition.relative(fromDir);
        if (!level.isLoaded(neighborPos)) return false;

        var neighborBlock = level.getBlockState(neighborPos).getBlock();
        var neighborBE = level.getBlockEntity(neighborPos);

        if (neighborBlock instanceof SteamTurbineBlock) {
            if (neighborBE instanceof SteamTurbineBlockEntity turbine) {
                Direction facing = turbine.getBlockState().getValue(SteamTurbineBlock.FACING);
                // Turbine pushes in its facing direction, steam comes from opposite
                return facing.getOpposite() == fromDir;
            }
            return false;
        }

        if (neighborBlock instanceof PressurizedPipeBlock) {
            // Continue walking back
            return walkBackFromTurbineSource(fromDir, depth - 1);
        }

        // Not a turbine or pipe (could be boiler, etc.)
        return false;
    }

    private void propagateToNeighbor(Direction dir, SteamData steam) {
        BlockPos neighborPos = worldPosition.relative(dir);
        if (!level.isLoaded(neighborPos)) return;

        if (!steam.shouldPropagate()) return;

        var neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof PressurizedPipeBlockEntity pipe) {
            pipe.receiveSteam(dir.getOpposite(), steam);
        } else if (neighbor instanceof ISteamTransport transport) {
            transport.pushSteam(dir.getOpposite(), steam);
        } else if (neighbor instanceof ISteamConsumer consumer) {
            consumer.receiveSteam(dir.getOpposite(), steam);
        }
    }

    private void clientVisualUpdate() {
        // Visual shows storage level (pressure)
        for (Direction dir : Direction.values()) {
            float currentPressure = visualPressure.get(dir);
            float targetPressure = storage;  // Use storage as pressure indicator

            float newPressure = currentPressure + (targetPressure - currentPressure) * LERP_FACTOR;
            newPressure *= DECAY_FACTOR;
            if (newPressure < 0.01f) newPressure = 0f;

            float currentQuality = visualQuality.get(dir);
            float newQuality = currentQuality * 0.998f;
            if (newQuality < 0.01f) newQuality = 0f;

            visualPressure.put(dir, newPressure);
            visualQuality.put(dir, newQuality);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
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

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
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

    @Override
    public boolean canConnect(Direction direction) {
        return true;
    }

    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        receiveSteam(direction, steam);
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        if (storage <= 0) return SteamData.empty();
        float extracted = Math.min(storage, amount);
        storage -= extracted;
        return SteamData.of(extracted, SteamType.REGULAR, 1f, 1f, extracted);
    }

    @Override
    public float getFlowRate(Direction direction) {
        return storage;
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
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && steam.getThroughput() > max) {
                max = steam.getThroughput();
            }
        }
        return max;
    }

    public float getMaxPressure() {
        float max = 0f;
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && steam.getPressure() > max) {
                max = steam.getPressure();
            }
        }
        return max;
    }

    public int getActiveDirectionCount() {
        int count = 0;
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && steam.getPressure() > 0.001f) {
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
