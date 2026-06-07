package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity;
import com.xciel.steamturbine.steam.SteamConstants;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BoilerTurbineBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public WeakReference<PoweredShaftBlockEntity> target;
    public WeakReference<SteamBoilerBlockEntity> source;

    float prevAngle = 0;

    private boolean hasShaftCasing = false;
    private boolean shaftCasingRemovedPermanently = false;

    public BoilerTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        source = new WeakReference<>(null);
        target = new WeakReference<>(null);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        SteamBoilerBlockEntity boiler = getBoiler();
        PoweredShaftBlockEntity shaft = getShaft();

        if (boiler == null || shaft == null || !isValid()) {
            if (level.isClientSide)
                return;
            if (shaft == null)
                return;
            if (!shaft.getBlockPos().subtract(worldPosition).equals(shaft.enginePos))
                return;
            if (shaft.engineEfficiency == 0)
                return;
            Direction facing = BoilerTurbineBlock.getFacing(getBlockState());
            if (level.isLoaded(worldPosition.relative(facing.getOpposite())))
                shaft.update(worldPosition, 0, 0);
            return;
        }

        boolean verticalTarget = false;
        BlockState shaftState = shaft.getBlockState();
        Axis targetAxis = Axis.X;
        if (shaftState.getBlock() instanceof IRotate ir)
            targetAxis = ir.getRotationAxis(shaftState);
        verticalTarget = targetAxis == Axis.Y;

        BlockState blockState = getBlockState();
        if (!AllBlocks.STEAM_ENGINE.has(blockState))
            return;
        Direction facing = BoilerTurbineBlock.getFacing(blockState);
        if (facing.getAxis() == Axis.Y)
            facing = blockState.getValue(BoilerTurbineBlock.FACING);

        float steamPressure = getSteamPressure(boiler);
        float efficiency = Mth.clamp(steamPressure / SteamConstants.MAX_PRESSURE, 0, 1);

        int conveyedSpeedLevel =
            efficiency == 0 ? 1 : verticalTarget ? 1 : (int) com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity.convertToDirection(1, facing);
        if (targetAxis == Axis.Z)
            conveyedSpeedLevel *= -1;

        float shaftSpeed = shaft.getTheoreticalSpeed();
        if (shaft.hasSource() && shaftSpeed != 0 && conveyedSpeedLevel != 0
            && (shaftSpeed > 0) != (conveyedSpeedLevel > 0)) {
            conveyedSpeedLevel *= -1;
        }

        shaft.update(worldPosition, conveyedSpeedLevel, efficiency);

        if (!level.isClientSide)
            return;

        CatnipServices.PLATFORM.executeOnClientOnly(() -> this::spawnParticles);
    }

    private float getSteamPressure(SteamBoilerBlockEntity boiler) {
        if (boiler == null) return 0f;
        return boiler.getPressure();
    }

    public float getShaftSpeed() {
        PoweredShaftBlockEntity shaft = getShaft();
        if (shaft != null) {
            return shaft.getSpeed();
        }
        return 0f;
    }

    @Override
    public void remove() {
        PoweredShaftBlockEntity shaft = getShaft();
        if (shaft != null)
            shaft.remove(worldPosition);
        super.remove();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        hasShaftCasing = tag.getBoolean("HasShaftCasing");
        shaftCasingRemovedPermanently = tag.getBoolean("ShaftCasingRemovedPermanently");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("HasShaftCasing", hasShaftCasing);
        tag.putBoolean("ShaftCasingRemovedPermanently", shaftCasingRemovedPermanently);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }

    public PoweredShaftBlockEntity getShaft() {
        PoweredShaftBlockEntity shaft = target.get();
        if (shaft == null || shaft.isRemoved() || !shaft.canBePoweredBy(worldPosition)) {
            if (shaft != null)
                target = new WeakReference<>(null);
            BlockPos shaftPos = BoilerTurbineBlock.getShaftPos(getBlockState(), worldPosition);
            BlockEntity anyShaftAt = level.getBlockEntity(shaftPos);
            if (anyShaftAt instanceof PoweredShaftBlockEntity ps && ps.canBePoweredBy(worldPosition))
                target = new WeakReference<>(shaft = ps);
        }
        return shaft;
    }

    public SteamBoilerBlockEntity getBoiler() {
        SteamBoilerBlockEntity boiler = source.get();
        if (boiler == null || boiler.isRemoved()) {
            if (boiler != null)
                source = new WeakReference<>(null);
            Direction facing = BoilerTurbineBlock.getFacing(getBlockState());
            BlockEntity be = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
            if (be instanceof SteamBoilerBlockEntity boilerBe)
                source = new WeakReference<>(boiler = boilerBe);
        }
        if (boiler == null)
            return null;
        return boiler.getControllerBE();
    }

    public boolean isValid() {
        Direction dir = BoilerTurbineBlock.getConnectedDirection(getBlockState()).getOpposite();

        Level level = getLevel();
        if (level == null)
            return false;

        return level.getBlockState(getBlockPos().relative(dir)).is(com.xciel.steamturbine.AllBlocks.STEAM_BOILER.get());
    }

    public void onShaftPlaced() {
        if (shaftCasingRemovedPermanently) {
            return;
        }
        if (!hasShaftCasing) {
            hasShaftCasing = true;
        }
    }

    public void onCasingSpawned() {
        hasShaftCasing = true;
    }

    public void onShaftRemovedFromCasing() {
        hasShaftCasing = false;
        shaftCasingRemovedPermanently = true;
    }

    public boolean shouldRespawnCasing() {
        return !hasShaftCasing && shaftCasingRemovedPermanently;
    }

    public void markCasingRespawned() {
        hasShaftCasing = true;
        shaftCasingRemovedPermanently = false;
    }

    public boolean isShaftCasingRemovedPermanently() {
        return shaftCasingRemovedPermanently;
    }

    @OnlyIn(Dist.CLIENT)
    private void spawnParticles() {
        Float targetAngle = getTargetAngle();
        PoweredShaftBlockEntity ste = target.get();
        if (ste == null)
            return;
        if (!ste.isPoweredBy(worldPosition) || ste.engineEfficiency == 0)
            return;
        if (targetAngle == null)
            return;

        float angle = AngleHelper.deg(targetAngle);
        angle += (angle < 0) ? -180 + 75 : 360 - 75;
        angle %= 360;

        PoweredShaftBlockEntity shaft = getShaft();
        if (shaft == null || shaft.getSpeed() == 0)
            return;

        if (angle >= 0 && !(prevAngle > 180 && angle < 180)) {
            prevAngle = angle;
            return;
        }
        if (angle < 0 && !(prevAngle < -180 && angle > -180)) {
            prevAngle = angle;
            return;
        }

        Direction facing = BoilerTurbineBlock.getFacing(getBlockState());
        float pistonPhase = (float) (Math.sin(Math.toRadians(angle)) * 0.5 + 0.5);

        level.addParticle(
            ParticleTypes.LARGE_SMOKE,
            worldPosition.getX() + 0.5 + facing.getStepX() * 0.3,
            worldPosition.getY() + 0.5,
            worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.3,
            0, pistonPhase * 0.1, 0
        );

        prevAngle = angle;
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Float getTargetAngle() {
        float angle = 0;
        BlockState blockState = getBlockState();
        if (!AllBlocks.STEAM_ENGINE.has(blockState))
            return null;

        Direction facing = BoilerTurbineBlock.getFacing(blockState);
        PoweredShaftBlockEntity shaft = getShaft();
        Axis facingAxis = facing.getAxis();
        Axis axis = Axis.Y;

        if (shaft == null)
            return null;

        axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
        angle = KineticBlockEntityRenderer.getAngleForBe(shaft, shaft.getBlockPos(), axis);

        if (axis == facingAxis)
            return null;
        if (axis.isHorizontal() && (facingAxis == Axis.X ^ facing.getAxisDirection() == AxisDirection.POSITIVE))
            angle *= -1;
        if (axis == Axis.X && facing == Direction.DOWN)
            angle *= -1;
        return angle;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Boiler Turbine").withStyle(ChatFormatting.GOLD));

        SteamBoilerBlockEntity boiler = getBoiler();
        if (boiler != null) {
            tooltip.add(Component.literal("    Pressure: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.2f", getSteamPressure(boiler))).withStyle(ChatFormatting.WHITE)));
        }

        PoweredShaftBlockEntity shaft = getShaft();
        if (shaft != null && shaft.hasSource()) {
            tooltip.add(Component.literal("    Shaft Speed: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.0f RPM", shaft.getSpeed())).withStyle(ChatFormatting.WHITE)));
        }

        tooltip.add(Component.literal("    Efficiency: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.0f%%", (shaft != null ? shaft.engineEfficiency : 0) * 100)).withStyle(ChatFormatting.WHITE)));

        return true;
    }

    public void onNeighborChanged() {
        setChanged();
    }
}