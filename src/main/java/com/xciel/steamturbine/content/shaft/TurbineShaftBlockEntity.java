package com.xciel.steamturbine.content.shaft;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
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

import java.util.List;

public class TurbineShaftBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation, ISteamEndpoint {
    private static final float BASE_STRESS_CAPACITY = 390.0f;
    private static final float KINETIC_STRESS_CAPACITY = 256.0f;
    private static final float KICKSTART_CAPACITY = 10000000f;
    private static final int KICKSTART_DURATION = 40;

    private float aggregatedSpeed = 0f;
    private float aggregatedThroughput = 0f;
    private int connectedTurbineCount = 0;
    private int kickstartTicks = 0;
    private boolean wasRunning = false;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public TurbineShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        movementDirection = new ScrollOptionBehaviour<>(
            WindmillBearingBlockEntity.RotationDirection.class,
            Component.translatable("create.contraptions.windmill.rotation_direction"),
            this,
            new TurbineShaftDirectionOption());
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

        wasRunning = aggregatedSpeed > 0f;

        updateFromConnectedTurbines();
    }

    private void spawnSteamParticles() {
        if (level == null) return;
        float su = calculateAddedStressCapacity();
        if (su < 100) return;
        Direction facing = getBlockState().getValue(TurbineShaftBlock.FACING);
        Direction eastFace = facing.getClockWise();
        BlockPos eastPos = worldPosition.relative(eastFace);
        if (!level.isLoaded(eastPos)) return;
        if (!level.isEmptyBlock(eastPos)) return;
        float distance;
        if (su < 500) distance = 1.0f;
        else if (su < 1000) distance = 2.0f;
        else if (su < 10000) distance = 3.0f;
        else if (su < 100000) distance = 5.0f;
        else distance = 8.0f;
        if (level.random.nextInt(3) != 0) return;
        Vec3 normal = Vec3.atLowerCornerOf(eastFace.getNormal());
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
        Direction facing = state.getValue(TurbineShaftBlock.FACING);

        Direction walkDir = facing.getOpposite();

        // Use the network walker to find all turbines through pipes
        TurbineNetworkWalker walker = new TurbineNetworkWalker(level);
        List<TurbineNetworkWalker.TurbineInfo> turbines = walker.findTurbines(worldPosition, walkDir);

        float totalSpeed = 0f;
        float maxSpeed = 0f;
        float totalThroughput = 0f;
        int count = 0;

        for (TurbineNetworkWalker.TurbineInfo info : turbines) {
            float exhaustThroughput = info.turbine.getExhaustThroughput();
            if (exhaustThroughput <= 0) continue;

            float turbineSpeed = Math.abs(info.turbine.getTurbineSpeed());
            totalSpeed += turbineSpeed;
            if (turbineSpeed > maxSpeed) maxSpeed = turbineSpeed;
            totalThroughput += exhaustThroughput;
            count++;
        }

        aggregatedSpeed = totalSpeed;
        aggregatedThroughput = totalThroughput;
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
        float speed = Math.min(aggregatedSpeed, 256f);
        if (movementDirection != null && movementDirection.getValue() == 1) {
            speed = -speed;
        }
        return speed;
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
        if (kickstartTicks > 0) {
            return KICKSTART_CAPACITY;
        }
        if (aggregatedSpeed <= 0f) return 0f;

        float baseMultiplier = 0.095f;
        float stageBonus = 0.98f + (connectedTurbineCount * 0.022f);
        float calculatedStressCapacity = aggregatedSpeed * aggregatedThroughput * baseMultiplier * stageBonus;
        return Math.round(calculatedStressCapacity);
    }

    public void onNeighborChanged() {
        updateFromConnectedTurbines();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    Turbine Shaft: ").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Connected Turbines: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(connectedTurbineCount)).withStyle(ChatFormatting.WHITE)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        connectedTurbineCount = tag.getInt("ConnectedTurbineCount");
        wasRunning = tag.getBoolean("WasRunning");
        if (clientPacket) {
            aggregatedSpeed = tag.getFloat("AggregatedSpeed");
            aggregatedThroughput = tag.contains("AggregatedThroughput") ? tag.getFloat("AggregatedThroughput") : 0f;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("ConnectedTurbineCount", connectedTurbineCount);
        tag.putBoolean("WasRunning", wasRunning);
        if (clientPacket) {
            tag.putFloat("AggregatedSpeed", aggregatedSpeed);
            tag.putFloat("AggregatedThroughput", aggregatedThroughput);
        }
    }
}
