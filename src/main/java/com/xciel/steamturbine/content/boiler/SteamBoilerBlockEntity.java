package com.xciel.steamturbine.content.boiler;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
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
import java.util.Map;

public class SteamBoilerBlockEntity extends SmartBlockEntity implements ISteamEndpoint, ISteamConsumer, ISteamTransport {
    private final EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private SteamData outputSteam = SteamData.empty();

    public SteamBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction dir : Direction.values()) {
            connections.put(dir, false);
            receivedSteam.put(dir, SteamData.empty());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) {
            clientVisualUpdate();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        updateConnectionStates();

        Direction outputDirection = getOutputDirection();
        if (outputDirection != null && outputSteam.shouldPropagate()) {
            pushToOutput(outputDirection);
        }

        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
        }
    }

    private void pushToOutput(Direction outputDir) {
        BlockPos neighborPos = worldPosition.relative(outputDir);
        if (!level.isLoaded(neighborPos)) return;

        var neighbor = level.getBlockEntity(neighborPos);
        SteamData toSend = outputSteam.withPropagationLoss();

        if (!toSend.shouldPropagate()) return;

        if (neighbor instanceof ISteamTransport transport) {
            transport.pushSteam(outputDir.getOpposite(), toSend);
        } else if (neighbor instanceof ISteamConsumer consumer) {
            if (consumer.canReceive(outputDir.getOpposite())) {
                consumer.receiveSteam(outputDir.getOpposite(), toSend);
            }
        }
    }

    private void clientVisualUpdate() {
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && !steam.isEmpty()) {
            }
        }
    }

    public void updateConnectionStates() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            boolean connected = isValidConnection(neighborBlock, dir);
            connections.put(dir, connected);
        }
        setChanged();
    }

    private boolean isValidConnection(Block block, Direction dir) {
        if (block instanceof PressurizedPipeBlock) return true;
        if (block instanceof SteamBoilerBlock) return true;
        return false;
    }

    public Direction getOutputDirection() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(SteamBoilerBlock.FACING);
        return facing;
    }

    public boolean isOutputSide(Direction dir) {
        return dir == getOutputDirection();
    }

    public boolean isInputSide(Direction dir) {
        return !isOutputSide(dir);
    }

    public boolean hasConnection(Direction dir) {
        return connections.getOrDefault(dir, false);
    }

    public void generateSteam(float amount, SteamType type, float quality) {
        outputSteam = SteamData.of(amount, type, quality);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        outputSteam = SteamData.loadFromNBT(tag, registries);
        for (Direction dir : Direction.values()) {
            connections.put(dir, tag.getBoolean("conn_" + dir.getName()));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        outputSteam.saveToNBT(tag, registries);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("conn_" + dir.getName(), connections.getOrDefault(dir, false));
        }
    }

    public Map<Direction, Boolean> getConnections() {
        return connections;
    }

    public SteamData getOutputSteam() {
        return outputSteam;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return isOutputSide(direction);
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam.isEmpty()) return;
        receivedSteam.put(direction, steam);
        setChanged();
    }

@Override
    public boolean canReceive(Direction direction) {
        return false;
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return 0f;
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
        return outputSteam.getPressure();
    }
}