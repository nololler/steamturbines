package com.xciel.steamturbine;

import com.xciel.steamturbine.registrate.STBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;

public class AllBlocks {
    public static final BlockEntry<com.xciel.steamturbine.content.boiler.SteamBoilerBlock> STEAM_BOILER = STBlocks.STEAM_BOILER;
    public static final BlockEntry<com.xciel.steamturbine.content.compressor.SteamCompressorBlock> STEAM_COMPRESSOR = STBlocks.STEAM_COMPRESSOR;
    public static final BlockEntry<com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock> PRESSURE_PIPE = STBlocks.PRESSURE_PIPE;
    public static final BlockEntry<com.xciel.steamturbine.content.turbine.SteamTurbineBlock> STEAM_TURBINE = STBlocks.STEAM_TURBINE;
    public static final BlockEntry<com.xciel.steamturbine.content.shaft.TurbineShaftBlock> TURBINE_SHAFT = STBlocks.TURBINE_SHAFT;
    public static final BlockEntry<com.xciel.steamturbine.content.pump.SteamPumpBlock> STEAM_PUMP = STBlocks.STEAM_PUMP;
    public static final BlockEntry<com.xciel.steamturbine.content.boilerTurbine.BoilerTurbineBlock> BOILER_TURBINE = STBlocks.BOILER_TURBINE;
    public static final BlockEntry<com.xciel.steamturbine.content.boilerTurbine.ShaftCasingBlock> BOILER_TURBINE_SHAFT_CASING = STBlocks.BOILER_TURBINE_SHAFT_CASING;

    private AllBlocks() {}
}