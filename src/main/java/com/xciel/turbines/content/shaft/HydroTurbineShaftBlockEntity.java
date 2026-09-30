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

import java.util.List;

public class HydroTurbineShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private static final int MAX_FLOW_PER_TICK = 50;
    private static final float MAX_RPM = 256;
    private static final float MAX_SU_AT_FULL_SETUP = 2_000_000;
    private static final long TANK_WATER_LOSS_INTERVAL = 20L * 60 * 60;
    private static final int TANK_WATER_LOSS_AMOUNT = 1_000;

    private HydroTurbineChamber.Result chamber;
    private String chamberStatus = "Checking the Hydro chamber.";
    private int scanCooldown;
    private int stages;
    private int renderedStageCount;
    private int sourceWaterBlocks;
    private int flowRate;
    private int pendingExhaustWater;
    private int tankNetworkCapacity;
    private int tankNetworkWater;
    private int tankNetworkRequirement;
    private long tankNetworkRunningTicks;
    private int pendingTankWaterLoss;
    private float activeWaterFactor;
    private boolean structureValid;
    private boolean outputOwner;
    private boolean sharedFluidNetwork;
    private boolean unresolvedFluidNetwork;
    private boolean tankMode;
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

        float previousWaterFactor = activeWaterFactor;
        int nextFlowRate = transferWater();
        if (flowRate != nextFlowRate || Math.abs(previousWaterFactor - activeWaterFactor) > 0.0001f) {
            flowRate = nextFlowRate;
            updateGeneratedRotation();
            setChanged();
            sendData();
        }
        processTankWaterMaintenance();
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
        HydroTurbineChamber.FluidNetworkInfo networkInfo = inlet == null
            ? new HydroTurbineChamber.FluidNetworkInfo(0, 0, false, true)
            : inlet.getFluidNetworkInfo();
        int nextTankCapacity = networkInfo.tankCapacity();
        int nextTankWater = networkInfo.waterAmount();
        int nextTankRequirement = networkInfo.requiredTankWater();
        boolean nextSharedNetwork = networkInfo.sharedHydroUnit();
        boolean nextUnresolvedNetwork = networkInfo.unresolvedHydroUnit();
        boolean nextTankMode = networkInfo.satisfiesTankInput() && inlet != null
            && (inlet.getInletBufferAmount() > 0 || !networkInfo.tankControllers().isEmpty());
        boolean changed = chamber == null || !chamber.equals(next) || stages != nextStages
            || renderedStageCount != nextRenderedStages
            || sourceWaterBlocks != nextWater || structureValid != nextValid || outputOwner != nextOwner
            || tankNetworkCapacity != nextTankCapacity || tankNetworkWater != nextTankWater
            || tankNetworkRequirement != nextTankRequirement
            || sharedFluidNetwork != nextSharedNetwork || unresolvedFluidNetwork != nextUnresolvedNetwork
            || tankMode != nextTankMode || waterVisualsVisible != nextWaterVisualsVisible;

        chamber = next;
        chamberStatus = next.status();
        stages = nextStages;
        renderedStageCount = nextRenderedStages;
        sourceWaterBlocks = nextWater;
        structureValid = nextValid;
        outputOwner = nextOwner;
        tankNetworkCapacity = nextTankCapacity;
        tankNetworkWater = nextTankWater;
        tankNetworkRequirement = nextTankRequirement;
        sharedFluidNetwork = nextSharedNetwork;
        unresolvedFluidNetwork = nextUnresolvedNetwork;
        tankMode = nextTankMode;
        waterVisualsVisible = nextWaterVisualsVisible;

        if (changed) {
            invalidateRenderBoundingBox();
            updateGeneratedRotation();
            setChanged();
            sendData();
        }
    }

    private int transferWater() {
        if (chamber == null || chamber.exhaust() == null)
            return 0;

        var exhaustEntity = level.getBlockEntity(chamber.exhaust());
        if (!(exhaustEntity instanceof HydroTurbineIOBlockEntity exhaust))
            return 0;

        exhaust.flushBufferedWater();
        int delivered = 0;
        if (pendingExhaustWater > 0) {
            int accepted = exhaust.acceptWater(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,
                pendingExhaustWater));
            pendingExhaustWater -= accepted;
            delivered += accepted;
            if (accepted > 0)
                setChanged();
            if (pendingExhaustWater > 0)
                return delivered;
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

        HydroTurbineChamber.FluidNetworkInfo networkInfo = inlet.getFluidNetworkInfo();
        boolean useTankMode = networkInfo.satisfiesTankInput()
            && (inlet.getInletBufferAmount() > 0 || !networkInfo.tankControllers().isEmpty());
        int requiredPoolWater = HydroTurbineChamber.minimumSourceWater(chamber.stages());
        boolean usePoolMode = !useTankMode && chamber.sourceWaterBlocks() > 0;
        if (!useTankMode && !usePoolMode) {
            activeWaterFactor = 0;
            return delivered;
        }

        activeWaterFactor = useTankMode
            ? Math.min(1f, (float) networkInfo.waterAmount() / Math.max(1, networkInfo.requiredTankWater()))
            : Math.min(1f, (float) chamber.sourceWaterBlocks() / requiredPoolWater);
        int requested = calculateFlowRate(activeWaterFactor);
        FluidStack available = inlet.simulateDrainWater(requested, useTankMode);
        if (available.isEmpty())
            return delivered;

        int fillable = exhaust.simulateAcceptWater(available);
        int amount = Math.min(available.getAmount(), fillable);
        if (amount <= 0)
            return delivered;

        FluidStack drained = inlet.drainWater(amount, useTankMode);
        if (drained.isEmpty())
            return delivered;

        int accepted = Math.min(drained.getAmount(), exhaust.acceptWater(drained));
        if (accepted < drained.getAmount()) {
            pendingExhaustWater += drained.getAmount() - accepted;
            setChanged();
        }
        return delivered + accepted;
    }

    private int calculateFlowRate(float waterFactor) {
        if (stages <= 0 || waterFactor <= 0)
            return 0;
        return MAX_FLOW_PER_TICK;
    }

    private void processTankWaterMaintenance() {
        if (structureValid && outputOwner && tankMode && flowRate > 0) {
            tankNetworkRunningTicks++;
            if (tankNetworkRunningTicks >= TANK_WATER_LOSS_INTERVAL) {
                tankNetworkRunningTicks -= TANK_WATER_LOSS_INTERVAL;
                pendingTankWaterLoss += TANK_WATER_LOSS_AMOUNT;
                setChanged();
            }
        }

        if (pendingTankWaterLoss <= 0 || chamber == null || chamber.inlet() == null)
            return;
        if (!(level.getBlockEntity(chamber.inlet()) instanceof HydroTurbineIOBlockEntity inlet))
            return;

        int discarded = inlet.discardConnectedTankWater(pendingTankWaterLoss);
        pendingTankWaterLoss -= discarded;
        int totalDiscarded = discarded;
        if (pendingTankWaterLoss > 0 && chamber.exhaust() != null
            && level.getBlockEntity(chamber.exhaust()) instanceof HydroTurbineIOBlockEntity exhaust) {
            int exhaustDiscarded = exhaust.discardBufferedExhaustWater(pendingTankWaterLoss);
            pendingTankWaterLoss -= exhaustDiscarded;
            totalDiscarded += exhaustDiscarded;
        }

        if (totalDiscarded > 0 || pendingTankWaterLoss == 0) {
            setChanged();
            sendData();
        }
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
        tooltip.add(Component.literal("    Hydro Shaft:").withStyle(ChatFormatting.GOLD));
        boolean invalidChamber = !structureValid;
        if (invalidChamber)
            tooltip.add(Component.literal("    The chamber build didn't pass; some blocks don't match the Hydro layout.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("    Stages: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(stages + " / " + HydroTurbineChamber.MAX_STAGES)
                .withStyle(structureValid ? ChatFormatting.WHITE : ChatFormatting.RED)));
        boolean sourceMode = !tankMode && sourceWaterBlocks > 0;
        boolean tankNetworkPopulated = tankNetworkCapacity > 0 || tankNetworkWater > 0
            || sharedFluidNetwork || unresolvedFluidNetwork;
        boolean showTankNetwork = tankMode || (!sourceMode && tankNetworkPopulated);
        boolean showSourceWater = !tankMode && !showTankNetwork;
        if (showSourceWater)
            tooltip.add(Component.literal("    Source water: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sourceWaterBlocks + " / " + HydroTurbineChamber.minimumSourceWater(stages)
                    + " for full SU")
                    .withStyle(sourceWaterBlocks > 0 ? ChatFormatting.WHITE : ChatFormatting.RED)));
        tooltip.add(Component.literal("    Water flow: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(flowRate + " mB/t").withStyle(ChatFormatting.AQUA)));
        if (isPlayerSneaking)
            tooltip.add(Component.literal("    Water visuals: " + (waterVisualsVisible
                ? "enabled (transparent side walls)" : "off (opaque side walls)")
                ).withStyle(waterVisualsVisible ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (showTankNetwork)
            tooltip.add(Component.literal("    Tank network: " + tankNetworkWater + " / "
                + tankNetworkRequirement + " mB water for full SU; " + tankNetworkCapacity + " mB capacity")
                .withStyle(ChatFormatting.GRAY));
        boolean createTankNetworkAvailable = !sharedFluidNetwork && !unresolvedFluidNetwork
            && tankNetworkCapacity > 0 && tankNetworkWater > 0;
        if (invalidChamber) {
            tooltip.add(Component.literal("    Hydro Unit checklist:").withStyle(ChatFormatting.GOLD));
            for (String item : HydroTurbineChamber.playerChecklist())
                tooltip.add(Component.literal("      • " + item).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("    Current snag: " + chamberStatus).withStyle(ChatFormatting.RED));
        } else if (showTankNetwork && sharedFluidNetwork) {
            tooltip.add(Component.literal("    This fluid network is already used by another Hydro Unit. Use a separate network.")
                .withStyle(ChatFormatting.RED));
        } else if (showTankNetwork && unresolvedFluidNetwork) {
            tooltip.add(Component.literal("    I couldn't validate the whole fluid network. Load the connected pipes and tanks.")
                .withStyle(ChatFormatting.RED));
        } else if (tankMode) {
            tooltip.add(Component.literal("    Inlet mode: Create fluid network").withStyle(ChatFormatting.AQUA));
            long ticksUntilLoss = Math.max(0, TANK_WATER_LOSS_INTERVAL - tankNetworkRunningTicks);
            tooltip.add(Component.literal("    Network upkeep: 1,000 mB per active hour (next in "
                + String.format("%.1f", ticksUntilLoss / 20f / 60f) + " min)")
                .withStyle(ChatFormatting.DARK_GRAY));
        } else if (sourceWaterBlocks > 0) {
            tooltip.add(Component.literal("    Inlet mode: source-water pool").withStyle(ChatFormatting.AQUA));
        } else if (structureValid && showTankNetwork && createTankNetworkAvailable && !tankMode) {
            tooltip.add(Component.literal("    Create network water is available; output scales with the water amount.")
                .withStyle(ChatFormatting.GRAY));
        } else if (structureValid && showTankNetwork && !createTankNetworkAvailable && !sharedFluidNetwork
            && !unresolvedFluidNetwork) {
            tooltip.add(Component.literal("    Add water to this Create network to run; output scales with the amount available.")
                .withStyle(ChatFormatting.RED));
        } else if (structureValid && showSourceWater && !createTankNetworkAvailable
            && sourceWaterBlocks <= 0) {
            tooltip.add(Component.literal("    Add any water to run; the displayed amounts are targets for full SU.")
                .withStyle(ChatFormatting.RED));
        } else if (structureValid && !outputOwner)
            tooltip.add(Component.literal("    Shared output shaft").withStyle(ChatFormatting.DARK_GRAY));
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
        tankNetworkCapacity = tag.getInt("TankNetworkCapacity");
        tankNetworkWater = tag.getInt("TankNetworkWater");
        tankNetworkRequirement = tag.getInt("TankNetworkRequirement");
        tankNetworkRunningTicks = tag.getLong("TankNetworkRunningTicks");
        pendingTankWaterLoss = tag.getInt("PendingTankWaterLoss");
        activeWaterFactor = tag.getFloat("ActiveWaterFactor");
        structureValid = tag.getBoolean("StructureValid");
        outputOwner = tag.getBoolean("OutputOwner");
        sharedFluidNetwork = tag.getBoolean("SharedFluidNetwork");
        unresolvedFluidNetwork = tag.getBoolean("UnresolvedFluidNetwork");
        tankMode = tag.getBoolean("TankMode");
        waterVisualsVisible = tag.getBoolean("WaterVisualsVisible");
        chamberStatus = tag.getString("ChamberStatus");
        if (chamberStatus.isBlank())
            chamberStatus = "The chamber blocks don't match the Hydro Unit layout.";
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
        tag.putInt("TankNetworkCapacity", tankNetworkCapacity);
        tag.putInt("TankNetworkWater", tankNetworkWater);
        tag.putInt("TankNetworkRequirement", tankNetworkRequirement);
        tag.putLong("TankNetworkRunningTicks", tankNetworkRunningTicks);
        tag.putInt("PendingTankWaterLoss", pendingTankWaterLoss);
        tag.putFloat("ActiveWaterFactor", activeWaterFactor);
        tag.putBoolean("StructureValid", structureValid);
        tag.putBoolean("OutputOwner", outputOwner);
        tag.putBoolean("SharedFluidNetwork", sharedFluidNetwork);
        tag.putBoolean("UnresolvedFluidNetwork", unresolvedFluidNetwork);
        tag.putBoolean("TankMode", tankMode);
        tag.putBoolean("WaterVisualsVisible", waterVisualsVisible);
        tag.putString("ChamberStatus", chamberStatus);
    }
}
