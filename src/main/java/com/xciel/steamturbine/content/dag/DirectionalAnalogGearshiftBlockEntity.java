package com.xciel.steamturbine.content.dag;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DirectionalAnalogGearshiftBlockEntity extends SplitShaftBlockEntity {

    private static final int RPM_PER_LEVEL = 17;
    private static final int ONE_AT_MAX = 1;
    private static final int MAX_RPM = 256;

    private Direction lastSourceFacing;
    private boolean redstoneLocked;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public DirectionalAnalogGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        Direction facing = getBlockState().getValue(DirectionalAnalogGearshiftBlock.FACING);
        boolean isVertical = facing == Direction.UP || facing == Direction.DOWN;
        ValueBoxTransform slot = isVertical ? new DAGDirectionOption.DAGDirectionOptionVertical() : new DAGDirectionOption();

        movementDirection = new ScrollOptionBehaviour<>(
            WindmillBearingBlockEntity.RotationDirection.class,
            Component.translatable("steamturbine.dag.redstone_lock"),
            this,
            slot);
        movementDirection.withCallback(v -> {
            redstoneLocked = movementDirection.getValue() == 1;
            detachKinetics();
            removeSource();
            attachKinetics();
        });
        behaviours.add(movementDirection);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource()) return 0;
        if (face == getSourceFacing()) return 1;

        BlockState state = getBlockState();
        int leftPhysical = state.getValue(DirectionalAnalogGearshiftBlock.LEFT_POWER);
        int rightPhysical = state.getValue(DirectionalAnalogGearshiftBlock.RIGHT_POWER);

        Direction source = getSourceFacing();
        Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);
        boolean invert = source != facing;

        int leftEffective = invert ? rightPhysical : leftPhysical;
        int rightEffective = invert ? leftPhysical : rightPhysical;

        int diff = leftEffective - rightEffective;
        if (diff == 0) return 0;

        int rpm = Math.abs(diff) * RPM_PER_LEVEL;
        if (Math.abs(diff) == 15) rpm += ONE_AT_MAX;

        float modifier = rpm / (float) MAX_RPM;

        if (redstoneLocked && diff < 0)
            return -modifier;

        return modifier;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        Direction currentSource = hasSource() ? getSourceFacing() : null;
        if (currentSource != lastSourceFacing) {
            lastSourceFacing = currentSource;
            if (hasSource()) {
                detachKinetics();
                removeSource();
                attachKinetics();
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        redstoneLocked = tag.getBoolean("RedstoneLocked");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("RedstoneLocked", redstoneLocked);
    }
}