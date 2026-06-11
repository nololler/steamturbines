package com.xciel.steamturbine.content.dag;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionalAnalogGearshiftBlockEntity extends SplitShaftBlockEntity {

    private static final int RPM_PER_LEVEL = 17;
    private static final int ONE_AT_MAX = 1;
    private static final int MAX_RPM = 256;

    private boolean needsReattach;

    public DirectionalAnalogGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource()) return 0;
        if (face == getSourceFacing()) return 1;

        BlockState state = getBlockState();
        boolean leftPowered = state.getValue(DirectionalAnalogGearshiftBlock.LEFT_POWERED);
        boolean rightPowered = state.getValue(DirectionalAnalogGearshiftBlock.RIGHT_POWERED);

        int diff = (leftPowered ? 15 : 0) - (rightPowered ? 15 : 0);
        if (diff == 0) return 0;

        int rpm = Math.abs(diff) * RPM_PER_LEVEL;
        if (Math.abs(diff) == 15) rpm += ONE_AT_MAX;

        float modifier = rpm / (float) MAX_RPM;
        return diff > 0 ? modifier : -modifier;
    }

    public void neighborChanged() {
        if (level == null || level.isClientSide) return;
        needsReattach = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (needsReattach) {
            needsReattach = false;
            detachKinetics();
            removeSource();
            attachKinetics();
        }
    }
}