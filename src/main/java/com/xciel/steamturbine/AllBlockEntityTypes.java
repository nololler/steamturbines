package com.xciel.steamturbine;

import com.xciel.steamturbine.registrate.STBlockEntityTypes;
import com.xciel.steamturbine.content.dag.DirectionalAnalogGearshiftBlockEntity;
import com.xciel.steamturbine.content.nd.NetworkDiagnoserBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity> STEAM_BOILER = STBlockEntityTypes.STEAM_BOILER;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity> STEAM_COMPRESSOR = STBlockEntityTypes.STEAM_COMPRESSOR;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity> PRESSURE_PIPE = STBlockEntityTypes.PRESSURE_PIPE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity> STEAM_TURBINE = STBlockEntityTypes.STEAM_TURBINE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.shaft.TurbineShaftBlockEntity> TURBINE_SHAFT = STBlockEntityTypes.TURBINE_SHAFT;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.pump.SteamPumpBlockEntity> STEAM_PUMP = STBlockEntityTypes.STEAM_PUMP;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlockEntity> LAVA_DUCT_TURBINE = STBlockEntityTypes.LAVA_DUCT_TURBINE;
    public static final BlockEntityEntry<com.xciel.steamturbine.content.shaft.LavaDuctShaftBlockEntity> LAVA_DUCT_SHAFT = STBlockEntityTypes.LAVA_DUCT_SHAFT;
    public static final BlockEntityEntry<DirectionalAnalogGearshiftBlockEntity> DIRECTIONAL_ANALOG_GEARSHIFT = STBlockEntityTypes.DIRECTIONAL_ANALOG_GEARSHIFT;
    public static final BlockEntityEntry<NetworkDiagnoserBlockEntity> NETWORK_DIAGNOSER = STBlockEntityTypes.NETWORK_DIAGNOSER;

    private AllBlockEntityTypes() {}
}