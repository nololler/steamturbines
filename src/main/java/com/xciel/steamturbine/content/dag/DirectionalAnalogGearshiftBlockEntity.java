package com.xciel.steamturbine.content.dag;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DirectionalAnalogGearshiftBlockEntity extends SplitShaftBlockEntity {

    private static final float RPM_PER_LEVEL = 17f;
    private static final float ONE_AT_MAX = 1f;
    private static final float MAX_RPM = 256f;

    private static final boolean DIR_NORTH_LEFT  = true;
    private static final boolean DIR_NORTH_RIGHT = false;
    private static final boolean DIR_SOUTH_LEFT  = false;
    private static final boolean DIR_SOUTH_RIGHT = true;
    private static final boolean DIR_EAST_LEFT   = false;
    private static final boolean DIR_EAST_RIGHT  = true;
    private static final boolean DIR_WEST_LEFT   = true;
    private static final boolean DIR_WEST_RIGHT  = false;
    private static final boolean DIR_UP_LEFT     = true;
    private static final boolean DIR_UP_RIGHT    = false;
    private static final boolean DIR_DOWN_LEFT   = true;
    private static final boolean DIR_DOWN_RIGHT  = false;

    private boolean redstoneLocked;
    private ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    private BlockPos lastSourcePos;

    public DirectionalAnalogGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private float computeModifier() {
        BlockState state = getBlockState();
        int leftPower = state.getValue(DirectionalAnalogGearshiftBlock.LEFT_POWER);
        int rightPower = state.getValue(DirectionalAnalogGearshiftBlock.RIGHT_POWER);
        int diff = leftPower - rightPower;
        if (diff == 0) return 0;
        float modifier = Math.abs(diff) * RPM_PER_LEVEL / MAX_RPM;
        if (Math.abs(diff) == 15) modifier += ONE_AT_MAX / MAX_RPM;
        return Math.min(modifier, 1f);
    }

    private float computeModeBSpeed() {
        BlockState state = getBlockState();
        int leftPower = state.getValue(DirectionalAnalogGearshiftBlock.LEFT_POWER);
        int rightPower = state.getValue(DirectionalAnalogGearshiftBlock.RIGHT_POWER);
        int diff = leftPower - rightPower;
        if (diff == 0) return 0;

        float modifier = Math.abs(diff) * RPM_PER_LEVEL / MAX_RPM;
        if (Math.abs(diff) == 15) modifier += ONE_AT_MAX / MAX_RPM;
        modifier = Math.min(modifier, 1f);

        float inputSpeed = getInputSpeed();
        float magnitude = Math.abs(inputSpeed) * modifier;

        Direction facing = state.getValue(DirectionalAnalogGearshiftBlock.FACING);
        return outputCW(facing, diff > 0) ? magnitude : -magnitude;
    }

    private float getInputSpeed() {
        if (lastSourcePos == null) return 0;
        BlockEntity be = level.getBlockEntity(lastSourcePos);
        if (be instanceof KineticBlockEntity kbe) return kbe.getSpeed();
        return 0;
    }

    private static boolean outputCW(Direction facing, boolean leftDominant) {
        return switch (facing) {
            case NORTH -> leftDominant ? DIR_NORTH_LEFT : DIR_NORTH_RIGHT;
            case SOUTH -> leftDominant ? DIR_SOUTH_LEFT : DIR_SOUTH_RIGHT;
            case EAST -> leftDominant ? DIR_EAST_LEFT : DIR_EAST_RIGHT;
            case WEST -> leftDominant ? DIR_WEST_LEFT : DIR_WEST_RIGHT;
            case UP -> leftDominant ? DIR_UP_LEFT : DIR_UP_RIGHT;
            case DOWN -> leftDominant ? DIR_DOWN_LEFT : DIR_DOWN_RIGHT;
        };
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
            if (hasLevel() && !level.isClientSide) {
                detachKinetics();
                removeSource();
                lastSourcePos = null;
                speed = 0;
                setNetwork(null);
                setChanged();
                sendData();
            }
        });
        behaviours.add(movementDirection);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (redstoneLocked) {
            Direction outputFace = getBlockState().getValue(DirectionalAnalogGearshiftBlock.FACING);
            if (face == outputFace) return 1;
            return 0;
        }
        if (!hasSource()) return 0;
        if (face == getSourceFacing()) return 1;

        float modifier = computeModifier();
        if (modifier == 0) return 0;

        return modifier;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) return;

        if (redstoneLocked) {
            if (lastSourcePos == null) {
                lastSourcePos = source;
            }

            float target = computeModeBSpeed();

            if (speed != target) {
                detachKinetics();
                removeSource();
                speed = target;
                if (target != 0) {
                    setNetwork(worldPosition.asLong());
                } else {
                    setNetwork(null);
                }
                attachKinetics();
                sendData();
            }
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (redstoneLocked) return speed;
        return 0;
    }

    @Override
    public Direction getSourceFacing() {
        if (redstoneLocked) {
            BlockState state = getBlockState();
            return state.getValue(DirectionalAnalogGearshiftBlock.FACING).getOpposite();
        }
        return super.getSourceFacing();
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedByAxis, boolean connectedByCogs) {
        if (redstoneLocked && connectedByAxis) {
            Direction face = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
            Direction outputFace = getBlockState().getValue(DirectionalAnalogGearshiftBlock.FACING);
            return face == outputFace ? 1f : 0f;
        }
        return 0;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        redstoneLocked = tag.getBoolean("RedstoneLocked");
        if (tag.contains("LastSourcePos")) {
            lastSourcePos = BlockPos.of(tag.getLong("LastSourcePos"));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("RedstoneLocked", redstoneLocked);
        if (lastSourcePos != null) {
            tag.putLong("LastSourcePos", lastSourcePos.asLong());
        }
    }
}