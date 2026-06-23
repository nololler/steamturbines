package com.xciel.turbines;

import com.xciel.turbines.registrate.STBlockEntityTypes;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlockEntity;
import com.xciel.turbines.content.ejector.SteamEjectorBlockEntity;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlockEntity;
import com.xciel.turbines.content.sjth.SteamJetThrusterBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<com.xciel.turbines.content.boiler.SteamBoilerBlockEntity> STEAM_BOILER = STBlockEntityTypes.STEAM_BOILER;
    public static final BlockEntityEntry<com.xciel.turbines.content.compressor.SteamCompressorBlockEntity> STEAM_COMPRESSOR = STBlockEntityTypes.STEAM_COMPRESSOR;
    public static final BlockEntityEntry<com.xciel.turbines.content.transport.pipe.PressurizedPipeBlockEntity> PRESSURE_PIPE = STBlockEntityTypes.PRESSURE_PIPE;
    public static final BlockEntityEntry<com.xciel.turbines.content.turbine.SteamTurbineBlockEntity> STEAM_TURBINE = STBlockEntityTypes.STEAM_TURBINE;
    public static final BlockEntityEntry<com.xciel.turbines.content.shaft.TurbineShaftBlockEntity> TURBINE_SHAFT = STBlockEntityTypes.TURBINE_SHAFT;
    public static final BlockEntityEntry<com.xciel.turbines.content.pump.SteamPumpBlockEntity> STEAM_PUMP = STBlockEntityTypes.STEAM_PUMP;
    public static final BlockEntityEntry<com.xciel.turbines.content.turbine.LavaDuctTurbineBlockEntity> LAVA_DUCT_TURBINE = STBlockEntityTypes.LAVA_DUCT_TURBINE;
    public static final BlockEntityEntry<com.xciel.turbines.content.shaft.LavaDuctShaftBlockEntity> LAVA_DUCT_SHAFT = STBlockEntityTypes.LAVA_DUCT_SHAFT;
    public static final BlockEntityEntry<DirectionalAnalogGearshiftBlockEntity> DIRECTIONAL_ANALOG_GEARSHIFT = STBlockEntityTypes.DIRECTIONAL_ANALOG_GEARSHIFT;
    public static final BlockEntityEntry<NetworkDiagnoserBlockEntity> NETWORK_DIAGNOSER = STBlockEntityTypes.NETWORK_DIAGNOSER;
    public static final BlockEntityEntry<SteamJetThrusterBlockEntity> STEAM_JET_THRUSTER = STBlockEntityTypes.STEAM_JET_THRUSTER;
    public static final BlockEntityEntry<SteamEjectorBlockEntity> STEAM_EJECTOR = STBlockEntityTypes.STEAM_EJECTOR;

    private AllBlockEntityTypes() {}
}