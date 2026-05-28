package com.xciel.steamturbine.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

import static com.xciel.steamturbine.SteamTurbine.rl;

public class STPartialModels {
    public static final PartialModel PIPE_CORE = partial("block/pressured-pipe-core-straight");
    public static final PartialModel PIPE_SPOUT = partial("block/pressured-pipe-spout-connection");

    private static PartialModel partial(String path) {
        return PartialModel.of(rl(path));
    }

    public static void init() {
    }
}