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

    private AllBlocks() {}
}