package com.xciel.steamturbine.registrate;

import com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity;
import com.xciel.steamturbine.content.nd.NetworkDiagnoserBlockEntity;
import com.xciel.steamturbine.content.pump.SteamPumpBlockEntity;
import com.xciel.steamturbine.content.shaft.TurbineShaftBlockEntity;
import com.xciel.steamturbine.content.shaft.LavaDuctShaftBlockEntity;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlockEntity;
import com.xciel.steamturbine.content.dag.DirectionalAnalogGearshiftBlockEntity;
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

    public static void register() {}
}