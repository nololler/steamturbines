package com.xciel.turbines;

import com.xciel.turbines.registrate.STBlockEntityTypes;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlockEntity;
import com.xciel.turbines.content.ejector.SteamEjectorBlockEntity;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlockEntity;
import com.xciel.turbines.content.green_nentia_block.GreenNentiaBlockEntity;
import com.xciel.turbines.content.large_turbine.LargeTurbineBlockEntity;
import com.xciel.turbines.content.shaft.HydroTurbineShaftBlockEntity;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineIOBlockEntity;
import com.xciel.turbines.content.open_air_turbine.OpenAirTurbineBlockEntity;
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
    public static final BlockEntityEntry<GreenNentiaBlockEntity> GREEN_NENTIA_BLOCK = STBlockEntityTypes.GREEN_NENTIA_BLOCK;
    public static final BlockEntityEntry<OpenAirTurbineBlockEntity> OPEN_AIR_TURBINE = STBlockEntityTypes.OPEN_AIR_TURBINE;
    public static final BlockEntityEntry<LargeTurbineBlockEntity> LARGE_TURBINE = STBlockEntityTypes.LARGE_TURBINE;
    public static final BlockEntityEntry<HydroTurbineShaftBlockEntity> HYDRO_TURBINE_SHAFT = STBlockEntityTypes.HYDRO_TURBINE_SHAFT;
    public static final BlockEntityEntry<HydroTurbineIOBlockEntity> HYDRO_TURBINE_IO = STBlockEntityTypes.HYDRO_TURBINE_IO;

    private AllBlockEntityTypes() {}
}
