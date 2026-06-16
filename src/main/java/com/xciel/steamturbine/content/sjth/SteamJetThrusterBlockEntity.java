package com.xciel.steamturbine.content.sjth;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.transfer.IPressurizedConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

import java.util.List;

public class SteamJetThrusterBlockEntity extends SmartBlockEntity implements
    IPressurizedConsumer, ISteamEndpoint, IHaveGoggleInformation, BlockEntitySubLevelActor {

    public static float PUSH_STRENGTH = 2.2f;

    private static final float MAX_THROUGHPUT = 50.0f;
    private static final float MAX_PUSH_RANGE = 5.0f;
    private static final float MAX_THRUST = 800.0f;

    private SteamData inputSteam = SteamData.empty();
    private SteamData lastInputSteam = SteamData.empty();
    private double currentThrust = 0d;

    public SteamJetThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (!level.isClientSide) {
            processThrust();
        }

        if (isActive()) {
            if (!level.isClientSide) {
                pushEntities();
            } else {
                spawnExhaustParticles();
            }
        }
    }

    private void processThrust() {
        if (inputSteam.isEmpty() || !inputSteam.shouldPropagate()) {
            currentThrust = 0;
            lastInputSteam = SteamData.empty();
            setChanged();
            sendData();
            return;
        }

        lastInputSteam = inputSteam.withThroughput(Math.min(inputSteam.getThroughput(), MAX_THROUGHPUT));

        float pressure = inputSteam.getPressure();
        float throughput = Math.min(inputSteam.getThroughput(), MAX_THROUGHPUT);

        currentThrust = Math.min(PUSH_STRENGTH * pressure * throughput, MAX_THRUST);

        inputSteam = SteamData.empty();
        setChanged();
        sendData();
    }

    public boolean isActive() {
        return currentThrust > 0.1f;
    }

    public double getCurrentThrust() {
        return currentThrust;
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        double thrust = getCurrentThrust();
        if (thrust <= 0) return;

        Direction facing = getFacing();
        Direction opposite = facing.getOpposite();

        Vector3d position = new Vector3d(
            worldPosition.getX() + 0.5,
            worldPosition.getY() + 0.5,
            worldPosition.getZ() + 0.5
        ).fma(0.55, new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ()));

        Vector3d force = new Vector3d(
            opposite.getStepX(),
            opposite.getStepY(),
            opposite.getStepZ()
        ).mul(thrust * timeStep);

        QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        forceGroup.applyAndRecordPointForce(position, force);
    }

    private void pushEntities() {
        Direction facing = getFacing();
        Vec3 thrustVec = Vec3.atLowerCornerOf(facing.getNormal()).scale(currentThrust * 0.02);

        float range = Math.min((float) (currentThrust / MAX_THRUST * MAX_PUSH_RANGE), MAX_PUSH_RANGE);
        Vec3 blockCenter = Vec3.atCenterOf(worldPosition);
        Vec3 min = blockCenter.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(-1));
        Vec3 max = blockCenter.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(range));
        AABB aabb = new AABB(min, max).inflate(1.5);

        List<Entity> entities = level.getEntities(null, aabb);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof AbstractMinecart)) {
                continue;
            }

            Vec3 entityCenter = entity.getBoundingBox().getCenter();
            Vec3 relative = entityCenter.subtract(blockCenter);

            float dot = (float) relative.dot(Vec3.atLowerCornerOf(facing.getNormal()));
            if (dot < 0) continue;

            float distanceFactor = 1.0f - (dot / range);
            if (distanceFactor < 0) continue;
            distanceFactor = Math.min(distanceFactor * distanceFactor, 1.0f);

            Vec3 push = thrustVec.scale(distanceFactor);
            entity.push(push.x, push.y, push.z);
            entity.fallDistance = 0;
        }
    }

    private void spawnExhaustParticles() {
        if (level == null || !level.isClientSide) return;

        Direction facing = getFacing();
        Vec3 nozzlePos = Vec3.atCenterOf(worldPosition)
            .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.1));

        for (int i = 0; i < 3; i++) {
            double spread = 0.3;
            double speed = currentThrust * 0.002;
            double vx = facing.getStepX() * speed + (level.random.nextDouble() - 0.5) * spread;
            double vy = facing.getStepY() * speed + (level.random.nextDouble() - 0.5) * spread;
            double vz = facing.getStepZ() * speed + (level.random.nextDouble() - 0.5) * spread;

            level.addParticle(ParticleTypes.POOF,
                nozzlePos.x + (level.random.nextDouble() - 0.5) * 0.5,
                nozzlePos.y + (level.random.nextDouble() - 0.5) * 0.5,
                nozzlePos.z + (level.random.nextDouble() - 0.5) * 0.5,
                vx, vy, vz
            );
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Steam Jet Thruster").withStyle(ChatFormatting.GOLD));
        if (isActive()) {
            tooltip.add(Component.literal("    Thrust: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f N", currentThrust)).withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("    Input: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f @ %.2f/t", lastInputSteam.getPressure(), lastInputSteam.getThroughput())).withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            tooltip.add(Component.literal("    Idle").withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        if (direction != getFacing().getOpposite()) return;
        if (inputSteam.isEmpty()) {
            inputSteam = steam;
        } else {
            inputSteam = inputSteam.withPressureAndThroughputAdded(steam.getPressure(), steam.getThroughput());
        }
        setChanged();
    }

    @Override
    public boolean canReceive(Direction direction) {
        return direction == getFacing().getOpposite();
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return MAX_THROUGHPUT;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return direction == getFacing().getOpposite();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        currentThrust = tag.getFloat("CurrentThrust");
        if (tag.contains("InputSteam")) {
            inputSteam = SteamData.loadFromNBT(tag.getCompound("InputSteam"), registries);
        }
        if (clientPacket && tag.contains("LastInputSteam")) {
            lastInputSteam = SteamData.loadFromNBT(tag.getCompound("LastInputSteam"), registries);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("CurrentThrust", (float) currentThrust);
        CompoundTag inputTag = new CompoundTag();
        inputSteam.saveToNBT(inputTag, registries);
        tag.put("InputSteam", inputTag);
        if (clientPacket) {
            CompoundTag lastInputTag = new CompoundTag();
            lastInputSteam.saveToNBT(lastInputTag, registries);
            tag.put("LastInputSteam", lastInputTag);
        }
    }
}
