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

    private boolean redstoneLocked;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    private float lastInputSpeed;

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

        int diff = leftPhysical - rightPhysical;
        if (diff == 0) return 0;

        int rpm = Math.abs(diff) * RPM_PER_LEVEL;
        if (Math.abs(diff) == 15) rpm += ONE_AT_MAX;

        float modifier = rpm / (float) MAX_RPM;

        // Get input direction
        float inputSpeed = getSpeed();
        boolean inputIsCW = inputSpeed > 0;
        boolean inputIsCCW = inputSpeed < 0;

        if (redstoneLocked) {
            // Mode B: Determine ABSOLUTE output direction
            // LEFT dominant → CW output (absolute)
            // RIGHT dominant → CCW output (absolute)
            if (diff > 0) {
                // LEFT dominant → want CW output
                if (inputIsCW) return modifier;      // Input already CW, follow along
                if (inputIsCCW) return -modifier;    // Input is CCW, reverse it to get CW
                return modifier;                     // No input, output CW
            } else {
                // RIGHT dominant → want CCW output
                if (inputIsCCW) return modifier;     // Input already CCW, follow along
                if (inputIsCW) return -modifier;     // Input is CW, reverse it to get CCW
                return -modifier;                    // No input, output CCW
            }
        }

        // Mode A: always follow input direction at reduced speed
        return modifier;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        float currentSpeed = getSpeed();
        if (Math.abs(currentSpeed - lastInputSpeed) > 0.001f) {
            lastInputSpeed = currentSpeed;
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
        lastInputSpeed = tag.getFloat("LastInputSpeed");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("RedstoneLocked", redstoneLocked);
        tag.putFloat("LastInputSpeed", lastInputSpeed);
    }
}