package com.xciel.steamturbine.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

import static com.xciel.steamturbine.SteamTurbine.rl;

public class STPartialModels {
    public static final PartialModel PIPE_CORE = partial("block/pipe_core");
    public static final PartialModel PIPE_SPOUT = partial("block/pipe_spout");
    public static final PartialModel PIPE_HUB = partial("block/pipe_hub");

    private static PartialModel partial(String path) {
        return PartialModel.of(rl(path));
    }

    public static void init() {
    }
}