package com.xciel.steamturbine.content.turbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SteamTurbineBlockEntity extends SmartBlockEntity implements ISteamConsumer, ISteamEndpoint, ITurbineEndpoint, IHaveGoggleInformation {
    private static final float MAX_RPM = 256f;
    private static final float MIN_PRESSURE_FOR_OPERATION = 0.5f;
    private static final float MAX_THROUGHPUT = 2048.0f;
    private static final float[] STAGE_EFFICIENCY = {0.8f, 0.7f, 0.6f, 0.5f, 0.4f};

    private SteamData inputSteam = SteamData.empty();
    private float turbineSpeed = 0f;
    private int stageNumber = 0;
    private float stageEfficiency = 1.0f;
    private SteamData lastInputSteam = SteamData.empty();
    private SteamData lastExhaustSteam = SteamData.empty();

    public SteamTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;

        BlockState state = getBlockState();
        Direction facing = state.getValue(SteamTurbineBlock.FACING);

        stageNumber = countTurbinesBehind(worldPosition, facing);
        int efficiencyIndex = Math.min(stageNumber, STAGE_EFFICIENCY.length - 1);
        stageEfficiency = STAGE_EFFICIENCY[efficiencyIndex];

        if (inputSteam.isEmpty() || !inputSteam.shouldPropagate()) {
            inputSteam = SteamData.empty();
            lastInputSteam = SteamData.empty();
            lastExhaustSteam = SteamData.empty();
            turbineSpeed = 0f;
            setChanged();
            sendData();
            return;
        }

        lastInputSteam = inputSteam.withThroughput(Math.min(inputSteam.getThroughput(), MAX_THROUGHPUT));

        float inputPressure = inputSteam.getPressure();
        float inputThroughput = inputSteam.getThroughput();

        if (inputPressure >= MIN_PRESSURE_FOR_OPERATION) {
            float pressureFactor = Math.min(inputPressure / SteamConstants.MAX_PRESSURE, 1.0f);
            turbineSpeed = MAX_RPM * pressureFactor * stageEfficiency;
            float exhaustPressure = inputPressure * (1.0f - stageEfficiency * 0.5f);
            float exhaustThroughput = Math.min(inputThroughput * stageEfficiency, MAX_THROUGHPUT);
            lastExhaustSteam = SteamData.of(exhaustPressure, SteamType.REGULAR, 1f, 1f, exhaustThroughput);

            // Push to next in chain (turbine OR pipe that connects to next turbine)
            if (!lastExhaustSteam.isEmpty()) com.xciel.steamturbine.network.PipeNetworkManager.pushSteam(level, worldPosition, facing, lastExhaustSteam);
        } else {
            turbineSpeed = 0f;
            lastExhaustSteam = SteamData.of(0, SteamType.REGULAR, 1f, 1f, 0);
        }

        inputSteam = SteamData.empty();

        setChanged();
        sendData();
    }

    private int countTurbinesBehind(BlockPos start, Direction facing) {
        int count = 0;
        BlockPos current = start.relative(facing.getOpposite());
        while (level != null && level.isLoaded(current)) {
            var be = level.getBlockEntity(current);
            if (be instanceof SteamTurbineBlockEntity) {
                count++;
                current = current.relative(facing.getOpposite());
            } else {
                break;
            }
        }
        return count;
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        if (inputSteam.isEmpty()) {
            inputSteam = steam;
        } else {
            inputSteam = inputSteam.withPressureAndThroughputAdded(steam.getPressure(), steam.getThroughput());
        }
        setChanged();
    }

    @Override
    public SteamData produceTurbineSteam(Direction from) {
        return lastExhaustSteam;
    }

    @Override
    public boolean canReceive(Direction direction) {
        Direction facing = getBlockState().getValue(SteamTurbineBlock.FACING);
        return direction == facing.getOpposite(); // only SOUTH face = input
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return direction == getBlockState().getValue(SteamTurbineBlock.FACING).getOpposite() ? 100f : 0f;
    }

    @Override
    public boolean canConnect(Direction direction) {
        Direction facing = getBlockState().getValue(SteamTurbineBlock.FACING);
        return direction == facing.getOpposite(); // only SOUTH face accepts pipe connections
    }

    @Override
    public boolean canTurbineConnect(Direction direction) {
        Direction facing = getBlockState().getValue(SteamTurbineBlock.FACING);
        return direction == facing.getOpposite() || direction == facing; // accepts from behind and front
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Steam Turbine").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Stage: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(stageNumber + 1)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("    Efficiency: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", stageEfficiency * 100) + "%").withStyle(
                stageEfficiency >= 0.7f ? ChatFormatting.GREEN : stageEfficiency >= 0.5f ? ChatFormatting.YELLOW : ChatFormatting.RED)));
        tooltip.add(Component.literal("    Input: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f @ %.2f/t", lastInputSteam.getPressure(), lastInputSteam.getThroughput())).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.literal("    Exhaust: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f @ %.2f/t", lastExhaustSteam.getPressure(), lastExhaustSteam.getThroughput())).withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    public void onNeighborChanged() {
        setChanged();
    }

    public float getTurbineSpeed() {
        return turbineSpeed;
    }

    public float getInputPressure() {
        return lastInputSteam.getPressure();
    }

    public float getInputThroughput() {
        return lastInputSteam.getThroughput();
    }

    public float getExhaustThroughput() {
        return lastExhaustSteam.getThroughput();
    }

    public float getExhaustPressure() {
        return lastExhaustSteam.getPressure();
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public SteamData getInputSteam() {
        return inputSteam;
    }

    public SteamData getLastInputSteam() {
        return lastInputSteam;
    }

    public SteamData getLastExhaustSteam() {
        return lastExhaustSteam;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("InputSteam")) {
            inputSteam = SteamData.loadFromNBT(tag.getCompound("InputSteam"), registries);
        }
        turbineSpeed = tag.getFloat("TurbineSpeed");
        stageNumber = tag.getInt("StageNumber");
        stageEfficiency = tag.getFloat("StageEfficiency");
        if (clientPacket) {
            if (tag.contains("LastInputSteam")) {
                lastInputSteam = SteamData.loadFromNBT(tag.getCompound("LastInputSteam"), registries);
            }
            if (tag.contains("LastExhaustSteam")) {
                lastExhaustSteam = SteamData.loadFromNBT(tag.getCompound("LastExhaustSteam"), registries);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag inputTag = new CompoundTag();
        inputSteam.saveToNBT(inputTag, registries);
        tag.put("InputSteam", inputTag);
        tag.putFloat("TurbineSpeed", turbineSpeed);
        tag.putInt("StageNumber", stageNumber);
        tag.putFloat("StageEfficiency", stageEfficiency);
        CompoundTag lastInputTag = new CompoundTag();
        lastInputSteam.saveToNBT(lastInputTag, registries);
        tag.put("LastInputSteam", lastInputTag);
        CompoundTag lastExhaustTag = new CompoundTag();
        lastExhaustSteam.saveToNBT(lastExhaustTag, registries);
        tag.put("LastExhaustSteam", lastExhaustTag);
    }
}
