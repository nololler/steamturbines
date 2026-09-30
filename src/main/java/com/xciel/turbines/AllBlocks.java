package com.xciel.turbines;

import com.xciel.turbines.registrate.STBlocks;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlock;
import com.xciel.turbines.content.ejector.SteamEjectorBlock;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlock;
import com.xciel.turbines.content.green_nentia_block.GreenNentiaBlock;
import com.xciel.turbines.content.large_turbine.LargeTurbineBlock;
import com.xciel.turbines.content.open_air_turbine.OpenAirTurbineBlock;
import com.xciel.turbines.content.reinforced_glass.ReinforcedGlassBlock;
import com.xciel.turbines.content.shaft.HydroTurbineShaftBlock;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineIOBlock;
import com.xciel.turbines.content.pressured_sand.PressuredSandBlock;
import com.xciel.turbines.content.sjth.SteamJetThrusterBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;

public class AllBlocks {
    public static final BlockEntry<com.xciel.turbines.content.boiler.SteamBoilerBlock> STEAM_BOILER = STBlocks.STEAM_BOILER;
    public static final BlockEntry<com.xciel.turbines.content.compressor.SteamCompressorBlock> STEAM_COMPRESSOR = STBlocks.STEAM_COMPRESSOR;
    public static final BlockEntry<com.xciel.turbines.content.transport.pipe.PressurizedPipeBlock> PRESSURE_PIPE = STBlocks.PRESSURE_PIPE;
    public static final BlockEntry<com.xciel.turbines.content.turbine.SteamTurbineBlock> STEAM_TURBINE = STBlocks.STEAM_TURBINE;
    public static final BlockEntry<com.xciel.turbines.content.shaft.TurbineShaftBlock> TURBINE_SHAFT = STBlocks.TURBINE_SHAFT;
    public static final BlockEntry<com.xciel.turbines.content.pump.SteamPumpBlock> STEAM_PUMP = STBlocks.STEAM_PUMP;
    public static final BlockEntry<com.xciel.turbines.content.turbine.LavaDuctTurbineBlock> LAVA_DUCT_TURBINE = STBlocks.LAVA_DUCT_TURBINE;
    public static final BlockEntry<com.xciel.turbines.content.shaft.LavaDuctShaftBlock> LAVA_DUCT_SHAFT = STBlocks.LAVA_DUCT_SHAFT;
    public static final BlockEntry<DirectionalAnalogGearshiftBlock> DIRECTIONAL_ANALOG_GEARSHIFT = STBlocks.DIRECTIONAL_ANALOG_GEARSHIFT;
    public static final BlockEntry<NetworkDiagnoserBlock> NETWORK_DIAGNOSER = STBlocks.NETWORK_DIAGNOSER;
    public static final BlockEntry<SteamJetThrusterBlock> STEAM_JET_THRUSTER = STBlocks.STEAM_JET_THRUSTER;
    public static final BlockEntry<SteamEjectorBlock> STEAM_EJECTOR = STBlocks.STEAM_EJECTOR;
    public static final BlockEntry<GreenNentiaBlock> GREEN_NENTIA_BLOCK = STBlocks.GREEN_NENTIA_BLOCK;
    public static final BlockEntry<OpenAirTurbineBlock> OPEN_AIR_TURBINE = STBlocks.OPEN_AIR_TURBINE;
    public static final BlockEntry<LargeTurbineBlock> LARGE_TURBINE = STBlocks.LARGE_TURBINE;
    public static final BlockEntry<ReinforcedGlassBlock> REINFORCED_GLASS = STBlocks.REINFORCED_GLASS;
    public static final BlockEntry<HydroTurbineShaftBlock> HYDRO_TURBINE_SHAFT = STBlocks.HYDRO_TURBINE_SHAFT;
    public static final BlockEntry<HydroTurbineIOBlock> HYDRO_TURBINE_IO = STBlocks.HYDRO_TURBINE_IO;
    public static final BlockEntry<PressuredSandBlock> PRESSURED_SAND = STBlocks.PRESSURED_SAND;

    private AllBlocks() {}
}
