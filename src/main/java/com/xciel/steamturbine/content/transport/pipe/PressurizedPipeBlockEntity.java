package com.xciel.steamturbine.content.transport.pipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;

public class PressurizedPipeBlockEntity extends SmartBlockEntity implements ISteamTransport {
    private static final float LERP_FACTOR = 0.2f;
    private static final float DECAY_FACTOR = 0.98f;

    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualPressure = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamType> visualSteamType = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualQuality = new EnumMap<>(Direction.class);

    private int lazyTickCounter = 10;

    public PressurizedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        receivedSteam.put(from, steam);
        setChanged();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) {
            clientVisualUpdate();
        } else {
            serverPropagation();
            updateConnectionStates();
        }
        lazyTickCounter = 10;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            updateConnectionStates();
        }
    }

    private void updateConnectionStates() {
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
        return neighborBlock instanceof PressurizedPipeBlock;
    }

    private void serverPropagation() {
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && steam.shouldPropagate()) {
                propagateToNeighbor(dir, steam);
            }
            receivedSteam.put(dir, SteamData.empty());
        }
    }

    private void propagateToNeighbor(Direction dir, SteamData steam) {
        BlockPos neighborPos = worldPosition.relative(dir);
        if (!level.isLoaded(neighborPos)) return;

        SteamData propagated = steam.withPropagationLoss();
        if (!propagated.shouldPropagate()) return;

        var neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof PressurizedPipeBlockEntity pipe) {
            pipe.receiveSteam(dir.getOpposite(), propagated);
        } else if (neighbor instanceof ISteamConsumer consumer) {
            if (consumer.canReceive(dir.getOpposite())) {
                consumer.receiveSteam(dir.getOpposite(), propagated);
            }
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
                SteamType type = SteamType.REGULAR;
                try { type = SteamType.valueOf(typeStr); } catch (Exception ignored) {}
                SteamData steam = SteamData.of(p, type, q);
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
            tag.putFloat(prefix + "P", visualPressure.get(dir));
            tag.putString(prefix + "T", visualSteamType.get(dir).name());
            tag.putFloat(prefix + "Q", visualQuality.get(dir));
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
        SteamData steam = receivedSteam.get(direction);
        if (steam == null || steam.isEmpty()) {
            return SteamData.empty();
        }
        float extracted = Math.min(steam.getPressure(), amount);
        return SteamData.of(extracted, steam.getSteamType(), steam.getQuality());
    }

    @Override
    public float getFlowRate(Direction direction) {
        return visualPressure.getOrDefault(direction, 0f);
    }

    public float getOutboundPressure(Direction direction) {
        return visualPressure.getOrDefault(direction, 0f);
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