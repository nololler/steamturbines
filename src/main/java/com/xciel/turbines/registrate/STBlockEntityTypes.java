package com.xciel.turbines.registrate;

import com.xciel.turbines.content.boiler.SteamBoilerBlockEntity;
import com.xciel.turbines.content.compressor.SteamCompressorBlockEntity;
import com.xciel.turbines.content.ejector.SteamEjectorBlockEntity;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlockEntity;
import com.xciel.turbines.content.pump.SteamPumpBlockEntity;
import com.xciel.turbines.content.shaft.TurbineShaftBlockEntity;
import com.xciel.turbines.content.shaft.LavaDuctShaftBlockEntity;
import com.xciel.turbines.content.shaft.HydroTurbineShaftBlockEntity;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineIOBlockEntity;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.turbines.content.turbine.SteamTurbineBlockEntity;
import com.xciel.turbines.content.turbine.LavaDuctTurbineBlockEntity;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlockEntity;
import com.xciel.turbines.content.green_nentia_block.GreenNentiaBlockEntity;
import com.xciel.turbines.content.large_turbine.LargeTurbineBlockEntity;
import com.xciel.turbines.content.open_air_turbine.OpenAirTurbineBlockEntity;
import com.xciel.turbines.content.sjth.SteamJetThrusterBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.xciel.turbines.Turbines.REGISTRATE;

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

    public static final BlockEntityEntry<SteamPumpBlockEntity> STEAM_PUMP = REGISTRATE
            .blockEntity("steam_pump", SteamPumpBlockEntity::new)
            .validBlocks(STBlocks.STEAM_PUMP)
            .register();

    public static final BlockEntityEntry<LavaDuctTurbineBlockEntity> LAVA_DUCT_TURBINE = REGISTRATE
            .blockEntity("lava_duct_turbine", LavaDuctTurbineBlockEntity::new)
            .validBlocks(STBlocks.LAVA_DUCT_TURBINE)
            .register();

    public static final BlockEntityEntry<LavaDuctShaftBlockEntity> LAVA_DUCT_SHAFT = REGISTRATE
            .blockEntity("lava_duct_shaft", LavaDuctShaftBlockEntity::new)
            .validBlocks(STBlocks.LAVA_DUCT_SHAFT)
            .register();

    public static final BlockEntityEntry<DirectionalAnalogGearshiftBlockEntity> DIRECTIONAL_ANALOG_GEARSHIFT = REGISTRATE
            .blockEntity("directional_analog_gearshift", DirectionalAnalogGearshiftBlockEntity::new)
            .validBlocks(STBlocks.DIRECTIONAL_ANALOG_GEARSHIFT)
            .register();

    public static final BlockEntityEntry<NetworkDiagnoserBlockEntity> NETWORK_DIAGNOSER = REGISTRATE
            .blockEntity("network_diagnoser", NetworkDiagnoserBlockEntity::new)
            .validBlocks(STBlocks.NETWORK_DIAGNOSER)
            .register();

    public static final BlockEntityEntry<SteamJetThrusterBlockEntity> STEAM_JET_THRUSTER = REGISTRATE
            .blockEntity("steam_jet_thruster", SteamJetThrusterBlockEntity::new)
            .validBlocks(STBlocks.STEAM_JET_THRUSTER)
            .register();

    public static final BlockEntityEntry<SteamEjectorBlockEntity> STEAM_EJECTOR = REGISTRATE
            .blockEntity("steam_ejector", SteamEjectorBlockEntity::new)
            .validBlocks(STBlocks.STEAM_EJECTOR)
            .register();

    public static final BlockEntityEntry<GreenNentiaBlockEntity> GREEN_NENTIA_BLOCK = REGISTRATE
            .blockEntity("green_nentia_block", GreenNentiaBlockEntity::new)
            .validBlocks(STBlocks.GREEN_NENTIA_BLOCK)
            .register();

    public static final BlockEntityEntry<OpenAirTurbineBlockEntity> OPEN_AIR_TURBINE = REGISTRATE
            .blockEntity("open_air_turbine", OpenAirTurbineBlockEntity::new)
            .validBlocks(STBlocks.OPEN_AIR_TURBINE)
            .register();

    public static final BlockEntityEntry<LargeTurbineBlockEntity> LARGE_TURBINE = REGISTRATE
            .blockEntity("large_turbine", LargeTurbineBlockEntity::new)
            .validBlocks(STBlocks.LARGE_TURBINE)
            .register();

    public static final BlockEntityEntry<HydroTurbineShaftBlockEntity> HYDRO_TURBINE_SHAFT = REGISTRATE
            .blockEntity("hydro_shaft", HydroTurbineShaftBlockEntity::new)
            .validBlocks(STBlocks.HYDRO_TURBINE_SHAFT)
            .register();

    public static final BlockEntityEntry<HydroTurbineIOBlockEntity> HYDRO_TURBINE_IO = REGISTRATE
            .blockEntity("hydro_turbine_io", HydroTurbineIOBlockEntity::new)
            .validBlocks(STBlocks.HYDRO_TURBINE_IO)
            .register();

    public static void register() {}
}
