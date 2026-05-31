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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SteamTurbineBlockEntity extends SmartBlockEntity implements ISteamConsumer, ISteamEndpoint, IHaveGoggleInformation {
    private static final float MAX_RPM = 256f;
    private static final float MIN_PRESSURE_FOR_OPERATION = 0.5f;
    private static final float MAX_THROUGHPUT = 2048.0f;
    private static final float[] STAGE_EFFICIENCY = {0.8f, 0.7f, 0.6f, 0.5f, 0.4f};

    private float inputPressure = 0f;
    private float inputThroughput = 0f;
    private float receivedPressure = 0f;
    private float turbineSpeed = 0f;
    private float exhaustPressure = 0f;
    private float exhaustThroughput = 0f;
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

        float maxPressure = receivedPressure;
        float totalThroughput = 0f;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;
            var neighborBE = level.getBlockEntity(neighborPos);
            if (neighborBE instanceof ISteamTransport transport) {
                SteamData pulled = transport.pullSteam(dir.getOpposite(), getMaxReceiveRate(dir));
                if (pulled != null && !pulled.isEmpty()) {
                    maxPressure = Math.max(maxPressure, pulled.getPressure());
                    totalThroughput += pulled.getThroughput();
                }
            }
        }

        BlockPos behindPos = worldPosition.relative(facing.getOpposite());
        if (level.isLoaded(behindPos)) {
            var behindBE = level.getBlockEntity(behindPos);
            if (behindBE instanceof SteamTurbineBlockEntity prevTurbine) {
                maxPressure = Math.max(maxPressure, prevTurbine.exhaustPressure);
                totalThroughput += prevTurbine.exhaustThroughput;
            }
        }

        stageNumber = countTurbinesBehind(worldPosition, facing);

        int efficiencyIndex = Math.min(stageNumber, STAGE_EFFICIENCY.length - 1);
        stageEfficiency = STAGE_EFFICIENCY[efficiencyIndex];

        inputPressure = maxPressure;
        inputThroughput = Math.min(totalThroughput, MAX_THROUGHPUT);

        if (inputThroughput > 0) {
            lastInputSteam = SteamData.of(inputPressure, SteamType.REGULAR, 1f, 1f, inputThroughput);
        }

        if (inputPressure >= MIN_PRESSURE_FOR_OPERATION) {
            float pressureFactor = Math.min(inputPressure / SteamConstants.MAX_PRESSURE, 1.0f);
            turbineSpeed = MAX_RPM * pressureFactor * stageEfficiency;
            exhaustPressure = inputPressure * (1.0f - stageEfficiency * 0.5f);
            exhaustThroughput = Math.min(inputThroughput * stageEfficiency, MAX_THROUGHPUT);
            lastExhaustSteam = SteamData.of(exhaustPressure, SteamType.REGULAR, 1f, 1f, exhaustThroughput);
        } else {
            turbineSpeed = 0f;
            exhaustPressure = 0f;
            exhaustThroughput = 0f;
        }

        receivedPressure = 0f;

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
        receivedPressure = Math.max(receivedPressure, steam.getPressure());
        if (steam.getThroughput() > 0) {
            lastInputSteam = SteamData.of(Math.max(receivedPressure, steam.getPressure()), SteamType.REGULAR, 1f, 1f, steam.getThroughput());
        }
        setChanged();
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Encased Turbine").withStyle(ChatFormatting.GOLD));
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
        return inputPressure;
    }

    public float getInputThroughput() {
        return inputThroughput;
    }

    public float getExhaustThroughput() {
        return exhaustThroughput;
    }

    public float getExhaustPressure() {
        return exhaustPressure;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inputPressure = tag.getFloat("InputPressure");
        inputThroughput = tag.contains("InputThroughput") ? tag.getFloat("InputThroughput") : 0f;
        turbineSpeed = tag.getFloat("TurbineSpeed");
        exhaustPressure = tag.getFloat("ExhaustPressure");
        exhaustThroughput = tag.contains("ExhaustThroughput") ? tag.getFloat("ExhaustThroughput") : 0f;
        stageNumber = tag.getInt("StageNumber");
        stageEfficiency = tag.getFloat("StageEfficiency");
        receivedPressure = tag.getFloat("ReceivedPressure");
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
        tag.putFloat("InputPressure", inputPressure);
        tag.putFloat("InputThroughput", inputThroughput);
        tag.putFloat("TurbineSpeed", turbineSpeed);
        tag.putFloat("ExhaustPressure", exhaustPressure);
        tag.putFloat("ExhaustThroughput", exhaustThroughput);
        tag.putInt("StageNumber", stageNumber);
        tag.putFloat("StageEfficiency", stageEfficiency);
        tag.putFloat("ReceivedPressure", receivedPressure);
        CompoundTag lastInputTag = new CompoundTag();
        lastInputSteam.saveToNBT(lastInputTag, registries);
        tag.put("LastInputSteam", lastInputTag);
        CompoundTag lastExhaustTag = new CompoundTag();
        lastExhaustSteam.saveToNBT(lastExhaustTag, registries);
        tag.put("LastExhaustSteam", lastExhaustTag);
    }
}
