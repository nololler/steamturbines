package com.xciel.steamturbine.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlock;
import com.xciel.steamturbine.content.pump.SteamPumpBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.shaft.TurbineShaftBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static com.xciel.steamturbine.SteamTurbine.MOD_ID;
import static com.xciel.steamturbine.SteamTurbine.REGISTRATE;

public class STBlocks {

    public static final BlockEntry<SteamBoilerBlock> STEAM_BOILER = REGISTRATE.block("steam_boiler", SteamBoilerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamCompressorBlock> STEAM_COMPRESSOR = REGISTRATE.block("steam_compressor", SteamCompressorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<PressurizedPipeBlock> PRESSURE_PIPE = REGISTRATE.block("pressure_pipe", PressurizedPipeBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamTurbineBlock> STEAM_TURBINE = REGISTRATE.block("steam_turbine", SteamTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .item()
            .build()
            .register();

    public static final BlockEntry<TurbineShaftBlock> TURBINE_SHAFT = REGISTRATE.block("turbine_shaft", TurbineShaftBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(pickaxeOnly())
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(b, () -> 256.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamPumpBlock> STEAM_PUMP = REGISTRATE.block("steam_pump", SteamPumpBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item()
            .build()
            .register();

    public static void register() {}
}
