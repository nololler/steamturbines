package com.xciel.steamturbine.content.compressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import com.xciel.steamturbine.content.ejector.SteamEjectorBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ICompressorEndpoint;
import com.xciel.steamturbine.steam.transfer.IPressurizedConsumer;
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
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SteamCompressorBlockEntity extends KineticBlockEntity implements ISteamEndpoint, ISteamProducer, ISteamTransport, ITurbineEndpoint, ICompressorEndpoint, IHaveGoggleInformation {
    private static final float AMPLIFICATION_FACTOR = 2.0f;
    private static final float MAX_THROUGHPUT = 30.0f;
    private static final float MAX_STEAM_BUFFER = 250f;

    private final EnumMap<Direction, Boolean> steamConnections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Boolean> turbineConnections = new EnumMap<>(Direction.class);
    private SteamData inputSteam = SteamData.empty();
    private SteamData outputSteam = SteamData.empty();
    private float steamBuffer = 0f;
    private SteamType bufferedSteamType = SteamType.REGULAR;
    private float currentRPM = 0f;
    private SteamData lastReceivedSteam = SteamData.empty();
    private SteamData lastProducedSteam = SteamData.empty();

    public SteamCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
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
        if (level == null) return;
        if (!level.isClientSide) {
            updateConnectionStates();
            processSteam();
            pushSteam();
        }
    }

    private void processSteam() {
        currentRPM = Math.abs(getSpeed());

        boolean usedBuffer = false;
        if (inputSteam.isEmpty() || !inputSteam.shouldPropagate()) {
            if (steamBuffer > 0 && bufferedSteamType != null && currentRPM > 0) {
                float bufferPressure = steamBuffer / MAX_STEAM_BUFFER * 2f;
                inputSteam = SteamData.of(bufferPressure, bufferedSteamType, 1f, 1f, steamBuffer);
                usedBuffer = true;
            }
        }

        if (inputSteam.isEmpty() || !inputSteam.shouldPropagate()) {
            inputSteam = SteamData.empty();
            lastReceivedSteam = SteamData.empty();
            lastProducedSteam = SteamData.empty();
            outputSteam = SteamData.empty();
            if (usedBuffer) {
                steamBuffer = 0f;
            }
            setChanged();
            sendData();
            return;
        }

        lastReceivedSteam = inputSteam.withThroughput(Math.min(inputSteam.getThroughput(), MAX_THROUGHPUT));

        float pulledPressure = inputSteam.getPressure();
        float inputThroughput = Math.min(inputSteam.getThroughput(), MAX_THROUGHPUT);

        float rpmFactor = currentRPM / 64f;
        float amplification = 1f + rpmFactor * AMPLIFICATION_FACTOR;
        float amplified = pulledPressure * amplification;
        float amplifiedThroughput = Math.min(inputThroughput * amplification, MAX_THROUGHPUT);
        outputSteam = SteamData.of(amplified, SteamType.PRESSURIZED, 1f, 1f, amplifiedThroughput);
        lastProducedSteam = outputSteam;

        float remainingThroughput = inputThroughput - amplifiedThroughput;
        if (remainingThroughput > 0) {
            inputSteam = inputSteam.withThroughput(remainingThroughput);
        } else {
            inputSteam = SteamData.empty();
        }

        if (usedBuffer) {
            steamBuffer = 0f;
        }

        setChanged();
        sendData();
    }

    private void pushSteam() {
        if (!outputSteam.shouldPropagate()) return;

        Direction outputDir = getTurbineOutputDirection();
        BlockPos neighborPos = worldPosition.relative(outputDir);
        if (!level.isLoaded(neighborPos)) return;

        var neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof ICompressorEndpoint) return;  // Don't chain compressors
        if (neighbor instanceof ISteamTransport transport) {
            if (transport.canConnect(outputDir.getOpposite())) {
                transport.pushSteam(outputDir.getOpposite(), outputSteam);
            }
        } else if (neighbor instanceof IPressurizedConsumer consumer) {
            if (consumer.canReceive(outputDir.getOpposite())) {
                consumer.receiveSteam(outputDir.getOpposite(), outputSteam);
            }
        }
    }

    public void updateConnectionStates() {
        if (level == null) return;

        Direction inputDir = getSteamInputDirection();
        BlockPos inputPos = worldPosition.relative(inputDir);
        boolean hadInputConnection = false;

        // Check if we had a valid steam connection before
        if (level.isLoaded(inputPos)) {
            BlockState neighborState = level.getBlockState(inputPos);
            Block neighborBlock = neighborState.getBlock();
            hadInputConnection = isValidSteamConnection(neighborBlock, inputDir);
        }

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

        // Clear steam if we lost our input connection
        boolean hasInputConnectionNow = steamConnections.getOrDefault(inputDir, false);
        if (hadInputConnection && !hasInputConnectionNow) {
            inputSteam = SteamData.empty();
            outputSteam = SteamData.empty();
        }

        setChanged();
    }

    private boolean isValidSteamConnection(Block block, Direction dir) {
        if (block instanceof PressurizedPipeBlock) return true;
        if (block instanceof SteamBoilerBlock) return true;
        if (block instanceof SteamEjectorBlock) return true;
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
        return true;  // Accept connections from any direction in tank model
    }

    // ISteamProducer
    @Override
    public SteamData produceSteam(Direction from) {
        return outputSteam.isEmpty() ? SteamData.empty() : outputSteam;
    }

    @Override
    public float getMaxProduceRate(Direction from) {
        return 100f;  // Allow production to any direction
    }

    @Override
    public boolean canProduce(Direction direction) {
        return true;  // Can produce to any direction in tank model
    }

    // ITurbineEndpoint
    @Override
    public boolean canTurbineConnect(Direction direction) {
        return true;  // Can connect to any direction
    }

    @Override
    public SteamData produceTurbineSteam(Direction from) {
        return SteamData.empty();  // Compressor is not a turbine
    }

    // ICompressorEndpoint
    @Override
    public boolean isCompressor() {
        return true;
    }

    @Override
    public Direction getCompressorOutputDirection() {
        return getTurbineOutputDirection();
    }

    // ISteamTransport
    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        if (inputSteam.isEmpty()) {
            inputSteam = steam;
        } else {
            inputSteam = inputSteam.withPressureAndThroughputAdded(steam.getPressure(), steam.getThroughput());
        }
        setChanged();
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return outputSteam.getThroughput();
    }

    // Goggles
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    Steam Compressor:  ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Input: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f @ %.2f/t", lastReceivedSteam.getPressure(), lastReceivedSteam.getThroughput())).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.literal("    Output: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f @ %.2f/t", lastProducedSteam.getPressure(), lastProducedSteam.getThroughput())).withStyle(
                lastProducedSteam.getPressure() > SteamConstants.MAX_PRESSURE * 0.5f ? ChatFormatting.RED : ChatFormatting.DARK_GRAY)));
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
        steamBuffer = tag.getFloat("SteamBuffer");
        if (tag.contains("BufferedSteamType")) {
            try {
                bufferedSteamType = SteamType.valueOf(tag.getString("BufferedSteamType"));
            } catch (IllegalArgumentException ignored) {
                bufferedSteamType = SteamType.REGULAR;
            }
        }
        if (tag.contains("OutputSteam")) {
            outputSteam = SteamData.loadFromNBT(tag.getCompound("OutputSteam"), registries);
        }
        if (clientPacket) {
            if (tag.contains("LastReceivedSteam")) {
                lastReceivedSteam = SteamData.loadFromNBT(tag.getCompound("LastReceivedSteam"), registries);
            }
            if (tag.contains("LastProducedSteam")) {
                lastProducedSteam = SteamData.loadFromNBT(tag.getCompound("LastProducedSteam"), registries);
            }
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
        tag.putFloat("SteamBuffer", steamBuffer);
        tag.putString("BufferedSteamType", bufferedSteamType.name());
        CompoundTag outputTag = new CompoundTag();
        outputSteam.saveToNBT(outputTag, registries);
        tag.put("OutputSteam", outputTag);
        if (clientPacket) {
            CompoundTag lastReceivedTag = new CompoundTag();
            lastReceivedSteam.saveToNBT(lastReceivedTag, registries);
            tag.put("LastReceivedSteam", lastReceivedTag);
            CompoundTag lastProducedTag = new CompoundTag();
            lastProducedSteam.saveToNBT(lastProducedTag, registries);
            tag.put("LastProducedSteam", lastProducedTag);
        }
        tag.putFloat("CurrentRPM", currentRPM);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("steam_" + dir.getName(), steamConnections.getOrDefault(dir, false));
            tag.putBoolean("turb_" + dir.getName(), turbineConnections.getOrDefault(dir, false));
        }
    }
}
