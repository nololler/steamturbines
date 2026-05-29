package com.xciel.steamturbine.content.turbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;

public class SteamTurbineBlockEntity extends SmartBlockEntity implements ISteamConsumer, ISteamEndpoint, IHaveGoggleInformation {
    private static final float MAX_RPM = 256f;
    private static final float MIN_PRESSURE_FOR_OPERATION = 0.5f;

    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private float inputPressure = 0f;
    private float turbineSpeed = 0f;

    public SteamTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;

        // Calculate input pressure from received steam (take max from all directions)
        float maxPressure = 0f;
        for (Direction dir : Direction.values()) {
            SteamData steam = receivedSteam.get(dir);
            if (steam != null && !steam.isEmpty()) {
                maxPressure = Math.max(maxPressure, steam.getPressure());
            }
        }
        inputPressure = maxPressure;

        // Calculate turbine speed from pressure
        if (inputPressure >= MIN_PRESSURE_FOR_OPERATION) {
            float pressureFactor = Math.min(inputPressure / SteamConstants.MAX_PRESSURE, 1.0f);
            turbineSpeed = MAX_RPM * pressureFactor;
        } else {
            turbineSpeed = 0f;
        }

        // Clear received steam for next cycle
        for (Direction dir : Direction.values()) {
            receivedSteam.put(dir, SteamData.empty());
        }

        setChanged();
        sendData();
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam.isEmpty()) return;
        receivedSteam.put(direction, steam);
        setChanged();
    }

    @Override
    public boolean canReceive(Direction direction) {
        BlockState state = getBlockState();
        Direction facing = state.getValue(SteamTurbineBlock.FACING);
        return direction == facing; // Steam input from front (FACING direction)
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return 100f;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return canReceive(direction);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Encased Turbine").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Input Pressure: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", inputPressure)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Turbine RPM: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", turbineSpeed)).withStyle(ChatFormatting.WHITE)));
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

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inputPressure = tag.getFloat("InputPressure");
        turbineSpeed = tag.getFloat("TurbineSpeed");
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("InputPressure", inputPressure);
        tag.putFloat("TurbineSpeed", turbineSpeed);
    }
}
