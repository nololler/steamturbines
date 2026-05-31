package com.xciel.steamturbine.content.transport.pipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
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

public class PressurizedPipeBlockEntity extends SmartBlockEntity implements ISteamTransport, IHaveGoggleInformation {
    private static final float LERP_FACTOR = 0.2f;
    private static final float DECAY_FACTOR = 0.98f;
    private static final float MAX_THROUGHPUT = 1024.0f;

    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualPressure = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamType> visualSteamType = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualQuality = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamData> runtimeBuffer = new EnumMap<>(Direction.class);

    public PressurizedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
            visualPressure.put(dir, 0f);
            visualSteamType.put(dir, SteamType.REGULAR);
            visualQuality.put(dir, 1f);
            runtimeBuffer.put(dir, SteamData.empty());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void receiveSteam(Direction from, SteamData steam) {
        if (steam.isEmpty()) return;
        SteamData existing = receivedSteam.get(from);
        float newThroughput = steam.getThroughput();
        if (existing != null && !existing.isEmpty()) {
            newThroughput = Math.min(existing.getThroughput() + steam.getThroughput(), MAX_THROUGHPUT);
            receivedSteam.put(from, existing.withThroughput(newThroughput));
        } else {
            receivedSteam.put(from, steam.withThroughput(Math.min(newThroughput, MAX_THROUGHPUT)));
        }
        SteamData buffered = runtimeBuffer.get(from);
        if (buffered != null && !buffered.isEmpty()) {
            float bufferedThroughput = Math.min(buffered.getThroughput() + steam.getThroughput(), MAX_THROUGHPUT);
            runtimeBuffer.put(from, buffered.withThroughput(bufferedThroughput));
        } else {
            runtimeBuffer.put(from, steam.withThroughput(Math.min(newThroughput, MAX_THROUGHPUT)));
        }
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
            boolean isConnected = hasNeighborPipe(dir);
            boolean wasConnected = PressurizedPipeBlock.getConnection(state, dir);

            if (isConnected != wasConnected) {
                state = PressurizedPipeBlock.setConnection(state, dir, isConnected);
                changed = true;
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

        // Block compressor-to-compressor: don't connect to compressor's output direction
        if (neighborBE instanceof ICompressorEndpoint compressor) {
            if (compressor.getCompressorOutputDirection() == opposite) {
                return false;  // Don't connect to compressor's output (prevents compressor chain)
            }
        }

        if (neighborBE instanceof ISteamEndpoint endpoint) {
            if (endpoint.canConnect(opposite)) return true;
        }

        if (neighborBE instanceof ITurbineEndpoint turbineEndpoint) {
            if (turbineEndpoint.canTurbineConnect(opposite)) return true;
        }

        return false;
    }

    private void serverPropagation() {
        BlockState state = getBlockState();
        for (Direction inDir : Direction.values()) {
            SteamData steam = receivedSteam.get(inDir);
            if (steam == null || !steam.shouldPropagate()) {
                receivedSteam.put(inDir, SteamData.empty());
            } else {
                for (Direction outDir : Direction.values()) {
                    if (outDir == inDir) continue;
                    if (!PressurizedPipeBlock.getConnection(state, outDir)) continue;
                    propagateToNeighbor(outDir, steam);
                }
                receivedSteam.put(inDir, SteamData.empty());
            }

            SteamData buffered = runtimeBuffer.get(inDir);
            if (buffered != null && buffered.shouldPropagate()) {
                for (Direction outDir : Direction.values()) {
                    if (outDir == inDir) continue;
                    if (!PressurizedPipeBlock.getConnection(state, outDir)) continue;
                    propagateToNeighbor(outDir, buffered);
                }
            }
            runtimeBuffer.put(inDir, SteamData.empty());
        }
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
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            float targetPressure = steam.getPressure();
            SteamType targetType = steam.getSteamType();
            float targetQuality = steam.getQuality();

            float currentPressure = visualPressure.get(dir);

            float newPressure = currentPressure + (targetPressure - currentPressure) * LERP_FACTOR;
            newPressure *= DECAY_FACTOR;
            if (newPressure < 0.01f) newPressure = 0f;

            float currentQuality = visualQuality.get(dir);
            float newQuality = currentQuality + (targetQuality - currentQuality) * LERP_FACTOR;
            newQuality *= 0.998f;
            if (newQuality < 0.01f) newQuality = 0f;

            visualPressure.put(dir, newPressure);
            visualSteamType.put(dir, targetType);
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
        SteamData steam = runtimeBuffer.get(direction);
        if (steam == null || steam.isEmpty()) {
            steam = receivedSteam.get(direction);
            if (steam == null || steam.isEmpty()) {
                return SteamData.empty();
            }
        }
        float extracted = Math.min(steam.getPressure(), amount);
        float remaining = steam.getPressure() - extracted;
        float extractedRatio = steam.getPressure() > 0 ? extracted / steam.getPressure() : 0f;
        float extractedThroughput = steam.getThroughput() * extractedRatio;
        float remainingThroughput = steam.getThroughput() * (remaining / steam.getPressure());
        if (remaining <= SteamConstants.PROPAGATION_THRESHOLD) {
            runtimeBuffer.put(direction, SteamData.empty());
            receivedSteam.put(direction, SteamData.empty());
        } else {
            runtimeBuffer.put(direction, steam.withPressure(remaining).withThroughput(remainingThroughput));
            receivedSteam.put(direction, steam.withPressure(remaining).withThroughput(remainingThroughput));
        }
        return SteamData.of(extracted, steam.getSteamType(), steam.getQuality(), 1f, extractedThroughput);
    }

    @Override
    public float getFlowRate(Direction direction) {
        SteamData steam = receivedSteam.get(direction);
        if (steam == null) return 0f;
        return steam.getThroughput();
    }

    public float getOutboundPressure(Direction direction) {
        return visualPressure.getOrDefault(direction, 0f);
    }

    public float getActualThroughput() {
        float total = 0f;
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null) {
                total += steam.getThroughput();
            }
        }
        return total;
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

    public void clearState() {
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
            visualPressure.put(dir, 0f);
            visualSteamType.put(dir, SteamType.REGULAR);
            visualQuality.put(dir, 1f);
        }
    }
}
