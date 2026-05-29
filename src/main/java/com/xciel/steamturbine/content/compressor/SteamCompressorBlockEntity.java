package com.xciel.steamturbine.content.compressor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
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

public class SteamCompressorBlockEntity extends KineticBlockEntity implements ISteamEndpoint, ISteamConsumer, ISteamTransport, ITurbineEndpoint {
    private final EnumMap<Direction, Boolean> steamConnections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Boolean> turbineConnections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private SteamData outputSteam = SteamData.empty();

    public SteamCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(5);
        for (Direction dir : Direction.values()) {
            steamConnections.put(dir, false);
            turbineConnections.put(dir, false);
            receivedSteam.put(dir, SteamData.empty());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide) {
            serverTick();
        }
    }

    private void serverTick() {
        updateConnectionStates();
    }

    public void updateConnectionStates() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            boolean steamConn = isValidSteamConnection(neighborBlock, dir);
            steamConnections.put(dir, steamConn);

            boolean turbineConn = isValidTurbineConnection(neighborBlock, dir);
            turbineConnections.put(dir, turbineConn);
        }
        setChanged();
    }

    private boolean isValidSteamConnection(Block block, Direction dir) {
        if (block instanceof PressurizedPipeBlock) return true;
        if (block instanceof SteamBoilerBlock) return true;
        return false;
    }

    private boolean isValidTurbineConnection(Block block, Direction dir) {
        return block instanceof SteamTurbineBlock;
    }

    public Direction getSteamInputDirection() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(SteamCompressorBlock.FACING);
        return facing.getOpposite();
    }

    public Direction getTurbineOutputDirection() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(SteamCompressorBlock.FACING);
        return facing;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return direction == getSteamInputDirection();
    }

    @Override
    public boolean canTurbineConnect(Direction direction) {
        return direction == getTurbineOutputDirection();
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam.isEmpty()) return;
        receivedSteam.put(direction, steam);
        setChanged();
    }

    @Override
    public boolean canReceive(Direction direction) {
        return direction == getSteamInputDirection();
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return 100f;
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

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        outputSteam = SteamData.loadFromNBT(tag, registries);
        for (Direction dir : Direction.values()) {
            steamConnections.put(dir, tag.getBoolean("steam_" + dir.getName()));
            turbineConnections.put(dir, tag.getBoolean("turb_" + dir.getName()));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        outputSteam.saveToNBT(tag, registries);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("steam_" + dir.getName(), steamConnections.getOrDefault(dir, false));
            tag.putBoolean("turb_" + dir.getName(), turbineConnections.getOrDefault(dir, false));
        }
    }

    public Map<Direction, Boolean> getSteamConnections() {
        return steamConnections;
    }

    public Map<Direction, Boolean> getTurbineConnections() {
        return turbineConnections;
    }

    public SteamData getOutputSteam() {
        return outputSteam;
    }
}