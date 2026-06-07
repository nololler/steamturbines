package com.xciel.steamturbine;

import com.xciel.steamturbine.registrate.STBlockEntityTypes;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity> STEAM_BOILER = STBlockEntityTypes.STEAM_BOILER;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity> STEAM_COMPRESSOR = STBlockEntityTypes.STEAM_COMPRESSOR;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity> PRESSURE_PIPE = STBlockEntityTypes.PRESSURE_PIPE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity> STEAM_TURBINE = STBlockEntityTypes.STEAM_TURBINE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.shaft.TurbineShaftBlockEntity> TURBINE_SHAFT = STBlockEntityTypes.TURBINE_SHAFT;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.pump.SteamPumpBlockEntity> STEAM_PUMP = STBlockEntityTypes.STEAM_PUMP;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.boilerTurbine.BoilerTurbineBlockEntity> BOILER_TURBINE = STBlockEntityTypes.BOILER_TURBINE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.boilerTurbine.ShaftCasingBlockEntity> BOILER_TURBINE_SHAFT_CASING = STBlockEntityTypes.BOILER_TURBINE_SHAFT_CASING;

    private AllBlockEntityTypes() {}
}