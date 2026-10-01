package com.xciel.turbines.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineChamber;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineIOBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

public class HydroTurbineShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private static final int MAX_NETWORK_FLOW_PER_TICK = 100;
    private static final int MAX_SOURCE_POOL_FLOW_PER_TICK = 50;
    private static final int NETWORK_FLOW_SMOOTHING_TICKS = 20;
    private static final float MAX_RPM = 256;
    private static final float MAX_SU_AT_FULL_SETUP = 2_000_000;

    private HydroTurbineChamber.Result chamber;
    private String chamberStatus = "block.turbines.hydro_shaft.status.checking";
    private int scanCooldown;
    private int stages;
    private int renderedStageCount;
    private int sourceWaterBlocks;
    private int flowRate;
    private int pendingExhaustWater;
    private int networkBuffer;
    private final int[] recentNetworkFlows = new int[NETWORK_FLOW_SMOOTHING_TICKS];
    private int networkFlowSampleCount;
    private int networkFlowSampleIndex;
    private int networkFlowSampleTotal;
    private float activeWaterFactor;
    private boolean structureValid;
    private boolean outputOwner;
    private boolean networkMode;
    private boolean waterVisualsVisible;
    private boolean clientWaterActive;
    private boolean clientWaterStateInitialized;
    private boolean waterTransitionFromActive;
    private float waterTransitionStart;

    private static final float WATER_RUSH_DURATION_TICKS = 4f;

    public HydroTurbineShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        if (--scanCooldown <= 0)
            refreshChamber();

        boolean previousNetworkMode = networkMode;
        float previousWaterFactor = activeWaterFactor;
        int previousFlowRate = flowRate;
        int actualFlowRate = transferWater();
        if (!structureValid || !outputOwner) {
            resetNetworkFlowAverage();
            activeWaterFactor = 0;
            flowRate = 0;
        } else if (networkMode) {
            if (!previousNetworkMode)
                resetNetworkFlowAverage();
            flowRate = averageNetworkFlow(actualFlowRate);
            activeWaterFactor = Math.min(1f, (float) flowRate / MAX_NETWORK_FLOW_PER_TICK);
        } else {
            resetNetworkFlowAverage();
            flowRate = actualFlowRate;
        }
        if (previousFlowRate != flowRate || Math.abs(previousWaterFactor - activeWaterFactor) > 0.0001f) {
            updateGeneratedRotation();
            setChanged();
            sendData();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide)
            refreshChamber();
    }

    public void onNeighborChanged() {
        if (level != null && !level.isClientSide)
            refreshChamber();
    }

    private void refreshChamber() {
        if (level == null || level.isClientSide)
            return;
        scanCooldown = 10;

        HydroTurbineChamber.Result next = HydroTurbineChamber.inspect(level, worldPosition);
        int nextStages = next.stages();
        int nextRenderedStages = next.structureValid() ? nextStages : renderedStageCount;
        int nextWater = next.sourceWaterBlocks();
        boolean nextValid = next.structureValid();
        boolean nextOwner = next.isOutputOwner(worldPosition);
        boolean nextWaterVisualsVisible = next.structureValid()
            ? HydroTurbineChamber.hasTransparentWalls(level, next) : waterVisualsVisible;
        HydroTurbineIOBlockEntity inlet = next.inlet() != null
            && level.getBlockEntity(next.inlet()) instanceof HydroTurbineIOBlockEntity io ? io : null;
        int nextNetworkBuffer = inlet == null ? 0 : inlet.getInletBufferAmount();
        boolean nextNetworkMode = inlet != null && inlet.isNetworkInputMode();
        boolean changed = chamber == null || !chamber.equals(next) || stages != nextStages
            || renderedStageCount != nextRenderedStages
            || sourceWaterBlocks != nextWater || structureValid != nextValid || outputOwner != nextOwner
            || networkBuffer != nextNetworkBuffer || networkMode != nextNetworkMode
            || waterVisualsVisible != nextWaterVisualsVisible;

        chamber = next;
        chamberStatus = next.status();
        stages = nextStages;
        renderedStageCount = nextRenderedStages;
        sourceWaterBlocks = nextWater;
        structureValid = nextValid;
        outputOwner = nextOwner;
        networkBuffer = nextNetworkBuffer;
        networkMode = nextNetworkMode;
        waterVisualsVisible = nextWaterVisualsVisible;

        if (changed) {
            invalidateRenderBoundingBox();
            updateGeneratedRotation();
            setChanged();
            sendData();
        }
    }

    private int transferWater() {
        if (chamber == null || chamber.exhaust() == null) {
            activeWaterFactor = 0;
            return 0;
        }

        var exhaustEntity = level.getBlockEntity(chamber.exhaust());
        if (!(exhaustEntity instanceof HydroTurbineIOBlockEntity exhaust)) {
            activeWaterFactor = 0;
            return 0;
        }

        exhaust.flushBufferedWater();
        int delivered = 0;
        if (pendingExhaustWater > 0) {
            int accepted = exhaust.acceptWater(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,
                pendingExhaustWater));
            pendingExhaustWater -= accepted;
            delivered += accepted;
            if (accepted > 0)
                setChanged();
            if (pendingExhaustWater > 0) {
                activeWaterFactor = 0;
                return delivered;
            }
        }

        if (!structureValid || !outputOwner) {
            activeWaterFactor = 0;
            return delivered;
        }

        var inletEntity = level.getBlockEntity(chamber.inlet());
        if (!(inletEntity instanceof HydroTurbineIOBlockEntity inlet)) {
            activeWaterFactor = 0;
            return delivered;
        }

        int currentNetworkBuffer = inlet.getInletBufferAmount();
        boolean useNetworkMode = currentNetworkBuffer > 0 || chamber.sourceWaterBlocks() <= 0;
        if (networkBuffer != currentNetworkBuffer || networkMode != useNetworkMode) {
            networkBuffer = currentNetworkBuffer;
            networkMode = useNetworkMode;
            setChanged();
            sendData();
        }
        int requiredPoolWater = HydroTurbineChamber.minimumSourceWater(chamber.stages());
        boolean usePoolMode = !useNetworkMode && chamber.sourceWaterBlocks() > 0;
        if (!useNetworkMode && !usePoolMode) {
            activeWaterFactor = 0;
            return delivered;
        }

        float poolWaterFactor = Math.min(1f, (float) chamber.sourceWaterBlocks() / requiredPoolWater);
        int requested = calculateFlowRate(useNetworkMode ? 1f : poolWaterFactor, useNetworkMode);
        FluidStack available = inlet.simulateDrainWater(requested, useNetworkMode);
        if (available.isEmpty()) {
            activeWaterFactor = 0;
            return delivered;
        }

        int fillable = exhaust.simulateAcceptWater(available);
        int amount = Math.min(available.getAmount(), fillable);
        if (amount <= 0) {
            activeWaterFactor = 0;
            return delivered;
        }

        FluidStack drained = inlet.drainWater(amount, useNetworkMode);
        if (drained.isEmpty()) {
            activeWaterFactor = 0;
            return delivered;
        }

        int accepted = Math.min(drained.getAmount(), exhaust.acceptWater(drained));
        if (accepted < drained.getAmount()) {
            pendingExhaustWater += drained.getAmount() - accepted;
            setChanged();
        }
        activeWaterFactor = useNetworkMode
            ? Math.min(1f, (float) drained.getAmount() / MAX_NETWORK_FLOW_PER_TICK) : poolWaterFactor;
        return delivered + accepted;
    }

    private int calculateFlowRate(float waterFactor, boolean networkMode) {
        if (stages <= 0 || waterFactor <= 0)
            return 0;
        return networkMode ? MAX_NETWORK_FLOW_PER_TICK : MAX_SOURCE_POOL_FLOW_PER_TICK;
    }

    private int averageNetworkFlow(int flow) {
        int sample = Math.max(0, Math.min(MAX_NETWORK_FLOW_PER_TICK, flow));
        if (networkFlowSampleCount == recentNetworkFlows.length) {
            networkFlowSampleTotal -= recentNetworkFlows[networkFlowSampleIndex];
        } else {
            networkFlowSampleCount++;
        }
        recentNetworkFlows[networkFlowSampleIndex] = sample;
        networkFlowSampleTotal += sample;
        networkFlowSampleIndex = (networkFlowSampleIndex + 1) % recentNetworkFlows.length;
        return Math.round((float) networkFlowSampleTotal / networkFlowSampleCount);
    }

    private void resetNetworkFlowAverage() {
        Arrays.fill(recentNetworkFlows, 0);
        networkFlowSampleCount = 0;
        networkFlowSampleIndex = 0;
        networkFlowSampleTotal = 0;
    }

    private float stageFactor() {
        return Math.min(1f, (float) stages / HydroTurbineChamber.MAX_STAGES);
    }

    @Override
    public float getGeneratedSpeed() {
        if (!structureValid || !outputOwner || flowRate <= 0)
            return 0;
        return MAX_RPM * activeWaterFactor;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float speed = Math.abs(getGeneratedSpeed());
        if (speed <= 0) {
            lastCapacityProvided = 0;
            return 0;
        }
        float totalSU = MAX_SU_AT_FULL_SETUP * activeWaterFactor * stageFactor();
        // Choose a capacity whose float product with the current RPM is exactly the rounded SU;
        // otherwise float precision can leave decimal digits in Create's goggles overlay.
        float roundedSU = Math.round(totalSU);
        float capacity = roundedSU / speed;
        for (int i = 0; i < 8 && capacity * speed != roundedSU; i++)
            capacity = capacity * speed < roundedSU ? Math.nextUp(capacity) : Math.nextDown(capacity);
        lastCapacityProvided = capacity;
        return lastCapacityProvided;
    }

    public boolean shouldRenderWaterColumn() {
        return waterVisualsVisible && (clientWaterActive
            || clientWaterStateInitialized && waterTransitionFromActive && getWaterTransitionProgress() < 1);
    }

    public int getChamberStages() {
        return renderedStageCount;
    }

    public boolean isWaterActiveForRender() {
        return clientWaterActive;
    }

    public boolean wasWaterActiveBeforeTransition() {
        return waterTransitionFromActive;
    }

    public float getWaterTransitionProgress() {
        if (level == null || !clientWaterStateInitialized)
            return 1;
        float elapsed = AnimationTickHolder.getRenderTime(level) - waterTransitionStart;
        return Math.max(0, Math.min(1, elapsed / WATER_RUSH_DURATION_TICKS));
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!shouldRenderWaterColumn() || renderedStageCount <= 0)
            return new AABB(worldPosition);
        Direction facing = getBlockState().getValue(HydroTurbineShaftBlock.FACING);
        double minY = facing == Direction.UP ? worldPosition.getY() - renderedStageCount : worldPosition.getY();
        double maxY = facing == Direction.UP ? worldPosition.getY() + 1
            : worldPosition.getY() + renderedStageCount + 1;
        return new AABB(worldPosition.getX() - 1, minY, worldPosition.getZ() - 1,
            worldPosition.getX() + 2, maxY, worldPosition.getZ() + 2);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable(
            "block.turbines.hydro_shaft.goggles.title")).withStyle(ChatFormatting.GOLD));
        boolean invalidChamber = !structureValid;
        if (invalidChamber)
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.structure_invalid").withStyle(ChatFormatting.RED)));
        tooltip.add(Component.literal("    ").append(Component.translatable(
            "block.turbines.hydro_shaft.goggles.stages", stages, HydroTurbineChamber.MAX_STAGES)
            .withStyle(structureValid ? ChatFormatting.GRAY : ChatFormatting.RED)));
        boolean sourceMode = !networkMode && sourceWaterBlocks > 0;
        boolean showSourceWater = !networkMode;
        if (showSourceWater)
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.source_water", sourceWaterBlocks,
                    HydroTurbineChamber.minimumSourceWater(stages))
                .withStyle(sourceWaterBlocks > 0 ? ChatFormatting.WHITE : ChatFormatting.RED)));
        tooltip.add(Component.literal("    ").append(Component.translatable(
            "block.turbines.hydro_shaft.goggles.water_flow", flowRate).withStyle(ChatFormatting.AQUA)));
        if (isPlayerSneaking)
            tooltip.add(Component.literal("    ").append(Component.translatable(waterVisualsVisible
                ? "block.turbines.hydro_shaft.goggles.water_visuals_visible"
                : "block.turbines.hydro_shaft.goggles.water_visuals_hidden")
                .withStyle(waterVisualsVisible ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)));
        if (networkMode)
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.network_buffer", networkBuffer,
                    HydroTurbineIOBlockEntity.INLET_BUFFER_CAPACITY).withStyle(ChatFormatting.GRAY)));
        if (invalidChamber) {
            if (isPlayerSneaking) {
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_shaft.goggles.checklist").withStyle(ChatFormatting.GOLD)));
                for (String item : HydroTurbineChamber.playerChecklist())
                    tooltip.add(Component.literal("      • ").append(Component.translatable(item).withStyle(ChatFormatting.GRAY)));
            }
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.current_snag", Component.translatable(chamberStatus))
                .withStyle(ChatFormatting.RED)));
        } else if (networkMode) {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.inlet_mode_network").withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.network_rate", MAX_NETWORK_FLOW_PER_TICK,
                    MAX_NETWORK_FLOW_PER_TICK * 20).withStyle(ChatFormatting.DARK_GRAY)));
            if (networkBuffer <= 0)
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_shaft.goggles.network_supply_needed").withStyle(ChatFormatting.RED)));
        } else if (sourceMode) {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.inlet_mode_source").withStyle(ChatFormatting.AQUA)));
        } else if (structureValid && sourceWaterBlocks <= 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.network_supply_needed").withStyle(ChatFormatting.RED)));
        } else if (structureValid && !outputOwner)
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_shaft.goggles.shared_output").withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        boolean previousWaterActive = clientWaterActive;
        boolean wasInitialized = clientWaterStateInitialized;
        boolean previousWaterVisualsVisible = waterVisualsVisible;
        super.read(tag, registries, clientPacket);
        stages = tag.getInt("Stages");
        renderedStageCount = tag.contains("RenderedStageCount") ? tag.getInt("RenderedStageCount") : stages;
        sourceWaterBlocks = tag.getInt("SourceWaterBlocks");
        flowRate = tag.getInt("FlowRate");
        pendingExhaustWater = tag.getInt("PendingExhaustWater");
        networkBuffer = tag.getInt("NetworkBuffer");
        networkMode = tag.contains("NetworkMode") ? tag.getBoolean("NetworkMode") : tag.getBoolean("TankMode");
        activeWaterFactor = tag.getFloat("ActiveWaterFactor");
        structureValid = tag.getBoolean("StructureValid");
        outputOwner = tag.getBoolean("OutputOwner");
        waterVisualsVisible = tag.getBoolean("WaterVisualsVisible");
        chamberStatus = tag.getString("ChamberStatus");
        if (!chamberStatus.startsWith("block.turbines.hydro_shaft.status."))
            chamberStatus = "block.turbines.hydro_shaft.status.missing_stages";
        if (level != null && level.isClientSide) {
            boolean nextWaterActive = structureValid && outputOwner && flowRate > 0 && activeWaterFactor > 0;
            if (!wasInitialized || previousWaterVisualsVisible != waterVisualsVisible) {
                invalidateRenderBoundingBox();
            }
            if (!wasInitialized) {
                clientWaterStateInitialized = true;
                waterTransitionFromActive = false;
                waterTransitionStart = AnimationTickHolder.getRenderTime(level);
            } else if (nextWaterActive != previousWaterActive) {
                waterTransitionFromActive = previousWaterActive;
                waterTransitionStart = AnimationTickHolder.getRenderTime(level);
                invalidateRenderBoundingBox();
            }
            clientWaterActive = nextWaterActive;
        }
        chamber = null;
        scanCooldown = 0;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Stages", stages);
        tag.putInt("RenderedStageCount", renderedStageCount);
        tag.putInt("SourceWaterBlocks", sourceWaterBlocks);
        tag.putInt("FlowRate", flowRate);
        tag.putInt("PendingExhaustWater", pendingExhaustWater);
        tag.putInt("NetworkBuffer", networkBuffer);
        tag.putBoolean("NetworkMode", networkMode);
        tag.putFloat("ActiveWaterFactor", activeWaterFactor);
        tag.putBoolean("StructureValid", structureValid);
        tag.putBoolean("OutputOwner", outputOwner);
        tag.putBoolean("WaterVisualsVisible", waterVisualsVisible);
        tag.putString("ChamberStatus", chamberStatus);
    }
}
