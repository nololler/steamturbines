package com.xciel.steamturbine;

import com.xciel.steamturbine.registrate.STBlocks;
import com.xciel.steamturbine.content.dag.DirectionalAnalogGearshiftBlock;
import com.xciel.steamturbine.content.nd.NetworkDiagnoserBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;

public class AllBlocks {
    public static final BlockEntry<com.xciel.steamturbine.content.boiler.SteamBoilerBlock> STEAM_BOILER = STBlocks.STEAM_BOILER;
    public static final BlockEntry<com.xciel.steamturbine.content.compressor.SteamCompressorBlock> STEAM_COMPRESSOR = STBlocks.STEAM_COMPRESSOR;
    public static final BlockEntry<com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock> PRESSURE_PIPE = STBlocks.PRESSURE_PIPE;
    public static final BlockEntry<com.xciel.steamturbine.content.turbine.SteamTurbineBlock> STEAM_TURBINE = STBlocks.STEAM_TURBINE;
    public static final BlockEntry<com.xciel.steamturbine.content.shaft.TurbineShaftBlock> TURBINE_SHAFT = STBlocks.TURBINE_SHAFT;
    public static final BlockEntry<com.xciel.steamturbine.content.pump.SteamPumpBlock> STEAM_PUMP = STBlocks.STEAM_PUMP;
    public static final BlockEntry<com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlock> LAVA_DUCT_TURBINE = STBlocks.LAVA_DUCT_TURBINE;
    public static final BlockEntry<com.xciel.steamturbine.content.shaft.LavaDuctShaftBlock> LAVA_DUCT_SHAFT = STBlocks.LAVA_DUCT_SHAFT;
    public static final BlockEntry<DirectionalAnalogGearshiftBlock> DIRECTIONAL_ANALOG_GEARSHIFT = STBlocks.DIRECTIONAL_ANALOG_GEARSHIFT;
    public static final BlockEntry<NetworkDiagnoserBlock> NETWORK_DIAGNOSER = STBlocks.NETWORK_DIAGNOSER;

    private AllBlocks() {}
}