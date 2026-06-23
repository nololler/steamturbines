package com.xciel.turbines.registrate;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import static com.xciel.turbines.Turbines.MOD_ID;
import static com.xciel.turbines.Turbines.REGISTRATE;
import static com.xciel.turbines.Turbines.rl;

public class STFluids {

    public static final FluidEntry<BaseFlowingFluid.Flowing> PEARLESCENT_DEW =
        REGISTRATE.fluid("pearlescent_dew",
                rl("block/pearlescent_dew_still"),
                rl("block/pearlescent_dew_flow"))
            .properties(b -> b.viscosity(800).density(800).lightLevel(15))
            .fluidProperties(p -> p.levelDecreasePerBlock(2).tickRate(10).slopeFindDistance(4))
            .register();

    public static void register() {}
}
