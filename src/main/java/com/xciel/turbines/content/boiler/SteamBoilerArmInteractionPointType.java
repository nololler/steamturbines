package com.xciel.turbines.content.boiler;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SteamBoilerArmInteractionPointType extends ArmInteractionPointType {

    private static final SteamBoilerArmInteractionPointType INSTANCE = new SteamBoilerArmInteractionPointType();

    private SteamBoilerArmInteractionPointType() {
    }

    public static SteamBoilerArmInteractionPointType getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof SteamBoilerBlock;
    }

    @Override
    public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new Point(this, level, pos, state);
    }

    public static class Point extends ArmInteractionPoint {
        public Point(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            return Vec3.atLowerCornerOf(pos).add(0.5, 1.0, 0.5);
        }
    }
}
