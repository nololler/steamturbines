package com.xciel.turbines.content.large_turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineChamber;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Passive kinetic follower whose rotor is slowed according to its position in the Hydro chamber. */
public class LargeTurbineBlockEntity extends GeneratingKineticBlockEntity {

    private float rotorSpeedMultiplier = 1f;
    private boolean waterParticlesVisible;

    public LargeTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide || !waterParticlesVisible || Math.abs(getSpeed()) < 1
            || level.random.nextFloat() > 0.5f)
            return;

        float spinFactor = getRotorSpeedMultiplier();
        double spinAngle = level.getGameTime() * Math.abs(getSpeed()) * spinFactor * 0.0052359877
            + (worldPosition.asLong() & 0xFFFF) * 0.0001;
        double swirlSpeed = 0.025 * spinFactor;
        for (int i = 0; i < 3; i++) {
            double angle = spinAngle + i * Math.PI * 2 / 3 + (level.random.nextDouble() - 0.5) * 0.16;
            double radius = 0.8 + level.random.nextDouble() * 0.16;
            double x = worldPosition.getX() + 0.5 + Math.cos(angle) * radius;
            double z = worldPosition.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = worldPosition.getY() + 0.1 + level.random.nextDouble() * 0.8;
            double vx = -Math.sin(angle) * swirlSpeed;
            double vz = Math.cos(angle) * swirlSpeed;
            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, vx,
                -0.08 - level.random.nextDouble() * 0.06, vz);
            if (i == 1)
                level.addParticle(ParticleTypes.POOF, x, y, z, vx * 0.7, -0.03, vz * 0.7);
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = 0;
        return 0;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide)
            return;
        HydroTurbineChamber.StageVisualInfo visual = HydroTurbineChamber.stageVisualInfo(level, worldPosition);
        float next = visual.rotorSpeedMultiplier();
        boolean nextWaterVisible = visual.transparentWalls();
        if (Math.abs(next - rotorSpeedMultiplier) < 0.0001f && waterParticlesVisible == nextWaterVisible)
            return;
        rotorSpeedMultiplier = next;
        waterParticlesVisible = nextWaterVisible;
        setChanged();
        sendData();
    }

    public float getRotorSpeedMultiplier() {
        return rotorSpeedMultiplier;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        rotorSpeedMultiplier = tag.getFloat("RotorSpeedMultiplier");
        waterParticlesVisible = tag.getBoolean("WaterParticlesVisible");
        if (rotorSpeedMultiplier <= 0)
            rotorSpeedMultiplier = 1f;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("RotorSpeedMultiplier", rotorSpeedMultiplier);
        tag.putBoolean("WaterParticlesVisible", waterParticlesVisible);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }
}
