package com.xciel.steamturbine.content.compressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamProducer;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SteamCompressorBlockEntity extends KineticBlockEntity implements ISteamEndpoint, ISteamProducer, ISteamTransport, ITurbineEndpoint, IHaveGoggleInformation {
    private static final float AMPLIFICATION_FACTOR = 0.5f;

    private final EnumMap<Direction, Boolean> steamConnections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Boolean> turbineConnections = new EnumMap<>(Direction.class);
    private SteamData inputSteam = SteamData.empty();
    private SteamData outputSteam = SteamData.empty();
    private float currentRPM = 0f;

    public SteamCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(5);
        for (Direction dir : Direction.values()) {
            steamConnections.put(dir, false);
            turbineConnections.put(dir, false);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
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
        processSteam();
        pushSteam();
    }

    private void processSteam() {
        currentRPM = Math.abs(getSpeed());
        if (inputSteam.isEmpty() || !inputSteam.shouldPropagate()) {
            outputSteam = SteamData.empty();
            inputSteam = SteamData.empty();
            setChanged();
            sendData();
            return;
        }

        if (currentRPM < 1f) {
            outputSteam = SteamData.empty();
            setChanged();
            sendData();
            return;
        }

        float pulledPressure = inputSteam.getPressure();

        float rpmFactor = currentRPM / 64f;
        float amplified = pulledPressure * (1f + rpmFactor * AMPLIFICATION_FACTOR);
        outputSteam = SteamData.of(amplified, SteamType.PRESSURIZED, 1f);

        inputSteam = SteamData.empty();

        setChanged();
        sendData();
    }

    private void pushSteam() {
        if (!outputSteam.shouldPropagate()) return;

        Direction outputDir = getTurbineOutputDirection();
        BlockPos neighborPos = worldPosition.relative(outputDir);
        if (!level.isLoaded(neighborPos)) return;

        var neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof ISteamTransport transport) {
            if (transport.canConnect(outputDir.getOpposite())) {
                transport.pushSteam(outputDir.getOpposite(), outputSteam);
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
        return getBlockState().getValue(SteamCompressorBlock.FACING).getOpposite();
    }

    public Direction getTurbineOutputDirection() {
        return getBlockState().getValue(SteamCompressorBlock.FACING);
    }

    // ISteamEndpoint
    @Override
    public boolean canConnect(Direction direction) {
        return direction == getSteamInputDirection();
    }

    // ISteamProducer
    @Override
    public SteamData produceSteam(Direction from) {
        return outputSteam.isEmpty() ? SteamData.empty() : outputSteam;
    }

    @Override
    public float getMaxProduceRate(Direction from) {
        return from == getTurbineOutputDirection() ? 100f : 0f;
    }

    @Override
    public boolean canProduce(Direction direction) {
        return direction == getTurbineOutputDirection();
    }

    // ITurbineEndpoint
    @Override
    public boolean canTurbineConnect(Direction direction) {
        return direction == getTurbineOutputDirection();
    }

    // ISteamTransport
    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        inputSteam = steam;
        setChanged();
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return outputSteam.getPressure();
    }

    // Goggles
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("  Steam Compressor  ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  RPM: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", currentRPM)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Input Pressure: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", inputSteam.getPressure())).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Output Pressure: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", outputSteam.getPressure())).withStyle(
                outputSteam.getPressure() > SteamConstants.MAX_PRESSURE * 0.5f ? ChatFormatting.RED : ChatFormatting.WHITE)));
        return true;
    }

    // Getters
    public Map<Direction, Boolean> getSteamConnections() {
        return steamConnections;
    }

    public Map<Direction, Boolean> getTurbineConnections() {
        return turbineConnections;
    }

    public SteamData getOutputSteam() {
        return outputSteam;
    }

    public SteamData getInputSteam() {
        return inputSteam;
    }

    // NBT
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("InputSteam")) {
            inputSteam = SteamData.loadFromNBT(tag.getCompound("InputSteam"), registries);
        }
        if (tag.contains("OutputSteam")) {
            outputSteam = SteamData.loadFromNBT(tag.getCompound("OutputSteam"), registries);
        }
        currentRPM = tag.getFloat("CurrentRPM");
        for (Direction dir : Direction.values()) {
            steamConnections.put(dir, tag.getBoolean("steam_" + dir.getName()));
            turbineConnections.put(dir, tag.getBoolean("turb_" + dir.getName()));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag inputTag = new CompoundTag();
        inputSteam.saveToNBT(inputTag, registries);
        tag.put("InputSteam", inputTag);
        CompoundTag outputTag = new CompoundTag();
        outputSteam.saveToNBT(outputTag, registries);
        tag.put("OutputSteam", outputTag);
        tag.putFloat("CurrentRPM", currentRPM);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("steam_" + dir.getName(), steamConnections.getOrDefault(dir, false));
            tag.putBoolean("turb_" + dir.getName(), turbineConnections.getOrDefault(dir, false));
        }
    }
}
