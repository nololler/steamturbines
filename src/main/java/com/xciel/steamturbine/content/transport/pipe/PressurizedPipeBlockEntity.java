package com.xciel.steamturbine.content.transport.pipe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.xciel.steamturbine.network.PipeNetworkManager;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;

public class PressurizedPipeBlockEntity extends SmartBlockEntity implements ISteamTransport, ITurbineEndpoint, IHaveGoggleInformation {

    private static final float DECAY_FACTOR = 0.98f;
    private final EnumMap<Direction, Float> visualPressure = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamType> visualSteamType = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Float> visualQuality = new EnumMap<>(Direction.class);

    public PressurizedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction d : Direction.values()) {
            visualPressure.put(d, 0f);
            visualSteamType.put(d, SteamType.REGULAR);
            visualQuality.put(d, 1f);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> b) {}

    @Override
    public void pushSteam(Direction d, SteamData s) {
        if (!s.isEmpty() && level != null && !level.isClientSide) {
            PipeNetworkManager.pushSteam(level, worldPosition, d, s);
        }
    }

    @Override
    public boolean canConnect(Direction d) { return true; }

    @Override
    public SteamData pullSteam(Direction d, float a) { return SteamData.empty(); }

    @Override
    public float getFlowRate(Direction d) { return 0f; }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) {
            clientTick();
        } else {
            updateConnectionStates();
            PipeNetworkManager.tickAll(level);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) updateConnectionStates();
    }

    void updateConnectionStates() {
        BlockState s = getBlockState();
        boolean c = false;
        for (Direction d : Direction.values()) {
            boolean was = PressurizedPipeBlock.getConnection(s, d);
            boolean now = hasNeighbor(d);
            if (now != was) {
                s = PressurizedPipeBlock.setConnection(s, d, now);
                c = true;
            }
        }
        if (c) level.setBlock(getBlockPos(), s, Block.UPDATE_ALL);
    }

    private boolean hasNeighbor(Direction d) {
        BlockPos n = getBlockPos().relative(d);
        if (!level.isLoaded(n)) return false;
        if (level.getBlockState(n).getBlock() instanceof PressurizedPipeBlock) return true;
        var be = level.getBlockEntity(n);
        Direction op = d.getOpposite();
        if (be instanceof com.xciel.steamturbine.steam.transfer.ISteamEndpoint e && e.canConnect(op)) return true;
        if (be instanceof ITurbineEndpoint e && e.canTurbineConnect(op)) return true;
        return false;
    }

    private void clientTick() {
        for (Direction d : Direction.values()) {
            float p = visualPressure.get(d) * DECAY_FACTOR;
            visualPressure.put(d, p);
            visualQuality.put(d, visualQuality.get(d) * DECAY_FACTOR);
            if (p < 0.05f) {
                visualPressure.put(d, 0f);
                visualQuality.put(d, 1f);
            }
        }
    }

    @Override
    public boolean canTurbineConnect(Direction d) {
        return PressurizedPipeBlock.getConnection(getBlockState(), d);
    }

    @Override
    public SteamData produceTurbineSteam(Direction f) { return SteamData.empty(); }

    public float getVisualPressure(Direction d) { return visualPressure.getOrDefault(d, 0f); }

    public SteamType getVisualSteamType(Direction d) { return visualSteamType.getOrDefault(d, SteamType.REGULAR); }

    public float getVisualQuality(Direction d) { return visualQuality.getOrDefault(d, 1f); }

    @Override
    protected void read(CompoundTag t, HolderLookup.Provider r, boolean p) { super.read(t, r, p); }

    @Override
    protected void write(CompoundTag t, HolderLookup.Provider r, boolean p) { super.write(t, r, p); }
}