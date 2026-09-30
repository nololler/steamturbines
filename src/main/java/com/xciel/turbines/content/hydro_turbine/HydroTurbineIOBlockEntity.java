package com.xciel.turbines.content.hydro_turbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.turbines.content.boiler.SteamBoilerBlock;
import com.xciel.turbines.content.boiler.SteamBoilerBlockEntity;
import com.xciel.turbines.registrate.STParticleTypes;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class HydroTurbineIOBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int INLET_BUFFER_CAPACITY = 8_000;
    public static final int EXHAUST_BUFFER_CAPACITY = 32_000;
    public static final int EXHAUST_SOURCE_AMOUNT = 1_000;
    public static final long EXHAUST_SOURCE_INTERVAL = 100;
    public static final int MAX_FLUID_TRANSFER_PER_TICK = 50;

    private final FluidTank inletBuffer;
    private final FluidTank exhaustBuffer;
    private final IFluidHandler inletPipeHandler;
    private final IFluidHandler exhaustPipeHandler;
    private final Map<BlockPos, FluidNeighbor> knownFluidNeighbors = new HashMap<>();
    private boolean fluidTopologyInitialized;
    private Level lastFluidTopologyLevel;

    private OpenEndedPipe poolIntakePipe;
    private HydroTurbineChamber.FluidNetworkInfo fluidNetworkInfo =
        new HydroTurbineChamber.FluidNetworkInfo(0, 0, false, true);
    private int sourceWaterBlocks;
    private long nextExhaustOutputTick;
    private long lastExternalExhaustDrainTick = Long.MIN_VALUE;
    private long inletTransferTick = Long.MIN_VALUE;
    private long exhaustTransferTick = Long.MIN_VALUE;
    private int inletTransferredThisTick;
    private int exhaustTransferredThisTick;
    private boolean exhaustBlocked;
    private int lastSyncedSourceWater = -1;
    private int lastSyncedInletBuffer = -1;
    private int lastSyncedExhaustBuffer = -1;
    private int lastSyncedNetworkWater = -1;
    private int lastSyncedNetworkCapacity = -1;
    private int lastSyncedNetworkRequirement = -1;
    private boolean lastSyncedNetworkShared;
    private boolean lastSyncedNetworkUnresolved;

    public HydroTurbineIOBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);

        inletBuffer = new FluidTank(INLET_BUFFER_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        exhaustBuffer = new FluidTank(EXHAUST_BUFFER_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        inletPipeHandler = new InletPipeHandler();
        exhaustPipeHandler = new ExhaustPipeHandler();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide)
            return;
        refreshAdjacentFluidTopology();
        updateSourceWaterCount();
        updateFluidNetworkInfo();
        syncGoggleStateIfChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.DOWN)
            return;
        if (level.isClientSide)
            spawnExhaustSplashParticles();
        else
            flushBufferedWater();
    }

    private void spawnExhaustSplashParticles() {
        if (exhaustBuffer.getFluidAmount() <= 0 || level.random.nextFloat() > 0.75f)
            return;
        BlockPos outputPos = worldPosition.below();
        if (!level.hasChunkAt(outputPos) || !level.getBlockState(outputPos).isAir())
            return;

        for (int i = 0; i < 4; i++) {
            double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.22;
            double y = worldPosition.getY() + 0.02;
            double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.22;
            double speed = 2.0 + level.random.nextDouble() * 2.0;
            double spread = 0.45;
            level.addParticle(STParticleTypes.BLUE_HYDRO_POOF.get(), x, y, z,
                (level.random.nextDouble() - 0.5) * spread,
                -speed,
                (level.random.nextDouble() - 0.5) * spread);
        }
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide)
            return;
        refreshAdjacentFluidTopology();
        updateSourceWaterCount();
        updateFluidNetworkInfo();
        syncGoggleStateIfChanged();
    }

    private void refreshAdjacentFluidTopology() {
        Map<BlockPos, FluidNeighbor> current = new HashMap<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(direction);
            if (!level.hasChunkAt(neighbour))
                continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, neighbour);
            if (pipe == null)
                continue;
            current.put(neighbour, new FluidNeighbor(level.getBlockState(neighbour), level.getBlockEntity(neighbour)));
        }

        if (fluidTopologyInitialized && lastFluidTopologyLevel == level && current.equals(knownFluidNeighbors))
            return;
        knownFluidNeighbors.clear();
        knownFluidNeighbors.putAll(current);
        fluidTopologyInitialized = true;
        lastFluidTopologyLevel = level;

        // Invalidate Create's cached fluid-handler views and force adjacent pipes/pumps to rebuild
        // their endpoint graph after chunk reloads or pipe replacement.
        invalidateCapabilities();
        for (Map.Entry<BlockPos, FluidNeighbor> entry : current.entrySet()) {
            BlockEntity neighbour = entry.getValue().blockEntity();
            if (neighbour instanceof PumpBlockEntity pump)
                pump.updatePressureChange();
            else
                FluidPropagator.propagateChangedPipe(level, entry.getKey(), entry.getValue().state());
        }
        setChanged();
    }

    private void updateSourceWaterCount() {
        int next = getBlockState().getValue(HydroTurbineIOBlock.FACING) == Direction.UP
            ? HydroTurbineChamber.countSourceWaterAbove(level, worldPosition)
            : 0;
        sourceWaterBlocks = next;
    }

    private void updateFluidNetworkInfo() {
        if (level == null)
            return;
        HydroTurbineChamber.FluidNetworkInfo next = HydroTurbineChamber.connectedFluidNetwork(level, worldPosition);
        if (!next.equals(fluidNetworkInfo)) {
            fluidNetworkInfo = next;
            setChanged();
        }
    }

    private void syncGoggleStateIfChanged() {
        if (lastSyncedSourceWater == sourceWaterBlocks
            && lastSyncedInletBuffer == inletBuffer.getFluidAmount()
            && lastSyncedExhaustBuffer == exhaustBuffer.getFluidAmount()
            && lastSyncedNetworkWater == fluidNetworkInfo.waterAmount()
            && lastSyncedNetworkCapacity == fluidNetworkInfo.tankCapacity()
            && lastSyncedNetworkRequirement == fluidNetworkInfo.requiredTankWater()
            && lastSyncedNetworkShared == fluidNetworkInfo.sharedHydroUnit()
            && lastSyncedNetworkUnresolved == fluidNetworkInfo.unresolvedHydroUnit())
            return;
        lastSyncedSourceWater = sourceWaterBlocks;
        lastSyncedInletBuffer = inletBuffer.getFluidAmount();
        lastSyncedExhaustBuffer = exhaustBuffer.getFluidAmount();
        lastSyncedNetworkWater = fluidNetworkInfo.waterAmount();
        lastSyncedNetworkCapacity = fluidNetworkInfo.tankCapacity();
        lastSyncedNetworkRequirement = fluidNetworkInfo.requiredTankWater();
        lastSyncedNetworkShared = fluidNetworkInfo.sharedHydroUnit();
        lastSyncedNetworkUnresolved = fluidNetworkInfo.unresolvedHydroUnit();
        setChanged();
        sendData();
    }

    private int remainingInletTransfer() {
        long gameTime = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (gameTime != inletTransferTick) {
            inletTransferTick = gameTime;
            inletTransferredThisTick = 0;
        }
        return Math.max(0, MAX_FLUID_TRANSFER_PER_TICK - inletTransferredThisTick);
    }

    private int remainingExhaustTransfer() {
        long gameTime = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (gameTime != exhaustTransferTick) {
            exhaustTransferTick = gameTime;
            exhaustTransferredThisTick = 0;
        }
        return Math.max(0, MAX_FLUID_TRANSFER_PER_TICK - exhaustTransferredThisTick);
    }

    private void recordExhaustTransfer(int amount) {
        if (amount <= 0)
            return;
        remainingExhaustTransfer();
        exhaustTransferredThisTick += amount;
    }

    public int getSourceWaterBlocks() {
        return sourceWaterBlocks;
    }

    public int getInletBufferAmount() {
        return inletBuffer.getFluidAmount();
    }

    public int getBufferedWaterAmount() {
        return inletBuffer.getFluidAmount() + exhaustBuffer.getFluidAmount();
    }

    public HydroTurbineChamber.FluidNetworkInfo getFluidNetworkInfo() {
        return fluidNetworkInfo;
    }

    public IFluidHandler getFluidHandler(Direction context) {
        return getBlockState().getValue(HydroTurbineIOBlock.FACING) == Direction.UP
            ? inletPipeHandler : exhaustPipeHandler;
    }

    private IFluidHandler getPoolIntakeHandler() {
        if (level == null || getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.UP)
            return null;

        Direction facing = Direction.UP;
        BlockPos outputPos = worldPosition.relative(facing);
        if (poolIntakePipe == null || !poolIntakePipe.getOutputPos().equals(outputPos))
            poolIntakePipe = new OpenEndedPipe(new BlockFace(worldPosition, facing));
        poolIntakePipe.manageSource(level, this);
        var provider = poolIntakePipe.provideHandler();
        return provider == null ? null : provider.getCapability();
    }

    public FluidStack simulateDrainWater(int amount, boolean tankMode) {
        if (getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.UP || amount <= 0)
            return FluidStack.EMPTY;
        if (tankMode) {
            if (!inletBuffer.isEmpty())
                return inletBuffer.drain(amount, IFluidHandler.FluidAction.SIMULATE);
            return simulateDrainConnectedTanks(amount);
        }

        IFluidHandler handler = getPoolIntakeHandler();
        if (handler == null)
            return FluidStack.EMPTY;
        FluidStack simulated = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        return simulated.getFluid() == Fluids.WATER ? simulated : FluidStack.EMPTY;
    }

    public FluidStack drainWater(int amount, boolean tankMode) {
        FluidStack simulated = simulateDrainWater(amount, tankMode);
        if (simulated.isEmpty())
            return FluidStack.EMPTY;
        if (tankMode && !inletBuffer.isEmpty())
            return inletBuffer.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
        if (tankMode)
            return drainConnectedTanks(simulated.getAmount());

        IFluidHandler handler = getPoolIntakeHandler();
        if (handler == null)
            return FluidStack.EMPTY;
        FluidStack drained = handler.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty())
            setChanged();
        return drained;
    }

    private FluidStack simulateDrainConnectedTanks(int amount) {
        if (level == null || !fluidNetworkInfo.satisfiesTankInput())
            return FluidStack.EMPTY;
        int remaining = Math.min(amount, remainingInletTransfer());
        int total = 0;
        for (BlockPos controller : fluidNetworkInfo.tankControllers()) {
            IFluidHandler handler = getTankHandler(controller);
            if (handler == null)
                continue;
            FluidStack simulated = handler.drain(new FluidStack(Fluids.WATER, remaining - total),
                IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && simulated.getFluid() == Fluids.WATER)
                total += Math.min(remaining - total, simulated.getAmount());
            if (total >= remaining)
                break;
        }
        return total == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, total);
    }

    private FluidStack drainConnectedTanks(int amount) {
        if (level == null || !fluidNetworkInfo.satisfiesTankInput())
            return FluidStack.EMPTY;
        int remaining = Math.min(amount, remainingInletTransfer());
        int total = 0;
        for (BlockPos controller : fluidNetworkInfo.tankControllers()) {
            IFluidHandler handler = getTankHandler(controller);
            if (handler == null)
                continue;
            FluidStack simulated = handler.drain(new FluidStack(Fluids.WATER, remaining - total),
                IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty() || simulated.getFluid() != Fluids.WATER)
                continue;
            FluidStack drained = handler.drain(new FluidStack(Fluids.WATER,
                Math.min(remaining - total, simulated.getAmount())), IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty() && drained.getFluid() == Fluids.WATER)
                total += drained.getAmount();
            if (total >= remaining)
                break;
        }
        if (total <= 0)
            return FluidStack.EMPTY;
        inletTransferredThisTick += total;
        setChanged();
        return new FluidStack(Fluids.WATER, total);
    }

    /** Remove the Hydro Unit's slow maintenance loss from its connected Create tank network. */
    public int discardConnectedTankWater(int amount) {
        if (level == null || amount <= 0)
            return 0;
        int remaining = amount;
        int total = 0;
        for (BlockPos controller : fluidNetworkInfo.tankControllers()) {
            IFluidHandler handler = getTankHandler(controller);
            if (handler == null)
                continue;
            FluidStack simulated = handler.drain(new FluidStack(Fluids.WATER, remaining),
                IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty() || simulated.getFluid() != Fluids.WATER)
                continue;
            FluidStack drained = handler.drain(new FluidStack(Fluids.WATER,
                Math.min(remaining, simulated.getAmount())), IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty() || drained.getFluid() != Fluids.WATER)
                continue;
            total += drained.getAmount();
            remaining -= drained.getAmount();
            if (remaining <= 0)
                break;
        }

        if (remaining > 0) {
            FluidStack buffered = inletBuffer.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
            total += buffered.getAmount();
        }
        if (total > 0) {
            setChanged();
            sendData();
        }
        return total;
    }

    public int discardBufferedExhaustWater(int amount) {
        if (amount <= 0)
            return 0;
        int discarded = exhaustBuffer.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (discarded > 0) {
            setChanged();
            sendData();
        }
        return discarded;
    }

    private IFluidHandler getTankHandler(BlockPos controller) {
        if (!level.hasChunkAt(controller))
            return null;
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, controller, null);
        if (handler != null)
            return handler;
        for (Direction direction : Direction.values()) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, controller, direction);
            if (handler != null)
                return handler;
        }
        return null;
    }

    public int simulateAcceptWater(FluidStack water) {
        if (getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.DOWN)
            return 0;
        return exhaustBuffer.fill(water, IFluidHandler.FluidAction.SIMULATE);
    }

    public int acceptWater(FluidStack water) {
        if (getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.DOWN)
            return 0;
        return exhaustBuffer.fill(water, IFluidHandler.FluidAction.EXECUTE);
    }

    public FluidStack simulateDrainWaterForBoiler(int amount) {
        if (getBlockState().getValue(HydroTurbineIOBlock.FACING) != Direction.DOWN || amount <= 0)
            return FluidStack.EMPTY;
        return exhaustBuffer.drain(Math.min(amount, remainingExhaustTransfer()), IFluidHandler.FluidAction.SIMULATE);
    }

    public FluidStack drainWaterForBoiler(int amount) {
        FluidStack simulated = simulateDrainWaterForBoiler(amount);
        if (simulated.isEmpty())
            return FluidStack.EMPTY;
        FluidStack drained = exhaustBuffer.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            recordExhaustTransfer(drained.getAmount());
            setChanged();
        }
        return drained;
    }

    public void flushBufferedWater() {
        if (level == null || level.isClientSide)
            return;

        long gameTime = level.getGameTime();
        if (lastExternalExhaustDrainTick != Long.MIN_VALUE && gameTime - lastExternalExhaustDrainTick < 20)
            return;

        BlockPos outputPos = worldPosition.below();
        if (!level.hasChunkAt(outputPos))
            return;

        if (level.getBlockState(outputPos).getBlock() instanceof SteamBoilerBlock
            || level.getBlockEntity(outputPos) instanceof SteamBoilerBlockEntity) {
            flushIntoBoiler(outputPos);
            return;
        }

        BlockState outputState = level.getBlockState(outputPos);
        var existingFluid = outputState.getFluidState();
        boolean flowingWater = existingFluid.getType() == Fluids.FLOWING_WATER;
        boolean blocked = existingFluid.isSource()
            || (!existingFluid.isEmpty() && !flowingWater)
            || (!outputState.canBeReplaced() && !flowingWater);

        if (blocked) {
            if (!exhaustBlocked) {
                exhaustBlocked = true;
                nextExhaustOutputTick = gameTime + EXHAUST_SOURCE_INTERVAL;
                setChanged();
                sendData();
            }
            return;
        }

        if (exhaustBlocked) {
            exhaustBlocked = false;
            nextExhaustOutputTick = Math.max(nextExhaustOutputTick, gameTime + EXHAUST_SOURCE_INTERVAL);
            setChanged();
            sendData();
            return;
        }

        if (exhaustBuffer.getFluidAmount() < EXHAUST_SOURCE_AMOUNT || gameTime < nextExhaustOutputTick)
            return;

        if (level.setBlock(outputPos, Fluids.WATER.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL)) {
            exhaustBuffer.drain(EXHAUST_SOURCE_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
            exhaustBlocked = true;
            nextExhaustOutputTick = gameTime + EXHAUST_SOURCE_INTERVAL;
            setChanged();
            sendData();
        }
    }

    private void flushIntoBoiler(BlockPos boilerPos) {
        if (exhaustBuffer.getFluidAmount() <= 0)
            return;
        SteamBoilerBlockEntity boilerBE = level.getBlockEntity(boilerPos) instanceof SteamBoilerBlockEntity found
            ? found : null;
        IFluidHandler boiler = boilerBE != null
            ? boilerBE.getFluidHandler()
            : level.getCapability(Capabilities.FluidHandler.BLOCK, boilerPos, Direction.UP);
        if (boiler == null)
            return;
        int amount = Math.min(remainingExhaustTransfer(), exhaustBuffer.getFluidAmount());
        if (amount <= 0)
            return;
        FluidStack offered = new FluidStack(Fluids.WATER, amount);
        int simulated = boiler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (simulated <= 0)
            return;
        FluidStack executeStack = offered.copy();
        executeStack.setAmount(simulated);
        int accepted = boiler.fill(executeStack, IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            exhaustBuffer.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            recordExhaustTransfer(accepted);
            setChanged();
            if (boilerBE != null)
                boilerBE.syncFluidContentsIfDue();
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        sourceWaterBlocks = tag.getInt("SourceWaterBlocks");
        exhaustBlocked = tag.getBoolean("ExhaustBlocked");
        nextExhaustOutputTick = tag.getLong("NextExhaustOutputTick");
        fluidNetworkInfo = new HydroTurbineChamber.FluidNetworkInfo(
            tag.getInt("TankNetworkCapacity"), tag.getInt("TankNetworkWater"),
            tag.getBoolean("TankNetworkShared"), tag.getBoolean("TankNetworkUnresolved"), List.of(),
            tag.getInt("TankNetworkRequirement"));
        if (tag.contains("InletBuffer"))
            inletBuffer.readFromNBT(registries, tag.getCompound("InletBuffer"));
        if (tag.contains("ExhaustBuffer"))
            exhaustBuffer.readFromNBT(registries, tag.getCompound("ExhaustBuffer"));
        if (tag.contains("OpenEndedPipe"))
            poolIntakePipe = OpenEndedPipe.fromNBT(tag.getCompound("OpenEndedPipe"), registries, worldPosition);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("SourceWaterBlocks", sourceWaterBlocks);
        tag.putBoolean("ExhaustBlocked", exhaustBlocked);
        tag.putLong("NextExhaustOutputTick", nextExhaustOutputTick);
        tag.putInt("TankNetworkCapacity", fluidNetworkInfo.tankCapacity());
        tag.putInt("TankNetworkWater", fluidNetworkInfo.waterAmount());
        tag.putBoolean("TankNetworkShared", fluidNetworkInfo.sharedHydroUnit());
        tag.putBoolean("TankNetworkUnresolved", fluidNetworkInfo.unresolvedHydroUnit());
        tag.putInt("TankNetworkRequirement", fluidNetworkInfo.requiredTankWater());
        CompoundTag inletTag = new CompoundTag();
        inletBuffer.writeToNBT(registries, inletTag);
        tag.put("InletBuffer", inletTag);
        CompoundTag exhaustTag = new CompoundTag();
        exhaustBuffer.writeToNBT(registries, exhaustTag);
        tag.put("ExhaustBuffer", exhaustTag);
        if (poolIntakePipe != null)
            tag.put("OpenEndedPipe", poolIntakePipe.serializeNBT(registries));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean inlet = getBlockState().getValue(HydroTurbineIOBlock.FACING) == Direction.UP;
        String key = inlet ? "block.turbines.hydro_turbine_io.goggles.inlet"
            : "block.turbines.hydro_turbine_io.goggles.exhaust";
        tooltip.add(Component.literal("    ").append(Component.translatable(key)).withStyle(ChatFormatting.GOLD));
        if (inlet) {
            boolean tankMode = fluidNetworkInfo.satisfiesTankInput()
                && (inletBuffer.getFluidAmount() > 0 || !fluidNetworkInfo.tankControllers().isEmpty());
            boolean sourceMode = !tankMode && sourceWaterBlocks > 0;
            boolean tankNetworkPopulated = fluidNetworkInfo.tankCapacity() > 0 || fluidNetworkInfo.waterAmount() > 0
                || fluidNetworkInfo.sharedHydroUnit() || fluidNetworkInfo.unresolvedHydroUnit();
        boolean showTankNetwork = tankMode || (!sourceMode && tankNetworkPopulated);

            if (showTankNetwork) {
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_turbine_io.goggles.network_water", fluidNetworkInfo.waterAmount(),
                        fluidNetworkInfo.requiredTankWater()).withStyle(ChatFormatting.GRAY)));
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_turbine_io.goggles.network_capacity", fluidNetworkInfo.tankCapacity())
                    .withStyle(ChatFormatting.GRAY)));
                if (fluidNetworkInfo.sharedHydroUnit())
                    tooltip.add(Component.literal("    ").append(Component.translatable(
                        "block.turbines.hydro_turbine_io.goggles.shared_network").withStyle(ChatFormatting.RED)));
                else if (fluidNetworkInfo.unresolvedHydroUnit())
                    tooltip.add(Component.literal("    ").append(Component.translatable(
                        "block.turbines.hydro_turbine_io.goggles.network_unresolved").withStyle(ChatFormatting.RED)));
                else if (tankMode)
                    tooltip.add(Component.literal("    ").append(Component.translatable(
                        "block.turbines.hydro_turbine_io.goggles.inlet_mode_network").withStyle(ChatFormatting.AQUA)));
            } else {
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_turbine_io.goggles.source_water", sourceWaterBlocks,
                        fluidNetworkInfo.requiredTankWater() / 1000)
                    .withStyle(sourceMode ? ChatFormatting.AQUA : ChatFormatting.GRAY)));
                if (sourceMode)
                    tooltip.add(Component.literal("    ").append(Component.translatable(
                        "block.turbines.hydro_turbine_io.goggles.inlet_mode_source").withStyle(ChatFormatting.AQUA)));
                else
                    tooltip.add(Component.literal("    ").append(Component.translatable(
                        "block.turbines.hydro_turbine_io.goggles.add_water").withStyle(ChatFormatting.GRAY)));
            }
        } else {
            long wait = Math.max(0, nextExhaustOutputTick - (level == null ? 0 : level.getGameTime()));
            tooltip.add(Component.literal("    ").append(Component.translatable(
                "block.turbines.hydro_turbine_io.goggles.exhaust_buffer", exhaustBuffer.getFluidAmount(),
                    EXHAUST_BUFFER_CAPACITY).withStyle(ChatFormatting.GRAY)));
            if (wait > 0)
                tooltip.add(Component.literal("    ").append(Component.translatable(
                    "block.turbines.hydro_turbine_io.goggles.next_source", (wait + 19) / 20)
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        return true;
    }

    private final class InletPipeHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? inletBuffer.getFluid() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? INLET_BUFFER_CAPACITY : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && stack.getFluid() == Fluids.WATER; }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource) || !fluidNetworkInfo.satisfiesTankInput())
                return 0;
            int amount = Math.min(resource.getAmount(), remainingInletTransfer());
            if (amount <= 0)
                return 0;
            FluidStack limited = resource.copy();
            limited.setAmount(amount);
            int accepted = inletBuffer.fill(limited, action);
            if (action.execute())
                inletTransferredThisTick += accepted;
            return accepted;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    private final class ExhaustPipeHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? exhaustBuffer.getFluid() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? EXHAUST_BUFFER_CAPACITY : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && stack.getFluid() == Fluids.WATER; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER)
                return FluidStack.EMPTY;
            return recordDrain(exhaustBuffer.drain(resource, action), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return recordDrain(exhaustBuffer.drain(maxDrain, action), action);
        }

        private FluidStack recordDrain(FluidStack drained, FluidAction action) {
            if (!drained.isEmpty() && action.execute()) {
                lastExternalExhaustDrainTick = level == null ? Long.MIN_VALUE : level.getGameTime();
                setChanged();
            }
            return drained;
        }
    }

    private record FluidNeighbor(BlockState state, BlockEntity blockEntity) {}
}
