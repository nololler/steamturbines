package com.xciel.turbines.content.pressured_sand;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;

/** Sand with pressure-treated appearance; retains vanilla falling behavior. */
public class PressuredSandBlock extends FallingBlock {

    public static final MapCodec<PressuredSandBlock> CODEC = simpleCodec(PressuredSandBlock::new);

    public PressuredSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
