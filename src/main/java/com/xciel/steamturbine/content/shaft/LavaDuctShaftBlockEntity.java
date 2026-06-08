package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.transfer.ILavaDuctTurbineEndpoint;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public class LavaDuctShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private static final int WATER_TANK_CAPACITY = SteamConstants.LAVA_DUCT_WATER_TANK_CAPACITY;
    private static final int WATER_PER_TICK_PER_TURBINE = SteamConstants.LAVA_DUCT_WATER_PER_TICK_PER_TURBINE;
    private static final int MAX_TURBINES = SteamConstants.LAVA_DUCT_MAX_TURBINES;
    private static final int KICKSTART_DURATION = 40;
    private static final float KICKSTART_CAPACITY = 10000000f;

    private final FluidTank waterTank;
    private final IFluidHandler waterHandler;

    private int connectedTurbineCount = 0;
    private float totalGeneratedSU = 0f;
    private int kickstartTicks = 0;
    private boolean wasRunning = false;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    private int clientTurbineCount = 0;
    private float clientGeneratedSU = 0f;

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

        wasRunning = totalGeneratedSU > 0f;

        updateFromConnectedTurbines();

        if (connectedTurbineCount > 0) {
            int waterNeeded = connectedTurbineCount * WATER_PER_TICK_PER_TURBINE;
            int drained = waterTank.drain(waterNeeded, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < waterNeeded) {
                waterTank.drain(drained, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        setChanged();
        sendData();
    }

    private void spawnSteamParticles() {
        if (level == null) return;
        float su = calculateAddedStressCapacity();
        if (su < 100) return;
        Direction facing = getBlockState().getValue(LavaDuctShaftBlock.FACING);
        Direction westFace = facing.getCounterClockWise();
        BlockPos westPos = worldPosition.relative(westFace);
        if (!level.isLoaded(westPos)) return;
        if (!level.isEmptyBlock(westPos)) return;
        float distance;
        if (su < 500) distance = 1.0f;
        else if (su < 1000) distance = 2.0f;
        else if (su < 10000) distance = 3.0f;
        else if (su < 100000) distance = 5.0f;
        else distance = 8.0f;
        if (level.random.nextInt(3) != 0) return;
        Vec3 normal = Vec3.atLowerCornerOf(westFace.getNormal());
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
        BlockState state = getBlockState();
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        Direction walkDir = Direction.DOWN;

        LavaDuctNetworkWalker walker = new LavaDuctNetworkWalker(level);
        List<LavaDuctNetworkWalker.TurbineInfo> turbines = walker.findTurbines(worldPosition, walkDir);

        float totalSU = 0f;
        int count = 0;

        for (LavaDuctNetworkWalker.TurbineInfo info : turbines) {
            if (info.turbine != null) {
                float turbineSU = info.turbine.getGeneratedSU();
                totalSU += turbineSU;
                count++;
            }
        }

        totalGeneratedSU = totalSU;
        connectedTurbineCount = count;

        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (kickstartTicks > 0) {
            float speed = 256f;
            if (movementDirection != null && movementDirection.getValue() == 1) {
                speed = -speed;
            }
            return speed;
        }
        if (totalGeneratedSU <= 0f) return 0f;
        float speed = Math.min(totalGeneratedSU / 1000f * 256f, 256f);
        if (movementDirection != null && movementDirection.getValue() == 1) {
            speed = -speed;
        }
        return speed;
    }

    public boolean canConnect(Direction direction) {
        BlockState state = getBlockState();
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        Direction eastFace = facing.getClockWise();
        return direction == facing || direction == facing.getOpposite() || direction == eastFace;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (kickstartTicks > 0) {
            return KICKSTART_CAPACITY;
        }
        if (totalGeneratedSU <= 0f) return 0f;
        return Math.round(totalGeneratedSU);
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

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    Lava Duct Shaft: ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Connected Turbines: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(clientTurbineCount)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("    Total SU: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f", clientGeneratedSU)).withStyle(ChatFormatting.WHITE)));
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
        } else {
            clientTurbineCount = connectedTurbineCount;
            clientGeneratedSU = totalGeneratedSU;
        }
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
        tag.putBoolean("WasRunning", wasRunning);
        tag.putInt("ClientTurbineCount", clientTurbineCount);
        tag.putFloat("ClientGeneratedSU", clientGeneratedSU);
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(registries, waterTag);
        tag.put("WaterTank", waterTag);
    }
}