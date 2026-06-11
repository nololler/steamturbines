package com.xciel.steamturbine.content.dag;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionalAnalogGearshiftBlockEntity extends SplitShaftBlockEntity {

    private static final int RPM_PER_LEVEL = 17;
    private static final int ONE_AT_MAX = 1;
    private static final int MAX_RPM = 256;

    private boolean needsReattach;
    private int leftPower;
    private int rightPower;

    public DirectionalAnalogGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("LeftPower", leftPower);
        tag.putInt("RightPower", rightPower);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        leftPower = tag.getInt("LeftPower");
        rightPower = tag.getInt("RightPower");
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource()) return 0;
        if (face == getSourceFacing()) return 1;

        int diff = leftPower - rightPower;
        if (diff == 0) return 0;

        int rpm = Math.abs(diff) * RPM_PER_LEVEL;
        if (Math.abs(diff) == 15) rpm += ONE_AT_MAX;

        float baseModifier = rpm / (float) MAX_RPM;
        return diff > 0 ? baseModifier : -baseModifier;
    }

    public void neighborChanged() {
        if (level == null || level.isClientSide) return;
        refreshPower();
        needsReattach = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        refreshPower();
        if (needsReattach) {
            needsReattach = false;
            detachKinetics();
            removeSource();
            attachKinetics();
        }
    }

    private void refreshPower() {
        BlockState state = getBlockState();
        int left = DirectionalAnalogGearshiftBlock.getSignalForLeft(level, worldPosition);
        int right = DirectionalAnalogGearshiftBlock.getSignalForRight(level, worldPosition);
        leftPower = left;
        rightPower = right;
    }
}