package com.xciel.turbines;

import com.xciel.turbines.registrate.STBlocks;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlock;
import com.xciel.turbines.content.ejector.SteamEjectorBlock;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlock;
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

    private AllBlocks() {}
}