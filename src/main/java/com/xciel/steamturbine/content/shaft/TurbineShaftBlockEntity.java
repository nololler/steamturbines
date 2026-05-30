package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TurbineShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation, ISteamEndpoint {
    private static final float BASE_STRESS_CAPACITY = 78.0f; // SU per RPM

    private float aggregatedSpeed = 0f;
    private float aggregatedThroughput = 0f;
    private int connectedTurbineCount = 0;

    public TurbineShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        updateFromConnectedTurbines();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;

        updateFromConnectedTurbines();
    }

    private void updateFromConnectedTurbines() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(TurbineShaftBlock.FACING);

        Direction walkDir = facing.getOpposite();
        BlockPos current = worldPosition.relative(walkDir);

        float totalSpeed = 0f;
        float maxSpeed = 0f;
        float totalThroughput = 0f;
        int count = 0;

        while (level.isLoaded(current)) {
            var be = level.getBlockEntity(current);
            if (be instanceof SteamTurbineBlockEntity turbine) {
                float turbineSpeed = Math.abs(turbine.getTurbineSpeed());
                totalSpeed += turbineSpeed;
                if (turbineSpeed > maxSpeed) maxSpeed = turbineSpeed;
                totalThroughput += turbine.getInputThroughput();
                count++;
                current = current.relative(walkDir);
            } else {
                break;
            }
        }

        float prevSpeed = aggregatedSpeed;
        aggregatedSpeed = totalSpeed;
        aggregatedThroughput = totalThroughput;
        connectedTurbineCount = count;

        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        return Math.min(aggregatedSpeed, 256f);
    }

    public float getCappedSpeed() {
        return Math.min(256f, Math.max(1f, aggregatedSpeed));
    }

    @Override
    public boolean canConnect(Direction direction) {
        BlockState state = getBlockState();
        Direction facing = state.getValue(TurbineShaftBlock.FACING);
        return direction == facing.getOpposite() || direction == facing.getClockWise();
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (aggregatedSpeed <= 0f) return 0f;
        return Math.max(aggregatedSpeed * BASE_STRESS_CAPACITY / 256f, BASE_STRESS_CAPACITY / 256f);
    }

    public void onNeighborChanged() {
        updateFromConnectedTurbines();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("   Turbine Shaft   ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Connected Turbines: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(connectedTurbineCount)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Output Speed: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", getGeneratedSpeed()) + " RPM").withStyle(ChatFormatting.WHITE)));
        float suCapacity = Math.abs(getGeneratedSpeed() * calculateAddedStressCapacity());
        tooltip.add(Component.literal("  SU Capacity: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", suCapacity) + " SU").withStyle(ChatFormatting.AQUA)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        aggregatedSpeed = tag.getFloat("AggregatedSpeed");
        aggregatedThroughput = tag.contains("AggregatedThroughput") ? tag.getFloat("AggregatedThroughput") : 0f;
        connectedTurbineCount = tag.getInt("ConnectedTurbineCount");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("AggregatedSpeed", aggregatedSpeed);
        tag.putFloat("AggregatedThroughput", aggregatedThroughput);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
    }
}
