package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TurbineShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    private static final float BASE_STRESS_CAPACITY = 4.0f; // SU per RPM

    private float aggregatedSpeed = 0f;
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
        Direction inputDir = ((TurbineShaftBlock) state.getBlock()).getTurbineInputDirection(state);
        BlockPos inputPos = worldPosition.relative(inputDir);

        float maxSpeed = 0f;
        int count = 0;

        // Check immediate input neighbor for turbine
        if (level.isLoaded(inputPos)) {
            BlockEntity be = level.getBlockEntity(inputPos);
            if (be instanceof SteamTurbineBlockEntity turbine) {
                maxSpeed = Math.max(maxSpeed, Math.abs(turbine.getTurbineSpeed()));
                count++;
            }
        }

        aggregatedSpeed = maxSpeed;
        connectedTurbineCount = count;

        // Notify kinetic network if speed changed
        float generated = getGeneratedSpeed();
        if (Math.abs(generated - aggregatedSpeed) > 0.01f) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return aggregatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return BASE_STRESS_CAPACITY;
    }

    public void onNeighborChanged() {
        updateFromConnectedTurbines();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Turbine Shaft").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Connected Turbines: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(connectedTurbineCount)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Output Speed: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", aggregatedSpeed) + " RPM").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("  Output Capacity: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", Math.abs(aggregatedSpeed * BASE_STRESS_CAPACITY)) + " SU").withStyle(ChatFormatting.WHITE)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        aggregatedSpeed = tag.getFloat("AggregatedSpeed");
        connectedTurbineCount = tag.getInt("ConnectedTurbineCount");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("AggregatedSpeed", aggregatedSpeed);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
    }
}
