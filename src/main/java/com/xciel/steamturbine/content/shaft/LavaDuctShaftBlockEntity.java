package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.xciel.steamturbine.client.sound.BlockLoopingSoundInstance;
import com.xciel.steamturbine.registrate.STSounds;
import com.xciel.steamturbine.steam.SteamConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class LavaDuctShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private static final int WATER_TANK_CAPACITY = SteamConstants.LAVA_DUCT_WATER_TANK_CAPACITY;
    private static final int WATER_PER_TICK_PER_TURBINE = SteamConstants.LAVA_DUCT_WATER_PER_TICK_PER_TURBINE;
    private static final float BASE_RPM_PER_FACE = 5.0f;
    private static final float MAX_RPM = 256.0f;

    private final FluidTank waterTank;
    private final IFluidHandler waterHandler;

    private int connectedTurbineCount;
    private int totalLavaFaces;
    private float totalGeneratedSU;
    private boolean hasWater;

    @OnlyIn(Dist.CLIENT)
    private BlockLoopingSoundInstance soundInstance;

    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public LavaDuctShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        waterTank = new FluidTank(WATER_TANK_CAPACITY, fs -> fs.getFluid() == Fluids.WATER) {
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
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            spawnSteamParticles();
            tickSound();
            return;
        }

        boolean prevHasWater = hasWater;
        hasWater = waterTank.getFluidAmount() > 0 && connectedTurbineCount > 0;
        if (hasWater != prevHasWater) updateGeneratedRotation();

        updateFromConnectedTurbines();

        if (connectedTurbineCount > 0 && waterTank.getFluidAmount() > 0)
            waterTank.drain(connectedTurbineCount * WATER_PER_TICK_PER_TURBINE, IFluidHandler.FluidAction.EXECUTE);

        setChanged();
        sendData();
    }

    @OnlyIn(Dist.CLIENT)
    private void tickSound() {
        if (getGeneratedSpeed() == 0) {
            if (soundInstance != null) {
                soundInstance.stopSound();
                soundInstance = null;
            }
            return;
        }
        float cap = calculateAddedStressCapacity();
        if (cap < 50) {
            if (soundInstance != null) {
                soundInstance.stopSound();
                soundInstance = null;
            }
            return;
        }
        if (soundInstance == null || soundInstance.isStopped()) {
            soundInstance = new BlockLoopingSoundInstance(STSounds.LAVA_DUCT_SHAFT.get(), worldPosition, 0.225f);
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        }
        soundInstance.keepAlive();
    }

    private void spawnSteamParticles() {
        if (level == null) return;
        if (getGeneratedSpeed() == 0) return;
        float cap = calculateAddedStressCapacity();
        if (cap < 50) return;
        Direction facing = getBlockState().getValue(LavaDuctShaftBlock.FACING);
        Direction exhaustFace = facing.getCounterClockWise();
        BlockPos exhaustPos = worldPosition.relative(exhaustFace);
        if (!level.isLoaded(exhaustPos) || !level.isEmptyBlock(exhaustPos)) return;
        if (level.random.nextInt(3) != 0) return;
        float distance;
        if (cap < 500) distance = 1.0f;
        else if (cap < 1000) distance = 2.0f;
        else if (cap < 10000) distance = 3.0f;
        else if (cap < 100000) distance = 5.0f;
        else distance = 8.0f;
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
        LavaDuctNetworkWalker walker = new LavaDuctNetworkWalker(level);
        List<LavaDuctNetworkWalker.TurbineInfo> turbines = walker.findTurbines(worldPosition, Direction.DOWN);

        float totalSU = 0f;
        int totalFaces = 0;
        int count = 0;

        for (LavaDuctNetworkWalker.TurbineInfo info : turbines) {
            if (info.turbine != null) {
                totalSU += info.turbine.getGeneratedSU();
                totalFaces += info.turbine.getLavaFaceCount();
                count++;
            }
        }

        totalGeneratedSU = totalSU;
        totalLavaFaces = totalFaces;
        connectedTurbineCount = count;

        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (connectedTurbineCount <= 0 || totalLavaFaces <= 0 || !hasWater) return 0f;

        float speed = Math.min(totalLavaFaces * BASE_RPM_PER_FACE, MAX_RPM);
        return movementDirection != null && movementDirection.getValue() == 1 ? -speed : speed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float speed = Math.abs(totalLavaFaces * BASE_RPM_PER_FACE);
        if (speed <= 0f) {
            this.lastCapacityProvided = 0f;
            return 0f;
        }

        this.lastCapacityProvided = Math.round(totalGeneratedSU / speed);
        return this.lastCapacityProvided;
    }

    public boolean canConnect(Direction direction) {
        Direction facing = getBlockState().getValue(LavaDuctShaftBlock.FACING);
        return direction == facing.getClockWise();
    }

    public void onNeighborChanged() {
        updateFromConnectedTurbines();
    }

    public IFluidHandler getFluidHandler() {
        return waterHandler;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    Lava Duct Shaft: ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Turbines Connected: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(connectedTurbineCount)).withStyle(ChatFormatting.WHITE)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        connectedTurbineCount = tag.getInt("ConnectedTurbineCount");
        totalLavaFaces = tag.getInt("TotalLavaFaces");
        totalGeneratedSU = tag.getFloat("TotalGeneratedSU");
        hasWater = tag.getBoolean("HasWater");
        if (tag.contains("WaterTank"))
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
        tag.putInt("TotalLavaFaces", totalLavaFaces);
        tag.putFloat("TotalGeneratedSU", totalGeneratedSU);
        tag.putBoolean("HasWater", hasWater);
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(registries, waterTag);
        tag.put("WaterTank", waterTag);
    }
}
