package com.xciel.steamturbine.registrate;

import com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity;
import com.xciel.steamturbine.content.shaft.TurbineShaftBlockEntity;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.xciel.steamturbine.SteamTurbine.REGISTRATE;

public class STBlockEntityTypes {

    public static final BlockEntityEntry<SteamBoilerBlockEntity> STEAM_BOILER = REGISTRATE
            .blockEntity("steam_boiler", SteamBoilerBlockEntity::new)
            .validBlocks(STBlocks.STEAM_BOILER)
            .register();

    public static final BlockEntityEntry<SteamCompressorBlockEntity> STEAM_COMPRESSOR = REGISTRATE
            .blockEntity("steam_compressor", SteamCompressorBlockEntity::new)
            .validBlocks(STBlocks.STEAM_COMPRESSOR)
            .register();

    public static final BlockEntityEntry<PressurizedPipeBlockEntity> PRESSURE_PIPE = REGISTRATE
            .blockEntity("pressure_pipe", PressurizedPipeBlockEntity::new)
            .validBlocks(STBlocks.PRESSURE_PIPE)
            .register();

    public static final BlockEntityEntry<SteamTurbineBlockEntity> STEAM_TURBINE = REGISTRATE
            .blockEntity("steam_turbine", SteamTurbineBlockEntity::new)
            .validBlocks(STBlocks.STEAM_TURBINE)
            .register();

    public static final BlockEntityEntry<TurbineShaftBlockEntity> TURBINE_SHAFT = REGISTRATE
            .blockEntity("turbine_shaft", TurbineShaftBlockEntity::new)
            .validBlocks(STBlocks.TURBINE_SHAFT)
            .register();

    public static void register() {}
}