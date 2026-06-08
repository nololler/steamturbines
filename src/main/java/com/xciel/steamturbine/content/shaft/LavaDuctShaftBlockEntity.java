package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.xciel.steamturbine.steam.SteamConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public class LavaDuctShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private static final int WATER_TANK_CAPACITY = SteamConstants.LAVA_DUCT_WATER_TANK_CAPACITY;
    private static final int WATER_PER_TICK_PER_TURBINE = SteamConstants.LAVA_DUCT_WATER_PER_TICK_PER_TURBINE;
    private static final int KICKSTART_DURATION = 40;
    private static final float KICKSTART_CAPACITY = 10000000f;

    private static final float SU_PER_FACE = SteamConstants.LAVA_DUCT_SU_PER_FACE;
    private static final float BASE_RPM_PER_FACE = 5.0f;
    private static final float MAX_RPM = 256.0f;

    private final FluidTank waterTank;
    private final IFluidHandler waterHandler;

    private int connectedTurbineCount = 0;
    private int totalLavaFaces = 0;
    private float totalGeneratedSU = 0f;
    private float rpm = 0f;
    private int kickstartTicks = 0;
    private boolean wasRunning = false;
    private boolean hasWater = false;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    private int clientTurbineCount = 0;
    private float clientGeneratedSU = 0f;
    private int clientLavaFaces = 0;
    private boolean clientHasWater = false;

    public LavaDuctShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> fluidStack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        waterHandler = waterTank;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        movementDirection = new ScrollOptionBehaviour<>(
            WindmillBearingBlockEntity.RotationDirection.class,
            Component.translatable("create.contraptions.windmill.rotation_direction"),
            this,
            new LavaDuctShaftDirectionOption());
        movementDirection.withCallback(v -> updateGeneratedRotation());
        behaviours.add(movementDirection);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        if (wasRunning) {
            kickstartTicks = KICKSTART_DURATION;
        }
        updateFromConnectedTurbines();
    }

    private boolean hasWaterInTank() {
        return waterTank.getFluidAmount() > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            spawnSteamParticles();
            return;
        }

        if (kickstartTicks > 0) {
            kickstartTicks--;
        }

        boolean prevHasWater = hasWater;
        hasWater = hasWaterInTank() && connectedTurbineCount > 0;
        if (hasWater != prevHasWater) {
            updateGeneratedRotation();
        }

        wasRunning = canGeneratePower();

        updateFromConnectedTurbines();

        if (connectedTurbineCount > 0 && hasWaterInTank()) {
            int waterNeeded = connectedTurbineCount * WATER_PER_TICK_PER_TURBINE;
            waterTank.drain(waterNeeded, IFluidHandler.FluidAction.EXECUTE);
        }

        setChanged();
        sendData();
    }

    private void spawnSteamParticles() {
        if (level == null) return;
        if (!canGeneratePower()) return;
        float su = calculateAddedStressCapacity();
        if (su < 100) return;
        Direction facing = getBlockState().getValue(LavaDuctShaftBlock.FACING);
        Direction exhaustFace = facing.getCounterClockWise();
        BlockPos exhaustPos = worldPosition.relative(exhaustFace);
        if (!level.isLoaded(exhaustPos)) return;
        if (!level.isEmptyBlock(exhaustPos)) return;
        float distance;
        if (su < 500) distance = 1.0f;
        else if (su < 1000) distance = 2.0f;
        else if (su < 10000) distance = 3.0f;
        else if (su < 100000) distance = 5.0f;
        else distance = 8.0f;
        if (level.random.nextInt(3) != 0) return;
        Vec3 normal = Vec3.atLowerCornerOf(exhaustFace.getNormal());
        Vec3 offset = normal.scale(0.5).add(0.5, 0.5, 0.5);
        double speed = distance * 0.05;
        level.addParticle(ParticleTypes.CLOUD,
            worldPosition.getX() + offset.x + (level.random.nextDouble() - 0.5) * 0.1,
            worldPosition.getY() + offset.y + (level.random.nextDouble() - 0.5) * 0.1,
            worldPosition.getZ() + offset.z + (level.random.nextDouble() - 0.5) * 0.1,
            normal.x * speed, normal.y * speed * 0.3, normal.z * speed);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;
        updateFromConnectedTurbines();
    }

    private void updateFromConnectedTurbines() {
        Direction walkDir = Direction.DOWN;

        LavaDuctNetworkWalker walker = new LavaDuctNetworkWalker(level);
        List<LavaDuctNetworkWalker.TurbineInfo> turbines = walker.findTurbines(worldPosition, walkDir);

        float totalSU = 0f;
        int totalFaces = 0;
        int count = 0;

        for (LavaDuctNetworkWalker.TurbineInfo info : turbines) {
            if (info.turbine != null) {
                float turbineSU = info.turbine.getGeneratedSU();
                int turbineFaces = info.turbine.getLavaFaceCount();
                totalSU += turbineSU;
                totalFaces += turbineFaces;
                count++;
            }
        }

        totalGeneratedSU = totalSU;
        totalLavaFaces = totalFaces;
        connectedTurbineCount = count;

        clientTurbineCount = count;
        clientGeneratedSU = totalSU;
        clientLavaFaces = totalFaces;
        clientHasWater = hasWaterInTank() && count > 0;

        updateGeneratedRotation();
    }

    private boolean canGeneratePower() {
        return hasWater && connectedTurbineCount > 0 && totalLavaFaces > 0;
    }

    @Override
    public float getGeneratedSpeed() {
        if (kickstartTicks > 0) {
            float speed = MAX_RPM;
            if (movementDirection != null && movementDirection.getValue() == 1) {
                speed = -speed;
            }
            return speed;
        }
        if (!canGeneratePower()) return 0f;
        rpm = Math.min(totalLavaFaces * BASE_RPM_PER_FACE, MAX_RPM);
        if (movementDirection != null && movementDirection.getValue() == 1) {
            rpm = -rpm;
        }
        return rpm;
    }

    public boolean canConnect(Direction direction) {
        BlockState state = getBlockState();
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        Direction lavaFace = facing.getClockWise();
        return direction == lavaFace;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (kickstartTicks > 0) {
            return KICKSTART_CAPACITY;
        }
        if (!canGeneratePower()) return 0f;
        float speed = Math.abs(rpm);
        if (speed <= 0f) return 0f;
        float baseCapacity = totalGeneratedSU / speed;
        return Math.round(baseCapacity);
    }

    public void onNeighborChanged() {
        updateFromConnectedTurbines();
    }

    public IFluidHandler getFluidHandler() {
        return waterHandler;
    }

    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public int getConnectedTurbineCount() {
        return connectedTurbineCount;
    }

    public float getTotalGeneratedSU() {
        return totalGeneratedSU;
    }

    public int getTotalLavaFaces() {
        return totalLavaFaces;
    }

    public boolean hasWater() {
        return hasWater;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    Lava Duct Shaft: ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Connected Turbines: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(clientTurbineCount)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("    Lava Faces: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(clientLavaFaces)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("    Total SU/tick: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", clientGeneratedSU / 20f)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("    Has Water: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(clientHasWater ? "Yes" : "No").withStyle(clientHasWater ? ChatFormatting.GREEN : ChatFormatting.RED)));
        tooltip.add(Component.literal("    Water: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(waterTank.getFluidAmount() + " / " + WATER_TANK_CAPACITY + "mb").withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        connectedTurbineCount = tag.getInt("ConnectedTurbineCount");
        wasRunning = tag.getBoolean("WasRunning");
        if (clientPacket) {
            clientTurbineCount = tag.getInt("ClientTurbineCount");
            clientGeneratedSU = tag.getFloat("ClientGeneratedSU");
            clientLavaFaces = tag.getInt("ClientLavaFaces");
            clientHasWater = tag.getBoolean("ClientHasWater");
        } else {
            clientTurbineCount = connectedTurbineCount;
            clientGeneratedSU = totalGeneratedSU;
            clientLavaFaces = totalLavaFaces;
            clientHasWater = hasWater;
        }
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
        tag.putBoolean("WasRunning", wasRunning);
        tag.putInt("ClientTurbineCount", clientTurbineCount);
        tag.putFloat("ClientGeneratedSU", clientGeneratedSU);
        tag.putInt("ClientLavaFaces", clientLavaFaces);
        tag.putBoolean("ClientHasWater", clientHasWater);
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(registries, waterTag);
        tag.put("WaterTank", waterTag);
    }
}